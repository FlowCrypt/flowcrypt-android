/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.IMAPStoreManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.LabelEntity
import com.flowcrypt.email.jetpack.workmanager.BaseSyncWorker
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.mail.MessagingException
import javax.mail.Store

/**
 * This task does job of receiving labels of an active account.
 *
 * @author DenBond7
 * Date: 19.06.2017
 * Time: 13:34
 * E-mail: DenBond7@gmail.com
 */
class UpdateLabelsSyncTask(context: Context, params: WorkerParameters) : BaseSyncWorker(context, params) {
  override suspend fun doWork(): Result =
      withContext(Dispatchers.IO) {
        if (isStopped) {
          return@withContext Result.success()
        }

        try {
          val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
          val activeAccountEntity = roomDatabase.accountDao().getActiveAccountSuspend()
          activeAccountEntity?.let {
            val connection = IMAPStoreManager.activeConnections[activeAccountEntity.id]
            connection?.store?.let { store ->
              fetchLabels(activeAccountEntity, store)
            }
          }

          return@withContext Result.success()
        } catch (e: Exception) {
          e.printStackTrace()
          ExceptionUtil.handleError(e)
          when (e) {
            is MessagingException -> {
              return@withContext Result.retry()
            }

            else -> return@withContext Result.failure()
          }
        }
      }

  private suspend fun fetchLabels(account: AccountEntity, store: Store) = withContext(Dispatchers.IO) {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
    val folders = store.defaultFolder.list("*")
    val email = account.email

    val foldersManager = FoldersManager(account.email)
    for (folder in folders) {
      try {
        val imapFolder = folder as IMAPFolder
        foldersManager.addFolder(imapFolder, folder.getName())
      } catch (e: MessagingException) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
      }
    }

    val localFolder = LocalFolder(email, JavaEmailConstants.FOLDER_OUTBOX,
        JavaEmailConstants.FOLDER_OUTBOX, listOf(JavaEmailConstants.FOLDER_FLAG_HAS_NO_CHILDREN), false, 0, "")

    foldersManager.addFolder(localFolder)

    val existedLabels = roomDatabase.labelDao().getLabelsSuspend(email)
    val freshLabels = mutableListOf<LabelEntity>()
    for (folder in foldersManager.allFolders) {
      freshLabels.add(LabelEntity.genLabel(email, folder))
    }

    if (existedLabels.isEmpty()) {
      roomDatabase.labelDao().insertSuspend(freshLabels)
    } else {
      roomDatabase.labelDao().update(existedLabels, freshLabels)
    }
  }

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".FETCH_LABELS"

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
          .setRequiredNetworkType(NetworkType.CONNECTED)
          .build()

      WorkManager
          .getInstance(context.applicationContext)
          .enqueueUniqueWork(
              GROUP_UNIQUE_TAG,
              ExistingWorkPolicy.REPLACE,
              OneTimeWorkRequestBuilder<UpdateLabelsSyncTask>()
                  .addTag(TAG_SYNC)
                  .setConstraints(constraints)
                  .build()
          )
    }
  }
}
