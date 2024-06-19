/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FlowCryptMimeMessage
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.InitializationData
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.workmanager.sync.DeleteDraftsWorker
import com.flowcrypt.email.jetpack.workmanager.sync.UploadDraftsWorker
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.flowcrypt.email.util.CacheManager
import com.flowcrypt.email.util.FileAndDirectoryUtils
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.pgpainless.PGPainless
import org.pgpainless.key.protection.PasswordBasedSecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.io.File
import java.util.Properties
import java.util.concurrent.TimeUnit

/**
 * @author Denys Bondarenko
 */
class DraftViewModel(
  existingDraftMessageEntity: MessageEntity? = null,
  private val gmailThreadId: String? = null,
  application: Application
) : AccountViewModel(application) {
  private var sessionDraftMessageEntity: MessageEntity? = existingDraftMessageEntity
  private var draftFingerprint = DraftFingerprint()
  private var isDeleted = false

  val draftRepeatableCheckingFlow: Flow<Long> = flow {
    while (viewModelScope.isActive) {
      delay(DELAY_TIMEOUT)
      emit(System.currentTimeMillis())
    }
  }.flowOn(Dispatchers.Default)

  private val controlledRunnerForSavingDraft = ControlledRunner<Result<Boolean>>()
  private val savingDraftMutableStateFlow: MutableStateFlow<Result<Boolean>> =
    MutableStateFlow(Result.none())
  val savingDraftStateFlow: StateFlow<Result<Boolean>> =
    savingDraftMutableStateFlow.asStateFlow()

  fun processDraft(
    coroutineScope: CoroutineScope = viewModelScope,
    currentOutgoingMessageInfo: OutgoingMessageInfo,
    showNotification: Boolean = false,
    timeToCompare: Long = Long.MAX_VALUE
  ) {
    coroutineScope.launch {
      if (isDeleted) return@launch
      val activeAccount = roomDatabase.accountDao().getActiveAccountSuspend() ?: return@launch

      //here we enable 'drafts' functionality only for Google users who use Gmail API.
      if (!activeAccount.isGoogleSignInAccount || !activeAccount.useAPI) {
        return@launch
      }

      val context: Context = getApplication()
      val isSavingDraftNeeded = isMessageChanged(currentOutgoingMessageInfo)
      if (isSavingDraftNeeded) {
        if (showNotification) {
          withContext(Dispatchers.Main) {
            context.toast(context.getString(R.string.draft_saved))
          }
        }
        withContext(Dispatchers.IO) {
          savingDraftMutableStateFlow.value = Result.loading()
          try {
            savingDraftMutableStateFlow.value =
              controlledRunnerForSavingDraft.cancelPreviousThenRun {
                return@cancelPreviousThenRun prepareAndSaveDraftForUploading(
                  currentOutgoingMessageInfo
                )
              }
          } finally {
            UploadDraftsWorker.enqueue(getApplication())
          }
        }
      } else if (showNotification && timeToCompare < draftFingerprint.timeInMilliseconds) {
        withContext(Dispatchers.Main) {
          context.toast(context.getString(R.string.draft_saved))
        }
      }
    }
  }

  fun setupWithInitializationData(
    initializationData: InitializationData,
    timeInMilliseconds: Long
  ) {
    draftFingerprint = DraftFingerprint(
      msgText = initializationData.body ?: "",
      msgSubject = initializationData.subject,
      toRecipients = initializationData.toAddresses.map { it.lowercase() }.toSet(),
      ccRecipients = initializationData.ccAddresses.map { it.lowercase() }.toSet(),
      bccRecipients = initializationData.bccAddresses.map { it.lowercase() }.toSet(),
      timeInMilliseconds = timeInMilliseconds
    )
  }

  private fun isMessageChanged(outgoingMessageInfo: OutgoingMessageInfo): Boolean {
    var isSavingDraftNeeded = false
    val currentToRecipients = outgoingMessageInfo.toRecipients?.map { internetAddress ->
      internetAddress.address.lowercase()
    }?.toSet() ?: emptySet()
    val currentCcRecipients = outgoingMessageInfo.ccRecipients?.map { internetAddress ->
      internetAddress.address.lowercase()
    }?.toSet() ?: emptySet()
    val currentBccRecipients =
      outgoingMessageInfo.bccRecipients?.map { internetAddress ->
        internetAddress.address.lowercase()
      }?.toSet() ?: emptySet()

    val isTextTheSame = if (outgoingMessageInfo.signature != null) {
      val textWithoutSignature = outgoingMessageInfo.msg?.replaceFirst(
        regex = ("\n\n" + outgoingMessageInfo.signature).toRegex(RegexOption.MULTILINE),
        replacement = ""
      )
      textWithoutSignature?.equals(draftFingerprint.msgText, true) == true
    } else {
      outgoingMessageInfo.msg?.equals(draftFingerprint.msgText, true) == true
    }

    if (!isTextTheSame
      || outgoingMessageInfo.subject != draftFingerprint.msgSubject
      || currentToRecipients != draftFingerprint.toRecipients
      || currentCcRecipients != draftFingerprint.ccRecipients
      || currentBccRecipients != draftFingerprint.bccRecipients
    ) {
      isSavingDraftNeeded = true
      draftFingerprint = DraftFingerprint(
        msgText = outgoingMessageInfo.msg ?: "",
        msgSubject = outgoingMessageInfo.subject,
        toRecipients = currentToRecipients,
        ccRecipients = currentCcRecipients,
        bccRecipients = currentBccRecipients,
      )
    }

    return isSavingDraftNeeded
  }

  private suspend fun prepareAndSaveDraftForUploading(outgoingMessageInfo: OutgoingMessageInfo): Result<Boolean> =
    withContext(Dispatchers.IO) {
      try {
        val activeAccount = getActiveAccountSuspend()
          ?: return@withContext Result.success(false)

        sessionDraftMessageEntity = if (sessionDraftMessageEntity == null) {
          genDraftMessageEntity(
            accountEntity = activeAccount,
            outgoingMessageInfo = outgoingMessageInfo
          )
        } else {
          roomDatabase.msgDao().getMsgById(sessionDraftMessageEntity?.id ?: Long.MIN_VALUE)
        }

        sessionDraftMessageEntity?.let { draftMessageEntity ->
          val mimeMessage = EmailUtil.genMessage(
            context = getApplication(),
            accountEntity = activeAccount,
            outgoingMsgInfo = outgoingMessageInfo,
            signingRequired = false,
            hideArmorMeta = activeAccount.clientConfiguration?.shouldHideArmorMeta() ?: false
          )
          val existingSnapshot = MsgsCacheManager.getMsgSnapshot(draftMessageEntity.id.toString())
          if (existingSnapshot != null) {
            existingSnapshot.getUri(0)?.let { fileUri ->
              (getApplication() as Context).contentResolver?.openInputStream(fileUri)
                ?.let { inputStream ->
                  val keys = PGPainless.readKeyRing()
                    .secretKeyRingCollection(activeAccount.servicePgpPrivateKey)

                  val decryptionStream = PgpDecryptAndOrVerify.genDecryptionStream(
                    srcInputStream = inputStream,
                    secretKeys = keys,
                    protector = PasswordBasedSecretKeyRingProtector.forKey(
                      keys.first(),
                      Passphrase.fromPassword(activeAccount.servicePgpPassphrase)
                    )
                  )

                  val oldVersion = FlowCryptMimeMessage(
                    Session.getInstance(Properties()),
                    decryptionStream
                  )

                  oldVersion.getHeader(JavaEmailConstants.HEADER_REFERENCES)?.firstOrNull()
                    ?.let { references ->
                      mimeMessage.setHeader(JavaEmailConstants.HEADER_REFERENCES, references)
                    }

                  oldVersion.getHeader(JavaEmailConstants.HEADER_IN_REPLY_TO)?.firstOrNull()
                    ?.let { inReplyTo ->
                      mimeMessage.setHeader(JavaEmailConstants.HEADER_IN_REPLY_TO, inReplyTo)
                    }
                }
            }
          }
          val draftsDir = CacheManager.getDraftDirectory(getApplication())

          val currentMsgDraftDir = draftsDir.walkTopDown().firstOrNull {
            it.name == draftMessageEntity.id.toString()
          } ?: FileAndDirectoryUtils.getDir(draftMessageEntity.id.toString(), draftsDir)

          val draftFile = File(currentMsgDraftDir, "${System.currentTimeMillis()}")
          draftFile.outputStream().use { outputStream ->
            KeyStoreCryptoManager.encryptOutputStream(outputStream) { cipherOutputStream ->
              mimeMessage.writeTo(cipherOutputStream)
            }
          }

          MsgsCacheManager.storeMsg(
            key = draftMessageEntity.id.toString(),
            msg = mimeMessage as MimeMessage,
            accountEntity = activeAccount
          )
          val messageEntityWithoutStateChange = draftMessageEntity.copy(
            subject = outgoingMessageInfo.subject,
            fromAddress = InternetAddress.toString(arrayOf(outgoingMessageInfo.from)),
            replyTo = InternetAddress.toString(arrayOf(outgoingMessageInfo.from)),
            toAddress = InternetAddress.toString(outgoingMessageInfo.toRecipients?.toTypedArray()),
            ccAddress = InternetAddress.toString(outgoingMessageInfo.ccRecipients?.toTypedArray()),
            sentDate = mimeMessage.sentDate?.time,
            receivedDate = mimeMessage.sentDate?.time
          )

          roomDatabase.msgDao().updateSuspend(
            if (isDeleted) {
              messageEntityWithoutStateChange
            } else {
              messageEntityWithoutStateChange.copy(state = MessageState.PENDING_UPLOADING_DRAFT.value)
            }
          )
          return@withContext Result.success(true)
        } ?: return@withContext Result.success(false)
      } catch (e: Exception) {
        e.printStackTrace()
        return@withContext Result.exception(e)
      }
    }

  private suspend fun genDraftMessageEntity(
    accountEntity: AccountEntity,
    outgoingMessageInfo: OutgoingMessageInfo
  ): MessageEntity =
    withContext(Dispatchers.IO) {
      val foldersManager = FoldersManager.fromDatabaseSuspend(getApplication(), accountEntity)
      val folderDrafts =
        foldersManager.folderDrafts ?: throw IllegalStateException("Drafts folder is undefined")
      val newDraftMessageEntity = MessageEntity.genMsgEntity(
        email = accountEntity.email,
        label = folderDrafts.fullName,
        uid = System.currentTimeMillis(),
        info = outgoingMessageInfo,
        flags = listOf(MessageFlag.DRAFT, MessageFlag.SEEN)
      ).copy(
        state = MessageState.PENDING_UPLOADING_DRAFT.value,
        threadId = gmailThreadId,
        labelIds = GmailApiHelper.LABEL_DRAFT
      )
      val id = roomDatabase.msgDao().insertSuspend(newDraftMessageEntity)
      return@withContext newDraftMessageEntity.copy(
        id = id,
        receivedDate = newDraftMessageEntity.sentDate
      )
    }

  fun deleteDraft(coroutineScope: CoroutineScope = viewModelScope) {
    isDeleted = true
    coroutineScope.launch {
      sessionDraftMessageEntity?.let { draftEntity ->
        roomDatabase.msgDao().updateSuspend(
          draftEntity.copy(state = MessageState.PENDING_DELETING_DRAFT.value)
        )

        DeleteDraftsWorker.enqueue(getApplication())
      }
    }
  }

  private data class DraftFingerprint(
    var msgText: String = "",
    var msgSubject: String? = null,
    val toRecipients: Set<String> = setOf(),
    val ccRecipients: Set<String> = setOf(),
    val bccRecipients: Set<String> = setOf(),
    val timeInMilliseconds: Long = System.currentTimeMillis()
  )

  companion object {
    val DELAY_TIMEOUT = TimeUnit.SECONDS.toMillis(5)
  }
}
