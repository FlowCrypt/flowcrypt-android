/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author Denys Bondarenko
 */
abstract class BaseWorker(context: Context, params: WorkerParameters) :
  CoroutineWorker(context, params) {
  protected val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

  suspend fun rescheduleIfActiveAccountWasChanged(accountEntity: AccountEntity?): Result =
    withContext(Dispatchers.IO) {
      val activeAccountEntity = roomDatabase.accountDao().getActiveAccountSuspend()
      if (activeAccountEntity?.id == accountEntity?.id) {
        return@withContext Result.success()
      } else {
        //reschedule a task if the active account was changed
        return@withContext Result.retry()
      }
    }
}
