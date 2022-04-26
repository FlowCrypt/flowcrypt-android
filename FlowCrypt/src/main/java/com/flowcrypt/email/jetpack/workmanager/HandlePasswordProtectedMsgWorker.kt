/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.api.retrofit.request.model.MessageUploadRequest
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.javax.mail.internet.getFromAddress
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.jetpack.workmanager.base.BaseMsgWorker
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.security.pgp.PgpEncryptAndOrSign
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.gson.GsonBuilder
import com.sun.mail.util.MailConnectException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.pgpainless.util.Passphrase
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.SocketException
import java.util.Base64
import java.util.Properties
import javax.mail.Address
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.net.ssl.SSLException
import kotlin.random.Random

/**
 * @author Denis Bondarenko
 *         Date: 12/29/21
 *         Time: 9:30 AM
 *         E-mail: DenBond7@gmail.com
 */
class HandlePasswordProtectedMsgWorker(context: Context, params: WorkerParameters) :
  BaseMsgWorker(context, params) {
  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    LogsUtil.d(TAG, "doWork")

    if (isStopped) {
      return@withContext Result.success()
    }

    try {
      val account = AccountViewModel.getAccountEntityWithDecryptedInfoSuspend(
        roomDatabase.accountDao().getActiveAccountSuspend()
      ) ?: return@withContext Result.success()

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
      val apiRepository = FlowcryptApiRepository()
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
            //get msg attachments(including forwarded that can be encrypted)
            val attachments = getAttachments(msgEntity)

            //create MimeMessage from content + attachments
            val plainMimeMsgWithAttachments = getMimeMessage(account, msgEntity, attachments)

            //get recipients that will be used to create password-encrypted msg
            val toCandidates =
              plainMimeMsgWithAttachments.getRecipients(Message.RecipientType.TO) ?: emptyArray()
            val ccCandidates =
              plainMimeMsgWithAttachments.getRecipients(Message.RecipientType.CC) ?: emptyArray()
            val bccCandidates =
              plainMimeMsgWithAttachments.getRecipients(Message.RecipientType.BCC) ?: emptyArray()

            if (toCandidates.isEmpty() && ccCandidates.isEmpty() && bccCandidates.isEmpty()) {
              throw IllegalStateException("Wrong password-protected implementation")
            }

            //start of creating and uploading a password-protected msg to FES
            val fromAddress = plainMimeMsgWithAttachments.getFromAddress()
            val domain = EmailUtil.getDomain(fromAddress.address)
            val idToken = GeneralUtil.getGoogleIdToken(
              applicationContext, RETRY_ATTEMPTS_COUNT_FOR_GETTING_ID_TOKEN
            )
            val replyToken = fetchReplyToken(apiRepository, domain, idToken)
            val replyInfoData = ReplyInfoData(
              sender = fromAddress.address.lowercase(),
              recipient = (toCandidates + ccCandidates + bccCandidates)
                .mapNotNull { (it as? InternetAddress)?.address?.lowercase() }
                .filterNot {
                  it.equals(fromAddress.address, true)
                }
                .toHashSet()
                .toList(),
              subject = plainMimeMsgWithAttachments.subject,
              token = replyToken
            )

            //prepare bodyWithReplyToken
            val replyInfo = Base64.getEncoder().encodeToString(
              GsonBuilder().create().toJson(replyInfoData).toByteArray()
            )

            val infoDiv = genInfoDiv(replyInfo)
            val originalText = getDecryptedContentFromMessage(
              mimeMsgWithAttachments = plainMimeMsgWithAttachments,
              accountSecretKeys = accountSecretKeys,
              keysStorage = keysStorage
            )
            val bodyWithReplyToken = originalText + "\n\n" + infoDiv

            val rawMimeMsg = createRawPlainMimeMsgWithAttachments(
              plainMimeMsgWithAttachments = plainMimeMsgWithAttachments,
              bodyWithReplyToken = bodyWithReplyToken
            )

            //encrypt the raw MIME message ONLY FOR THE MESSAGE PASSWORD
            val pwdEncryptedWithAttachments = PgpEncryptAndOrSign.encryptAndOrSignMsg(
              msg = rawMimeMsg,
              pubKeys = emptyList(),
              prvKeys = emptyList(),
              passphrase = Passphrase.fromPassword(
                KeyStoreCryptoManager.decryptSuspend(String(requireNotNull(msgEntity.password)))
              )
            )

            //upload resulting data to FES
            val fesUrl = uploadMsgToFESAndReturnUrl(
              apiRepository = apiRepository,
              domain = domain,
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

  private fun createRawPlainMimeMsgWithAttachments(
    plainMimeMsgWithAttachments: MimeMessage,
    bodyWithReplyToken: String
  ): String {
    // construct a regular plain mime message using bodyWithReplyToken + attachments
    val multipart = plainMimeMsgWithAttachments.content as Multipart
    //need to remove 'encrypted.asc' from the existing MIME
    multipart.removeBodyPart(0)
    multipart.addBodyPart(MimeBodyPart().apply { setText(bodyWithReplyToken) }, 0)
    plainMimeMsgWithAttachments.saveChanges()

    //prepare raw MIME
    val rawMimeMsg = String(ByteArrayOutputStream().apply {
      plainMimeMsgWithAttachments.writeTo(this)
    }.toByteArray())
    return rawMimeMsg
  }

  private suspend fun getMimeMessage(
    account: AccountEntity,
    msgEntity: MessageEntity,
    attachments: List<AttachmentEntity>
  ) = EmailUtil.createMimeMsg(
    context = applicationContext,
    sess = Session.getDefaultInstance(Properties()),
    account = account,
    msgEntity = msgEntity.copy(isEncrypted = false),
    atts = attachments
  )

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
      msgEntity.rawMessageWithoutAttachments?.toInputStream()
    )
    val fromAddress = (mimeMsgWithoutAttachments.from.first() as InternetAddress).address
    val multipart = mimeMsgWithoutAttachments.content as Multipart
    multipart.addBodyPart(MimeBodyPart().apply {
      setText(applicationContext.getString(R.string.password_protected_msg_promo, fromAddress, url))
    }, 0)
    //todo-denbond7 need to add HTML version
    mimeMsgWithoutAttachments.saveChanges()

    val out = ByteArrayOutputStream()
    mimeMsgWithoutAttachments.writeTo(out)

    roomDatabase.msgDao().updateSuspend(
      msgEntity.copy(
        rawMessageWithoutAttachments = String(out.toByteArray()),
        state = MessageState.QUEUED.value
      )
    )
  }

  private suspend fun uploadMsgToFESAndReturnUrl(
    apiRepository: FlowcryptApiRepository,
    domain: String,
    idToken: String,
    messageUploadRequest: MessageUploadRequest,
    pwdEncryptedWithAttachments: String
  ): String = withContext(Dispatchers.IO) {
    val messageUploadResponseResult = apiRepository.uploadPasswordProtectedMsgToWebPortal(
      context = applicationContext,
      domain = domain,
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
    mimeMsgWithAttachments: MimeMessage,
    accountSecretKeys: PGPSecretKeyRingCollection,
    keysStorage: KeysStorageImpl
  ): String = withContext(Dispatchers.IO) {
    val decryptionResult = PgpDecryptAndOrVerify.decryptAndOrVerifyWithResult(
      srcInputStream = (mimeMsgWithAttachments.content as Multipart).getBodyPart(0).content as InputStream,
      publicKeys = PGPPublicKeyRingCollection(emptyList()),
      secretKeys = accountSecretKeys,
      protector = keysStorage.getSecretKeyRingProtector()
    )

    return@withContext String(decryptionResult.content?.toByteArray() ?: byteArrayOf())
  }

  private suspend fun fetchReplyToken(
    apiRepository: FlowcryptApiRepository,
    domain: String,
    idToken: String
  ): String = withContext(Dispatchers.IO) {
    val messageReplyTokenResponseResult =
      apiRepository.getReplyTokenForPasswordProtectedMsg(
        context = applicationContext,
        domain = domain,
        idToken = idToken
      )

    com.flowcrypt.email.api.retrofit.response.base.Result.throwExceptionIfNotSuccess(
      messageReplyTokenResponseResult
    )

    return@withContext requireNotNull(messageReplyTokenResponseResult.data?.replyToken)
  }

  private data class ReplyInfoData(
    val sender: String,
    val recipient: List<String>,
    val subject: String,
    val token: String,
  )

  companion object {
    private val TAG = HandlePasswordProtectedMsgWorker::class.java.simpleName
    private const val RETRY_ATTEMPTS_COUNT_FOR_GETTING_ID_TOKEN = 6
    val NAME = HandlePasswordProtectedMsgWorker::class.java.simpleName

    private fun genInfoDiv(replyInfo: String?) =
      "<div style=\"display: none\" class=\"cryptup_reply\" cryptup-data=\"$replyInfo\"></div>"

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          NAME,
          ExistingWorkPolicy.REPLACE,
          OneTimeWorkRequestBuilder<HandlePasswordProtectedMsgWorker>()
            .setConstraints(constraints)
            .build()
        )
    }
  }
}
