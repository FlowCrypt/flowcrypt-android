/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.google.api.services.gmail.model.ListDraftsResponse
import jakarta.mail.Store

/**
 * @author Denis Bondarenko
 *         Date: 9/28/22
 *         Time: 6:43 PM
 *         E-mail: DenBond7@gmail.com
 */
class SyncDraftsWorker(context: Context, params: WorkerParameters) :
  BaseSyncWorker(context, params) {
  override fun useIndependentConnection(): Boolean = true

  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {

  }

  override suspend fun runAPIAction(accountEntity: AccountEntity) {
    val foldersManager = FoldersManager.fromDatabaseSuspend(applicationContext, accountEntity)
    val folderDrafts = foldersManager.folderDrafts ?: return
    val existingSyncedDrafts = roomDatabase.msgDao().getMsgsSuspend(
      account = accountEntity.email,
      folder = folderDrafts.fullName
    ).filter { it.isDraft && it.draftId?.isNotEmpty() == true }
    val existingDraftIds = existingSyncedDrafts.map { it.draftId }.toSet()

    val draftsOnServer = (GmailApiHelper.loadMsgsBaseInfo(
      context = applicationContext,
      accountEntity = accountEntity,
      localFolder = folderDrafts,
      fields = listOf("drafts/id", "drafts/message/id"),
      maxResult = 500
    ) as? ListDraftsResponse)?.drafts ?: return

    val remoteDraftIds = draftsOnServer.map { it.id }.toSet()
    val entitiesToBeDeleted = existingSyncedDrafts.filter { it.draftId !in remoteDraftIds }
    roomDatabase.msgDao().deleteSuspend(entitiesToBeDeleted)

    val newDrafts = draftsOnServer.filter { it.id !in existingDraftIds }
    val newCandidates = newDrafts.map { it.message }
    if (newCandidates.isNotEmpty()) {
      val msgs = GmailApiHelper.loadMsgsInParallel(
        context = applicationContext,
        accountEntity = accountEntity,
        messages = newCandidates.toList(),
        localFolder = folderDrafts
      )

      val msgEntities = MessageEntity.genMessageEntities(
        context = applicationContext,
        email = accountEntity.email,
        label = folderDrafts.fullName,
        msgsList = msgs,
        isNew = false,
        areAllMsgsEncrypted = accountEntity.showOnlyEncrypted ?: false,
        draftIdsMap = newDrafts.associateBy({ it.message.id }, { it.id })
      )

      roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)
      GmailApiHelper.identifyAttachments(
        msgEntities = msgEntities,
        msgs = msgs,
        account = accountEntity,
        localFolder = folderDrafts,
        roomDatabase = roomDatabase
      )
    }
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".SYNC_DRAFTS"

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          GROUP_UNIQUE_TAG,
          ExistingWorkPolicy.KEEP,
          OneTimeWorkRequestBuilder<SyncDraftsWorker>()
            .addTag(TAG_SYNC)
            .setConstraints(constraints)
            .build()
        )
    }
  }
}
