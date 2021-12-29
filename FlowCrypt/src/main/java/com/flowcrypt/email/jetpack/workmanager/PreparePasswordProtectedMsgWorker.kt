/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.accounts.AuthenticatorException
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
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.kotlin.toInputStream
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.jetpack.workmanager.base.BasePrepareMsgWorker
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.security.pgp.PgpEncryptAndOrSign
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.CopyNotSavedInSentFolderException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.google.GoogleApiClientHelper
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.gson.GsonBuilder
import com.sun.mail.util.MailConnectException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
import javax.mail.AuthenticationFailedException
import javax.mail.Message
import javax.mail.MessagingException
import javax.mail.Multipart
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.net.ssl.SSLException

/**
 * @author Denis Bondarenko
 *         Date: 12/29/21
 *         Time: 9:30 AM
 *         E-mail: DenBond7@gmail.com
 */
class PreparePasswordProtectedMsgWorker(context: Context, params: WorkerParameters) :
  BasePrepareMsgWorker(context, params) {
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
        prepareAndUploadMsgsToFES(account)
      }

      return@withContext rescheduleIfActiveAccountWasChanged(account)
    } catch (e: Exception) {
      e.printStackTrace()
      when (e) {
        is UserRecoverableAuthException,
        is UserRecoverableAuthIOException,
        is AuthenticatorException,
        is AuthenticationFailedException -> {
          markMsgsWithAuthFailureState(roomDatabase, MessageState.NEW_PASSWORD_PROTECTED)
        }
      }

      return@withContext Result.failure()
    } finally {
      LogsUtil.d(TAG, "work was finished")
    }
  }

  private suspend fun prepareAndUploadMsgsToFES(account: AccountEntity) =
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

        if (list.isEmpty()) {
          break
        }

        val iterator = list.iterator()
        var msgEntity: MessageEntity? = null

        while (iterator.hasNext()) {
          val tempMsgDetails = iterator.next()
          if (tempMsgDetails.uid > lastMsgUID) {
            msgEntity = tempMsgDetails
            break
          }
        }

        if (msgEntity == null) {
          msgEntity = list[0]
        }

        lastMsgUID = msgEntity.uid

        try {
          val isPasswordProtectedMsg = msgEntity.password?.isNotEmpty() ?: false
          if (isPasswordProtectedMsg && msgEntity.isEncrypted == true) {
            val attachments = roomDatabase.attachmentDao().getAttachmentsSuspend(
              account = msgEntity.email,
              label = JavaEmailConstants.FOLDER_OUTBOX,
              uid = msgEntity.uid
            )

            val mimeMsgWithAttachments = EmailUtil.createMimeMsg(
              context = applicationContext,
              sess = Session.getDefaultInstance(Properties()),
              account = account,
              msgEntity = msgEntity,
              atts = attachments
            )

            val fromAddress = (mimeMsgWithAttachments.from.first() as InternetAddress).address
            val domain = EmailUtil.getDomain(fromAddress)

            val toCandidates = mimeMsgWithAttachments.getRecipients(Message.RecipientType.TO)
            if (toCandidates.isNotEmpty()) {
              val idToken = getGoogleIdToken()
              val replyToken = fetchReplyToken(apiRepository, domain, idToken)
              val messageUploadRequest = MessageUploadRequest(
                associateReplyToken = replyToken,
                from = fromAddress,
                to = toCandidates.map { it.toString() }
              )
              val replyInfo = Base64.getEncoder().encodeToString(
                GsonBuilder().create().toJson(messageUploadRequest).toByteArray()
              )

              val infoDiv = genInfoDiv(replyInfo)
              val originalText = getDecryptedContentFromMessage(
                mimeMsgWithAttachments,
                accountSecretKeys,
                keysStorage
              )
              val bodyWithReplyToken = originalText + "\n\n" + infoDiv

              // construct a regular plain mime message using bodyWithReplyToken + attachments
              mimeMsgWithAttachments.setRecipients(Message.RecipientType.TO, toCandidates)
              mimeMsgWithAttachments.setRecipients(Message.RecipientType.CC, emptyArray())
              mimeMsgWithAttachments.setRecipients(Message.RecipientType.BCC, emptyArray())
              val multipart = mimeMsgWithAttachments.content as Multipart
              //need to remove 'encrypted.asc' from the existing MIME
              multipart.removeBodyPart(0)
              multipart.addBodyPart(MimeBodyPart().apply { setText(bodyWithReplyToken) }, 0)
              mimeMsgWithAttachments.saveChanges()

              val rawMimeMsg = String(ByteArrayOutputStream().apply {
                mimeMsgWithAttachments.writeTo(this)
              }.toByteArray())

              val pwdEncryptedWithAttachments = PgpEncryptAndOrSign.encryptAndOrSignMsg(
                msg = rawMimeMsg,
                pubKeys = emptyList(),
                prvKeys = emptyList(),
                passphrase = Passphrase.fromPassword(
                  KeyStoreCryptoManager.decryptSuspend(String(requireNotNull(msgEntity.password)))
                )
              )

              val fesUrl = uploadMsgToFESAndReturnUrl(
                apiRepository,
                domain,
                idToken,
                messageUploadRequest,
                pwdEncryptedWithAttachments
              )

              updateExistingMimeMsgWithUrl(msgEntity, fesUrl)

              MessagesSenderWorker.enqueue(applicationContext)
            }
          }
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)

          if (!GeneralUtil.isConnected(applicationContext)) {
            if (msgEntity.msgState !== MessageState.SENT) {
              roomDatabase.msgDao().updateSuspend(msgEntity.copy(state = MessageState.QUEUED.value))
            }

            throw e
          } else {
            val newMsgState = when (e) {
              is MailConnectException -> {
                MessageState.QUEUED
              }

              is MessagingException -> {
                if (e.cause is SSLException || e.cause is SocketException) {
                  MessageState.QUEUED
                } else {
                  MessageState.ERROR_SENDING_FAILED
                }
              }

              is CopyNotSavedInSentFolderException -> MessageState.ERROR_COPY_NOT_SAVED_IN_SENT_FOLDER

              else -> {
                when (e.cause) {
                  is FileNotFoundException -> MessageState.ERROR_CACHE_PROBLEM

                  else -> MessageState.ERROR_SENDING_FAILED
                }
              }
            }

            roomDatabase.msgDao()
              .updateSuspend(msgEntity.copy(state = newMsgState.value, errorMsg = e.message))
          }

          delay(5000)
        }
      }
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

    if (messageUploadResponseResult.status != com.flowcrypt.email.api.retrofit.response.base.Result.Status.SUCCESS) {
      throw IllegalStateException("status != SUCCESS")
    }
    return@withContext requireNotNull(messageUploadResponseResult.data?.url)
  }

  private fun genInfoDiv(replyInfo: String?) =
    "<div style=\"display: none\" class=\"cryptup_reply\" cryptup-data=\"$replyInfo\"></div>"

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

  private suspend fun getGoogleIdToken(): String = withContext(Dispatchers.IO) {
    val googleSignInClient = GoogleSignIn.getClient(
      applicationContext,
      GoogleApiClientHelper.generateGoogleSignInOptions()
    )
    val silentSignIn = googleSignInClient.silentSignIn()
    if (!silentSignIn.isSuccessful) {
      throw IllegalStateException("silentSignIn.isSuccessful == false")
    }
    return@withContext requireNotNull(silentSignIn.result.idToken)
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

    if (messageReplyTokenResponseResult.status != com.flowcrypt.email.api.retrofit.response.base.Result.Status.SUCCESS) {
      throw IllegalStateException("status != SUCCESS")
    }

    return@withContext requireNotNull(messageReplyTokenResponseResult.data?.replyToken)
  }

  companion object {
    private val TAG = PreparePasswordProtectedMsgWorker::class.java.simpleName
    val NAME = PreparePasswordProtectedMsgWorker::class.java.simpleName

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          NAME,
          ExistingWorkPolicy.REPLACE,
          OneTimeWorkRequestBuilder<PreparePasswordProtectedMsgWorker>()
            .setConstraints(constraints)
            .build()
        )
    }
  }
}
