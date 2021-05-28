/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.flowcrypt.email.BuildConfig
import java.util.concurrent.TimeUnit

/**
 * @author Denis Bondarenko
 *         Date: 12/11/20
 *         Time: 4:52 PM
 *         E-mail: DenBond7@gmail.com
 */
class SyncInboxWorker(context: Context, params: WorkerParameters) :
  InboxIdleSyncWorker(context, params) {
  override val useIndependentConnection: Boolean = true

  companion object {
    const val GROUP_UNIQUE_TAG = BuildConfig.APPLICATION_ID + ".SYNC_INBOX"

    fun enqueuePeriodic(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

      WorkManager
        .getInstance(context.applicationContext)
        .enqueueUniquePeriodicWork(
          GROUP_UNIQUE_TAG,
          ExistingPeriodicWorkPolicy.REPLACE,
          PeriodicWorkRequestBuilder<SyncInboxWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        )
    }
  }
}