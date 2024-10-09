/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

import android.content.Context
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.IMAPStoreConnection
import com.flowcrypt.email.api.email.IMAPStoreManager
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.jetpack.workmanager.BaseWorker
import com.flowcrypt.email.util.exception.CommonConnectionException
import jakarta.mail.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author Denys Bondarenko
 */
abstract class BaseSyncWorker(context: Context, params: WorkerParameters) :
  BaseWorker(context, params), SyncInterface {

  abstract suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store)
  abstract suspend fun runAPIAction(accountEntity: AccountEntity)

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    if (isStopped) {
      return@withContext Result.success()
    }

    try {
      val activeAccountEntity = roomDatabase.accountDao().getActiveAccountSuspend()
      activeAccountEntity?.withDecryptedInfo()?.let { accountWithDecryptedInfo ->
        if (useIndependentConnection()) {
          if (accountWithDecryptedInfo.useAPI) {
            when (accountWithDecryptedInfo.accountType) {
              AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
                runAPIAction(accountWithDecryptedInfo)
              }

              else -> throw IllegalArgumentException("Unsupported provider")
            }
          } else {
            val connection = IMAPStoreConnection(applicationContext, accountWithDecryptedInfo)
            connection.store.use { store ->
              connection.executeIMAPAction {
                runIMAPAction(activeAccountEntity, store)
              }
            }
          }
        } else {
          if (activeAccountEntity.useAPI) {
            runAPIAction(activeAccountEntity)
          } else {
            val connection = IMAPStoreManager.getConnection(activeAccountEntity.id)
            connection?.executeIMAPAction { store ->
              runIMAPAction(activeAccountEntity, store)
            }
          }
        }
      }

      return@withContext rescheduleIfActiveAccountWasChanged(activeAccountEntity)
    } catch (e: Exception) {
      e.printStackTrace()
      return@withContext when (e) {
        //reschedule a task if we have a connection issue
        is CommonConnectionException -> {
          Result.retry()
        }

        else -> {
          Result.failure()
        }
      }
    }
  }

  protected suspend fun cleanSomeThreadsCache(
    threadMessageEntitiesToBeDeleted: List<MessageEntity>,
    account: AccountEntity
  ) = withContext(Dispatchers.IO) {
    val localFoldersSet = threadMessageEntitiesToBeDeleted.map { it.folder }.toSet()
    localFoldersSet.forEach { folderName ->
      val threadIdList = threadMessageEntitiesToBeDeleted.filter { it.folder == folderName }
        .mapNotNull { it.threadId }
      if (threadIdList.isNotEmpty()) {
        roomDatabase.msgDao()
          .deleteCacheForGmailThreads(account.email, folderName, threadIdList)
      }
    }
    roomDatabase.msgDao().deleteSuspend(threadMessageEntitiesToBeDeleted)
  }

  companion object {
    const val TAG_SYNC = BuildConfig.APPLICATION_ID + ".SYNC"

    suspend fun <T> executeGMailAPICall(context: Context, action: suspend () -> T): T =
      withContext(Dispatchers.IO) {
        val result = GmailApiHelper.executeWithResult {
          com.flowcrypt.email.api.retrofit.response.base.Result.success(action.invoke())
        }

        result.data ?: throw result.exception
          ?: IllegalStateException(context.getString(R.string.unknown_error))
      }
  }
}
