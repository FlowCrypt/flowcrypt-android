/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.workmanager.sync.BaseSyncWorker
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

  companion object {
    inline fun <reified W : ListenableWorker> enqueueWithDefaultParameters(
      context: Context,
      uniqueWorkName: String,
      existingWorkPolicy: ExistingWorkPolicy,
      inputData: Data = Data.EMPTY
    ) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniqueWork(
          uniqueWorkName,
          existingWorkPolicy,
          OneTimeWorkRequestBuilder<W>()
            .addTag(BaseSyncWorker.TAG_SYNC)
            .setInputData(inputData)
            .setConstraints(constraints)
            .build()
        )
    }
  }
}
