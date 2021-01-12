/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

import android.content.Context
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.email.IMAPStoreConnection
import com.flowcrypt.email.api.email.IMAPStoreManager
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.viewmodel.AccountViewModel
import com.flowcrypt.email.jetpack.workmanager.BaseWorker
import com.flowcrypt.email.util.exception.CommonConnectionException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.mail.Store

/**
 * @author Denis Bondarenko
 *         Date: 11/25/20
 *         Time: 6:08 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseSyncWorker(context: Context, params: WorkerParameters) : BaseWorker(context, params) {
  override val useIndependentConnection: Boolean = false
  protected val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

  abstract suspend fun runIMAPAction(accountEntity: AccountEntity, store: Store)
  abstract suspend fun runAPIAction(accountEntity: AccountEntity)

  override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
    if (isStopped) {
      return@withContext Result.success()
    }

    try {
      val activeAccountEntity = roomDatabase.accountDao().getActiveAccountSuspend()
      activeAccountEntity?.let {
        if (useIndependentConnection) {
          AccountViewModel.getAccountEntityWithDecryptedInfoSuspend(it)?.let { accountWithDecryptedInfo ->
            if (accountWithDecryptedInfo.useAPI) {
              runAPIAction(accountWithDecryptedInfo)
            } else {
              val connection = IMAPStoreConnection(applicationContext, accountWithDecryptedInfo)
              connection.store.use { store ->
                connection.executeIMAPAction {
                  runIMAPAction(activeAccountEntity, store)
                }
              }
            }
          }
        } else {
          if (activeAccountEntity.useAPI) {
            runAPIAction(activeAccountEntity)
          } else {
            val connection = IMAPStoreManager.activeConnections[activeAccountEntity.id]
            connection?.executeIMAPAction { store ->
              runIMAPAction(activeAccountEntity, store)
            }
          }
        }
      }

      return@withContext Result.success()
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

  companion object {
    const val TAG_SYNC = BuildConfig.APPLICATION_ID + ".SYNC"
  }
}