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
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.api.email.model.InitializationData
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.workmanager.sync.UploadDraftsWorker
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.util.CacheManager
import com.flowcrypt.email.util.FileAndDirectoryUtils
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * @author Denis Bondarenko
 *         Date: 9/2/22
 *         Time: 3:45 PM
 *         E-mail: DenBond7@gmail.com
 */
class DraftViewModel(
  existingDraftMessageEntity: MessageEntity? = null,
  private val gmailThreadId: String? = null,
  application: Application
) : AccountViewModel(application) {
  private var sessionDraftMessageEntity: MessageEntity? = existingDraftMessageEntity
  private var draftFingerprint = DraftFingerprint()

  val draftRepeatableCheckingFlow: Flow<Long> = flow {
    while (viewModelScope.isActive) {
      delay(DELAY_TIMEOUT)
      emit(System.currentTimeMillis())
    }
  }.flowOn(Dispatchers.Default)

  fun processDraft(
    coroutineScope: CoroutineScope = viewModelScope,
    currentOutgoingMessageInfo: OutgoingMessageInfo,
    showNotification: Boolean = false
  ) {
    coroutineScope.launch {
      val context: Context = getApplication()
      val isSavingDraftNeeded = isMessageChanged(currentOutgoingMessageInfo)
      if (isSavingDraftNeeded) {
        if (showNotification) {
          withContext(Dispatchers.Main) {
            context.toast(context.getString(R.string.saving_draft))
          }
        }
        prepareAndSaveDraftForUploading(currentOutgoingMessageInfo)
      }
    }
  }

  fun setupWithInitializationData(
    initializationData: InitializationData
  ) {
    draftFingerprint = DraftFingerprint(
      msgText = initializationData.body ?: "",
      msgSubject = initializationData.subject ?: "",
      toRecipients = initializationData.toAddresses.map { it.lowercase() }.toSet(),
      ccRecipients = initializationData.ccAddresses.map { it.lowercase() }.toSet(),
      bccRecipients = initializationData.bccAddresses.map { it.lowercase() }.toSet(),
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

    if (outgoingMessageInfo.msg != draftFingerprint.msgText
      || outgoingMessageInfo.subject != draftFingerprint.msgSubject
      || currentToRecipients != draftFingerprint.toRecipients
      || currentCcRecipients != draftFingerprint.ccRecipients
      || currentBccRecipients != draftFingerprint.bccRecipients
    ) {
      isSavingDraftNeeded = true
      draftFingerprint = DraftFingerprint(
        msgText = outgoingMessageInfo.msg,
        msgSubject = outgoingMessageInfo.subject,
        toRecipients = currentToRecipients,
        ccRecipients = currentCcRecipients,
        bccRecipients = currentBccRecipients,
      )
    }

    return isSavingDraftNeeded
  }

  private suspend fun prepareAndSaveDraftForUploading(outgoingMessageInfo: OutgoingMessageInfo) =
    withContext(Dispatchers.IO) {
      try {
        val activeAccount =
          roomDatabase.accountDao().getActiveAccountSuspend() ?: return@withContext

        sessionDraftMessageEntity = if (sessionDraftMessageEntity == null) {
          genDraftMessageEntity(
            accountEntity = activeAccount,
            outgoingMessageInfo = outgoingMessageInfo
          )
        } else {
          roomDatabase.msgDao().getDraftById(sessionDraftMessageEntity?.id ?: Long.MIN_VALUE)
        }

        sessionDraftMessageEntity?.let { draftMessageEntity ->
          val mimeMessage =
            EmailUtil.genMessage(getApplication(), activeAccount, outgoingMessageInfo)
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

          MsgsCacheManager.storeMsg(draftMessageEntity.id.toString(), mimeMessage as MimeMessage)

          roomDatabase.msgDao().updateSuspend(
            draftMessageEntity.copy(
              state = MessageState.PENDING_UPLOADING_DRAFT.value,
              subject = outgoingMessageInfo.subject,
              fromAddress = InternetAddress.toString(arrayOf(outgoingMessageInfo.from)),
              toAddress = InternetAddress.toString(outgoingMessageInfo.toRecipients?.toTypedArray())
            )
          )
        }
      } catch (e: Exception) {
        e.printStackTrace()
        //need to think about this one
      } finally {
        UploadDraftsWorker.enqueue(getApplication())
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
        threadId = gmailThreadId
      )
      val id = roomDatabase.msgDao().insertSuspend(newDraftMessageEntity)
      return@withContext newDraftMessageEntity.copy(
        id = id,
        receivedDate = newDraftMessageEntity.sentDate
      )
    }

  private data class DraftFingerprint(
    var msgText: String? = null,
    var msgSubject: String? = null,
    val toRecipients: Set<String> = mutableSetOf(),
    val ccRecipients: Set<String> = mutableSetOf(),
    val bccRecipients: Set<String> = mutableSetOf()
  )

  companion object {
    val DELAY_TIMEOUT = TimeUnit.SECONDS.toMillis(5)
  }
}
