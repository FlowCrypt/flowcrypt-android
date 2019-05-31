/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email

import android.app.Application
import android.app.job.JobScheduler
import android.content.Context
import androidx.fragment.app.FragmentManager
import com.flowcrypt.email.jobscheduler.JobIdManager
import com.flowcrypt.email.jobscheduler.SyncJobService
import com.flowcrypt.email.ui.NotificationChannelManager
import com.flowcrypt.email.util.GeneralUtil

/**
 * The application class for FlowCrypt. Base class for maintaining global application state.
 *
 * @author Denis Bondarenko
 * Date: 2/1/19
 * Time: 4:53 PM
 * E-mail: DenBond7@gmail.com
 */
abstract class BaseApplication : Application() {

  abstract fun initAcra()

  abstract fun initLeakCanary()

  override fun onCreate() {
    super.onCreate()
    NotificationChannelManager.registerNotificationChannels(this)

    initLeakCanary()
    FragmentManager.enableDebugLogging(GeneralUtil.isDebugBuild())

    val scheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    scheduler.cancel(JobIdManager.JOB_TYPE_SYNC)
    SyncJobService.schedule(this)
  }

  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
    initAcra()
  }
}

