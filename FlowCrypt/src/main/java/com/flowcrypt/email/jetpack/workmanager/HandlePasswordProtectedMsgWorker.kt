/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.javamail.AttachmentInfoDataSource
import com.flowcrypt.email.api.email.javamail.PasswordProtectedAttachmentInfoDataSource
import com.flowcrypt.email.api.retrofit.ApiClientRepository
import com.flowcrypt.email.api.retrofit.request.model.MessageUploadRequest
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.jakarta.mail.internet.getFromAddress
import com.flowcrypt.email.jetpack.workmanager.base.BaseMsgWorker
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.security.pgp.PgpEncryptAndOrSign
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.OutgoingMessagesManager
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.gson.GsonBuilder
import com.sun.mail.util.MailConnectException
import jakarta.activation.DataHandler
import jakarta.mail.Address
import jakarta.mail.Message
import jakarta.mail.MessagingException
import jakarta.mail.Multipart
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.FilenameUtils
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.pgpainless.util.Passphrase
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.SocketException
import java.util.Base64
import java.util.Properties
import javax.net.ssl.SSLException
import kotlin.random.Random

/**
 * @author Denys Bondarenko
 */
class HandlePasswordProtectedMsgWorker(context: Context, params: WorkerParameters) :
  BaseMsgWorker(context, params) {
  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    LogsUtil.d(TAG, "doWork")

    if (isStopped) {
      return@withContext Result.success()
    }

    try {
      val account = roomDatabase.accountDao().getActiveAccountSuspend()?.withDecryptedInfo()
        ?: return@withContext Result.success()

      val passwordProtectedCandidates = roomDatabase.msgDao().getOutboxMsgsByStatesSuspend(
        account = account.email,
        msgStates = listOf(MessageState.NEW_PASSWORD_PROTECTED.value)
      )

      if (passwordProtectedCandidates.isNotEmpty()) {
        prepareAndUploadPasswordProtectedMsgsToFES(account)
      }

      return@withContext rescheduleIfActiveAccountWasChanged(account)
    } catch (e: Exception) {
      e.printStackTrace()
      return@withContext Result.failure()
    } finally {
      LogsUtil.d(TAG, "work was finished")
    }
  }

  private suspend fun prepareAndUploadPasswordProtectedMsgsToFES(account: AccountEntity) =
    withContext(Dispatchers.IO) {
      val keysStorage = KeysStorageImpl.getInstance(applicationContext)
      val accountSecretKeys = PGPSecretKeyRingCollection(keysStorage.getPGPSecretKeyRings())

      var list: List<MessageEntity>
      var lastMsgUID = 0L
      while (true) {
        list = roomDatabase.msgDao().getOutboxMsgsByStatesSuspend(
          account = account.email,
          msgStates = listOf(MessageState.NEW_PASSWORD_PROTECTED.value)
        )

        if (list.isEmpty()) break
        val msgEntity = list.firstOrNull { it.uid > lastMsgUID } ?: list[0]

        lastMsgUID = msgEntity.uid

        try {
          if (msgEntity.isEncrypted == true && msgEntity.isPasswordProtected) {
            //get msg attachments. Please pay attention that all of these attachments are encrypted.
            val attachments = getAttachments(msgEntity)

            //create MimeMessage from encrypted content
            val mimeMsgWithEncryptedContent = EmailUtil.createMimeMsg(
              context = applicationContext,
              sess = Session.getDefaultInstance(Properties()),
              msgEntity = msgEntity,
              atts = emptyList()
            )

            //get recipients that will be used to create password-encrypted msg
            val toCandidates =
              mimeMsgWithEncryptedContent.getRecipients(Message.RecipientType.TO) ?: emptyArray()
            val ccCandidates =
              mimeMsgWithEncryptedContent.getRecipients(Message.RecipientType.CC) ?: emptyArray()
            val bccCandidates =
              mimeMsgWithEncryptedContent.getRecipients(Message.RecipientType.BCC) ?: emptyArray()

            if (toCandidates.isEmpty() && ccCandidates.isEmpty() && bccCandidates.isEmpty()) {
              throw IllegalStateException("Wrong password-protected implementation")
            }

            //start of creating and uploading a password-protected msg to FES
            val fromAddress = mimeMsgWithEncryptedContent.getFromAddress()
            val baseFesUrlPath = GeneralUtil.genBaseFesUrlPath(
              useCustomerFesUrl = account.useCustomerFesUrl,
              domain = EmailUtil.getDomain(fromAddress.address)
            )

            val idToken = GeneralUtil.getGoogleIdTokenSilently(
              context = applicationContext,
              maxRetryAttemptCount = RETRY_ATTEMPTS_COUNT_FOR_GETTING_ID_TOKEN,
              accountEntity = account
            )
            val replyToken = fetchReplyToken(baseFesUrlPath, idToken)
            val replyInfoData = ReplyInfoData(
              sender = fromAddress.address.lowercase(),
              recipient = (toCandidates + ccCandidates + bccCandidates)
                .mapNotNull { (it as? InternetAddress)?.address?.lowercase() }
                .filterNot {
                  it.equals(fromAddress.address, true)
                }
                .toHashSet()
                .sorted(),
              subject = mimeMsgWithEncryptedContent.subject,
              token = replyToken
            )

            //prepare bodyWithReplyToken
            val replyInfo = Base64.getEncoder().encodeToString(
              GsonBuilder().create().toJson(replyInfoData).toByteArray()
            )

            val infoDiv = genInfoDiv(replyInfo)
            val originalText = getDecryptedContentFromMessage(
              mimeMsgWithEncryptedContent = mimeMsgWithEncryptedContent,
              accountSecretKeys = accountSecretKeys,
              keysStorage = keysStorage
            )

            val bodyWithReplyToken = originalText + "\n\n" + infoDiv

            val rawMimeMsg = createRawMimeMsgWithAttachments(
              sourceMimeMessage = mimeMsgWithEncryptedContent.apply {
                //ref https://github.com/FlowCrypt/flowcrypt-android/issues/2279
                setRecipients(Message.RecipientType.BCC, emptyArray())
              },
              bodyWithReplyToken = bodyWithReplyToken,
              attachments = attachments,
              accountSecretKeys = accountSecretKeys,
              keysStorage = keysStorage
            )

            //encrypt the raw MIME message ONLY FOR THE MESSAGE PASSWORD
            val pwdEncryptedWithAttachments = PgpEncryptAndOrSign.encryptAndOrSignMsg(
              msg = rawMimeMsg,
              pubKeys = emptyList(),
              prvKeys = emptyList(),
              passphrase = Passphrase.fromPassword(
                KeyStoreCryptoManager.decryptSuspend(String(requireNotNull(msgEntity.password)))
              ),
              hideArmorMeta = account.clientConfiguration?.shouldHideArmorMeta() ?: false
            )

            //upload resulting data to FES
            val fesUrl = uploadMsgToFESAndReturnUrl(
              baseFesUrlPath = baseFesUrlPath,
              idToken = idToken,
              messageUploadRequest = genMessageUploadRequest(
                replyToken,
                fromAddress,
                toCandidates,
                ccCandidates,
                bccCandidates
              ),
              pwdEncryptedWithAttachments = pwdEncryptedWithAttachments
            )

            updateExistingMimeMsgWithUrl(msgEntity, fesUrl)
          } else {
            roomDatabase.msgDao().updateSuspend(msgEntity.copy(state = MessageState.QUEUED.value))
          }
          MessagesSenderWorker.enqueue(applicationContext)
        } catch (e: Exception) {
          e.printStackTrace()
          handleExceptionsForMessage(e, msgEntity, account)
        }
      }
    }

  private fun genMessageUploadRequest(
    replyToken: String,
    fromAddress: Address,
    toCandidates: Array<Address>,
    ccCandidates: Array<Address>,
    bccCandidates: Array<Address>
  ) = MessageUploadRequest(
    associateReplyToken = replyToken,
    from = (fromAddress as InternetAddress).toString(),
    to = toCandidates.map { (it as InternetAddress).toString() },
    cc = ccCandidates.map { (it as InternetAddress).toString() },
    bcc = bccCandidates.map { (it as InternetAddress).toString() }
  )

  private suspend fun handleExceptionsForMessage(
    e: Exception,
    msgEntity: MessageEntity,
    account: AccountEntity
  ) = withContext(Dispatchers.IO) {
    ExceptionUtil.handleError(e)

    if (!GeneralUtil.isConnected(applicationContext)) {
      if (msgEntity.msgState !== MessageState.SENT) {
        roomDatabase.msgDao().updateSuspend(
          msgEntity.copy(state = MessageState.NEW_PASSWORD_PROTECTED.value)
        )
      }

      throw e
    } else {
      val newMsgState = when (e) {
        is MailConnectException -> {
          MessageState.NEW_PASSWORD_PROTECTED
        }

        is MessagingException -> {
          if (e.cause is SSLException || e.cause is SocketException) {
            MessageState.NEW_PASSWORD_PROTECTED
          } else {
            MessageState.ERROR_PASSWORD_PROTECTED
          }
        }

        else -> {
          when (e.cause) {
            is FileNotFoundException -> MessageState.ERROR_CACHE_PROBLEM

            else -> MessageState.ERROR_PASSWORD_PROTECTED
          }
        }
      }

      roomDatabase.msgDao()
        .updateSuspend(msgEntity.copy(state = newMsgState.value, errorMsg = e.message))

      if (newMsgState == MessageState.ERROR_PASSWORD_PROTECTED) {
        GeneralUtil.notifyUserAboutProblemWithOutgoingMsgs(applicationContext, account)
      }
    }
  }

  private fun createRawMimeMsgWithAttachments(
    sourceMimeMessage: MimeMessage,
    bodyWithReplyToken: String,
    attachments: List<AttachmentEntity>,
    accountSecretKeys: PGPSecretKeyRingCollection,
    keysStorage: KeysStorageImpl
  ): String {
    val resultMimeMessage = MimeMessage(sourceMimeMessage)
    // construct a regular mime message using bodyWithReplyToken + attachments
    val mimeMultipart = MimeMultipart()
    mimeMultipart.addBodyPart(MimeBodyPart().apply { setText(bodyWithReplyToken) }, 0)

    for (attachment in attachments) {
      val attBodyPart = MimeBodyPart()
      val attInfo = attachment.toAttInfo()
      /*At this stage we should have encrypted files.
      If the original file doesn't have 'pgp' extension it seems
      it was not encrypted and should be sent as is.
      For example, public keys that were sent via 'Include Public Key' action.*/
      val isAttachmentEncrypted =
        FilenameUtils.getExtension(attInfo.name).equals(Constants.PGP_FILE_EXT, true)

      attBodyPart.dataHandler = if (isAttachmentEncrypted) {
        DataHandler(
          PasswordProtectedAttachmentInfoDataSource(
            context = applicationContext,
            att = attInfo,
            secretKeys = accountSecretKeys,
            protector = keysStorage.getSecretKeyRingProtector()
          )
        )
      } else {
        DataHandler(AttachmentInfoDataSource(context = applicationContext, att = attInfo))
      }
      attBodyPart.fileName =
        if (isAttachmentEncrypted) FilenameUtils.removeExtension(attInfo.getSafeName()) else attInfo.getSafeName()
      attBodyPart.contentID = attInfo.id
      mimeMultipart.addBodyPart(attBodyPart)
    }

    resultMimeMessage.setContent(mimeMultipart)
    resultMimeMessage.saveChanges()

    //prepare raw MIME
    val rawMimeMsg = String(ByteArrayOutputStream().apply {
      resultMimeMessage.writeTo(this)
    }.toByteArray())
    return rawMimeMsg
  }

  private suspend fun getAttachments(msgEntity: MessageEntity) =
    roomDatabase.attachmentDao().getAttachmentsSuspend(
      account = msgEntity.email,
      label = JavaEmailConstants.FOLDER_OUTBOX,
      uid = msgEntity.uid
    ).map {
      it.copy(
        forwardedFolder = "Outbox",
        forwardedUid = Random.nextLong(),
        decryptWhenForward = true
      )
    }

  private suspend fun updateExistingMimeMsgWithUrl(
    msgEntity: MessageEntity,
    url: String
  ) = withContext(Dispatchers.IO) {
    val mimeMsgWithoutAttachments = MimeMessage(
      Session.getDefaultInstance(Properties()),
      OutgoingMessagesManager.getOutgoingMessageFromFile(
        applicationContext, requireNotNull(msgEntity.id)
      )?.inputStream()
    )

    val fromAddress = (mimeMsgWithoutAttachments.from.first() as InternetAddress).address
    val multipart = mimeMsgWithoutAttachments.content as Multipart
    multipart.addBodyPart(MimeBodyPart().apply {
      setText(applicationContext.getString(R.string.password_protected_msg_promo, fromAddress, url))
    }, 0)
    //todo-denbond7 need to add HTML version
    mimeMsgWithoutAttachments.saveChanges()

    OutgoingMessagesManager.updatedOutgoingMessage(
      applicationContext, requireNotNull(msgEntity.id),
      mimeMsgWithoutAttachments
    )

    roomDatabase.msgDao().updateSuspend(msgEntity.copy(state = MessageState.QUEUED.value))
  }

  private suspend fun uploadMsgToFESAndReturnUrl(
    baseFesUrlPath: String,
    idToken: String,
    messageUploadRequest: MessageUploadRequest,
    pwdEncryptedWithAttachments: String
  ): String = withContext(Dispatchers.IO) {
    val messageUploadResponseResult = ApiClientRepository.FES.uploadPasswordProtectedMsgToWebPortal(
      context = applicationContext,
      baseFesUrlPath = baseFesUrlPath,
      idToken = idToken,
      messageUploadRequest = messageUploadRequest,
      msg = pwdEncryptedWithAttachments
    )

    com.flowcrypt.email.api.retrofit.response.base.Result.throwExceptionIfNotSuccess(
      messageUploadResponseResult
    )
    return@withContext requireNotNull(messageUploadResponseResult.data?.url)
  }

  private suspend fun getDecryptedContentFromMessage(
    mimeMsgWithEncryptedContent: MimeMessage,
    accountSecretKeys: PGPSecretKeyRingCollection,
    keysStorage: KeysStorageImpl
  ): String = withContext(Dispatchers.IO) {
    val decryptionResult = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      srcInputStream = (mimeMsgWithEncryptedContent.content as Multipart).getBodyPart(0).content as InputStream,
      publicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = accountSecretKeys,
      protector = keysStorage.getSecretKeyRingProtector()
    )

    decryptionResult.exception?.printStackTrace()

    return@withContext String(decryptionResult.content?.toByteArray() ?: byteArrayOf())
  }

  private suspend fun fetchReplyToken(
    baseFesUrlPath: String,
    idToken: String
  ): String = withContext(Dispatchers.IO) {
    val messageReplyTokenResponseResult =
      ApiClientRepository.FES.getReplyTokenForPasswordProtectedMsg(
        context = applicationContext,
        idToken = idToken,
        baseFesUrlPath = baseFesUrlPath,
      )

    com.flowcrypt.email.api.retrofit.response.base.Result.throwExceptionIfNotSuccess(
      messageReplyTokenResponseResult
    )

    return@withContext requireNotNull(messageReplyTokenResponseResult.data?.replyToken)
  }

  data class ReplyInfoData(
    val sender: String,
    val recipient: List<String>,
    val subject: String,
    val token: String,
  )

  companion object {
    private val TAG = HandlePasswordProtectedMsgWorker::class.java.simpleName
    private const val RETRY_ATTEMPTS_COUNT_FOR_GETTING_ID_TOKEN = 6
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".HANDLE_PASSWORD_PROTECTED_MESSAGES"

    fun genInfoDiv(replyInfo: String?) =
      "<div style=\"display: none\" class=\"cryptup_reply\" cryptup-data=\"$replyInfo\"></div>"

    fun enqueue(context: Context) {
      enqueueWithDefaultParameters<HandlePasswordProtectedMsgWorker>(
        context = context,
        uniqueWorkName = GROUP_UNIQUE_TAG,
        existingWorkPolicy = ExistingWorkPolicy.REPLACE
      )
    }
  }
}
