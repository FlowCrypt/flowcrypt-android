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
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.LabelEntity
import com.sun.mail.imap.IMAPFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.mail.Store

/**
 * This task does job of receiving labels of an active account.
 *
 * @author DenBond7
 * Date: 19.06.2017
 * Time: 13:34
 * E-mail: DenBond7@gmail.com
 */
class UpdateLabelsWorker(context: Context, params: WorkerParameters) :
  BaseSyncWorker(context, params) {
  override suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store) {
    fetchAndSaveLabels(applicationContext, accountEntity, store)
  }

  override suspend fun runAPIAction(accountEntity: AccountEntity) {
    fetchAndSaveLabels(applicationContext, accountEntity)
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

    suspend fun fetchAndSaveLabels(context: Context, account: AccountEntity) =
      withContext(Dispatchers.IO) {
        saveLabels(context = context, account = account) {
          when (account.accountType) {
            AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
              executeGMailAPICall(context) {
                GmailApiHelper.getLabels(context, account).map {
                  FoldersManager.generateFolder(account.email, it)
                }
              }
            }

            else -> {
              emptyList()
            }
          }
        }
      }

    suspend fun fetchAndSaveLabels(context: Context, account: AccountEntity, store: Store) =
      withContext(Dispatchers.IO) {
        saveLabels(context, account) {
          val folders = store.defaultFolder.list("*")
          folders.mapNotNull {
            try {
              FoldersManager.generateFolder(account.email, it as IMAPFolder, it.name)
            } catch (e: Exception) {
              e.printStackTrace()
              null
            }
          }
        }
      }

    private suspend fun saveLabels(
      context: Context,
      account: AccountEntity,
      action: suspend () -> List<LocalFolder>
    ) = withContext(Dispatchers.IO) {
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
      val email = account.email
      val foldersManager = FoldersManager(account.email)

      val list = action.invoke()

      for (folder in list) {
        foldersManager.addFolder(folder)
      }

      foldersManager.addFolder(
        LocalFolder(
          account = email,
          fullName = JavaEmailConstants.FOLDER_OUTBOX,
          folderAlias = JavaEmailConstants.FOLDER_OUTBOX,
          attributes = listOf(JavaEmailConstants.FOLDER_FLAG_HAS_NO_CHILDREN),
          isCustom = false,
          msgCount = 0,
          searchQuery = ""
        )
      )

      if (account.accountType == AccountEntity.ACCOUNT_TYPE_GOOGLE) {
        if (foldersManager.folderAll == null) {
          foldersManager.addFolder(
            LocalFolder(
              account = email,
              fullName = JavaEmailConstants.FOLDER_ALL_MAIL,
              folderAlias = context.getString(R.string.all_mail),
              attributes = listOf(JavaEmailConstants.FOLDER_FLAG_HAS_NO_CHILDREN),
              isCustom = false,
              msgCount = 0,
              searchQuery = ""
            )
          )
        }
      }

      roomDatabase.labelDao()
        .update(account, foldersManager.allFolders.map { LabelEntity.genLabel(account, it) })
    }
  }
}
