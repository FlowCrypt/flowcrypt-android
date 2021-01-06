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
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.LabelEntity
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
class UpdateLabelsWorker(context: Context, params: WorkerParameters) : BaseSyncWorker(context, params) {
  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    fetchAndSaveLabels(applicationContext, accountEntity, store)
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
              OneTimeWorkRequestBuilder<UpdateLabelsWorker>()
                  .addTag(TAG_SYNC)
                  .setConstraints(constraints)
                  .build()
          )
    }

    suspend fun fetchAndSaveLabels(context: Context, account: AccountEntity, store: Store) = withContext(Dispatchers.IO) {
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
      val email = account.email
      val foldersManager = FoldersManager(account.email)

      when (account.accountType) {
        AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
          val gMailLabels = GmailApiHelper.getLabels(context, account)
          for (label in gMailLabels) {
            foldersManager.addFolder(label)
          }
        }

        else -> {
          val folders = store.defaultFolder.list("*")

          for (folder in folders) {
            try {
              val imapFolder = folder as IMAPFolder
              foldersManager.addFolder(imapFolder)
            } catch (e: MessagingException) {
              e.printStackTrace()
              ExceptionUtil.handleError(e)
            }
          }
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
  }
}
