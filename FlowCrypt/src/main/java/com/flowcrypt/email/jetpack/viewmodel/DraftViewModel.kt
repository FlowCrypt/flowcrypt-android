/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.InitializationData
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.DraftEntity
import com.flowcrypt.email.jetpack.workmanager.sync.UploadDraftsWorker
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
  private var lastMsgText: String? = null
  private var lastMsgSubject: String? = null
  private val lastToRecipients: MutableSet<String> = mutableSetOf()
  private val lastCcRecipients: MutableSet<String> = mutableSetOf()
  private val lastBccRecipients: MutableSet<String> = mutableSetOf()

  val draftRepeatableCheckingFlow: Flow<Long> = flow {
    while (viewModelScope.isActive) {
      delay(DELAY_TIMEOUT)
      emit(System.currentTimeMillis())
    }
  }.flowOn(Dispatchers.Default)

  fun processDraft(
    coroutineScope: CoroutineScope = viewModelScope,
    currentOutgoingMessageInfo: OutgoingMessageInfo
  ) {
    coroutineScope.launch {
      var isSavingDraftNeeded = false
      val currentToRecipients = currentOutgoingMessageInfo.toRecipients?.map { internetAddress ->
        internetAddress.address.lowercase()
      }?.toSet() ?: emptySet()
      val currentCcRecipients = currentOutgoingMessageInfo.ccRecipients?.map { internetAddress ->
        internetAddress.address.lowercase()
      }?.toSet() ?: emptySet()
      val currentBccRecipients =
        currentOutgoingMessageInfo.bccRecipients?.map { internetAddress ->
          internetAddress.address.lowercase()
        }?.toSet() ?: emptySet()

      if (currentOutgoingMessageInfo.msg != lastMsgText
        || currentOutgoingMessageInfo.subject != lastMsgSubject
        || currentToRecipients != lastToRecipients
        || currentCcRecipients != lastCcRecipients
        || currentBccRecipients != lastBccRecipients
      ) {
        isSavingDraftNeeded = true
      }

      lastMsgText = currentOutgoingMessageInfo.msg
      lastMsgSubject = currentOutgoingMessageInfo.subject
      lastToRecipients.clear()
      lastToRecipients.addAll(currentToRecipients)
      lastCcRecipients.clear()
      lastCcRecipients.addAll(currentCcRecipients)
      lastBccRecipients.clear()
      lastBccRecipients.addAll(currentBccRecipients)

      if (isSavingDraftNeeded) {
        prepareAndSaveDraftForUploading(currentOutgoingMessageInfo)
      }
    }
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
          draftFile.outputStream().use {
            mimeMessage.writeTo(it)
          }
        }
      } catch (e: Exception) {
        e.printStackTrace()
        //need to think about this one
      } finally {
        UploadDraftsWorker.enqueue(getApplication())
      }
    }

  fun setupWithInitializationData(
    initializationData: InitializationData
  ) {
    lastMsgText = initializationData.body ?: ""
    lastMsgSubject = initializationData.subject ?: ""
    lastToRecipients.addAll(initializationData.toAddresses.map { it.lowercase() })
    lastCcRecipients.addAll(initializationData.ccAddresses.map { it.lowercase() })
    lastBccRecipients.addAll(initializationData.bccAddresses.map { it.lowercase() })
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

  companion object {
    val DELAY_TIMEOUT = TimeUnit.SECONDS.toMillis(5)
  }
}
