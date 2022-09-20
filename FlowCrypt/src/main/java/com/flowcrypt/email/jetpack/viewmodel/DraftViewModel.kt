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
import com.flowcrypt.email.api.email.model.InitializationData
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.DraftEntity
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.workmanager.sync.UploadDraftsWorker
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.util.CacheManager
import com.flowcrypt.email.util.FileAndDirectoryUtils
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
class DraftViewModel(private val cachedDraftId: String? = null, application: Application) :
  AccountViewModel(application) {
  private var draftEntity: DraftEntity? = null
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

        if (draftEntity == null) {
          draftEntity = getDraftEntity(activeAccount)
        }

        draftEntity?.let { draftEntity ->
          val mimeMessage =
            EmailUtil.genMessage(getApplication(), activeAccount, outgoingMessageInfo)
          val draftsDir = CacheManager.getDraftDirectory(getApplication())

          val currentMsgDraftDir = draftsDir.walkTopDown().firstOrNull {
            it.name == draftEntity.id.toString()
          } ?: FileAndDirectoryUtils.getDir(draftEntity.id.toString(), draftsDir)

          val draftFile = File(currentMsgDraftDir, "${System.currentTimeMillis()}")
          draftFile.outputStream().use { outputStream ->
            KeyStoreCryptoManager.encryptOutputStream(outputStream) { cipherOutputStream ->
              mimeMessage.writeTo(cipherOutputStream)
            }
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        //need to think about this one
      } finally {
        UploadDraftsWorker.enqueue(getApplication())
      }
    }

  private suspend fun getDraftEntity(accountEntity: AccountEntity): DraftEntity =
    withContext(Dispatchers.IO) {
      val existingDraft = roomDatabase.draftDao()
        .getDraftEntity(accountEntity.email, accountEntity.accountType, cachedDraftId)

      if (existingDraft == null) {
        val newDraft = DraftEntity(
          account = accountEntity.email,
          accountType = accountEntity.accountType ?: "",
          draftId = cachedDraftId
        )
        val id = roomDatabase.draftDao().insertSuspend(newDraft)
        return@withContext newDraft.copy(id = id)
      }

      return@withContext existingDraft
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
