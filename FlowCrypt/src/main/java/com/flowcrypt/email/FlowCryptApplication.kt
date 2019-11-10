/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email

import android.app.Application
import android.app.job.JobScheduler
import android.content.Context
import androidx.preference.PreferenceManager
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.jobscheduler.JobIdManager
import com.flowcrypt.email.jobscheduler.SyncJobService
import com.flowcrypt.email.ui.notifications.NotificationChannelManager
import com.flowcrypt.email.util.CacheManager
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.SharedPreferencesHelper
import com.flowcrypt.email.util.acra.CustomReportSenderFactory
import com.squareup.leakcanary.LeakCanary
import org.acra.ACRA
import org.acra.ReportField
import org.acra.annotation.ReportsCrashes
import org.acra.sender.HttpSender

/**
 * The application class for FlowCrypt. Base class for maintaining global application state. The production version.
 *
 * @author DenBond7
 * Date: 02/01/2019
 * Time: 16:43
 * E-mail: DenBond7@gmail.com
 */
@ReportsCrashes(reportSenderFactoryClasses = [CustomReportSenderFactory::class],
    formUri = "https://flowcrypt.com/api/help/acra", customReportContent = [
  ReportField.ANDROID_VERSION,
  ReportField.APP_VERSION_CODE,
  ReportField.APP_VERSION_NAME,
  ReportField.AVAILABLE_MEM_SIZE,
  ReportField.BRAND,
  ReportField.BUILD,
  ReportField.BUILD_CONFIG,
  ReportField.CRASH_CONFIGURATION,
  ReportField.CUSTOM_DATA,
  ReportField.DEVICE_FEATURES,
  ReportField.DISPLAY,
  ReportField.DUMPSYS_MEMINFO,
  ReportField.ENVIRONMENT,
  ReportField.FILE_PATH,
  ReportField.INITIAL_CONFIGURATION,
  ReportField.INSTALLATION_ID,
  ReportField.IS_SILENT,
  ReportField.PACKAGE_NAME,
  ReportField.PHONE_MODEL,
  ReportField.PRODUCT,
  ReportField.REPORT_ID,
  ReportField.STACK_TRACE,
  ReportField.TOTAL_MEM_SIZE,
  ReportField.USER_APP_START_DATE,
  ReportField.USER_CRASH_DATE,
  ReportField.USER_EMAIL]
    , httpMethod = HttpSender.Method.POST, reportType = HttpSender.Type.JSON, buildConfigClass = BuildConfig::class)
class FlowCryptApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    CacheManager.init(this)
    MsgsCacheManager.init(this)
    NotificationChannelManager.registerNotificationChannels(this)

    initLeakCanary()

    val scheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    scheduler.cancel(JobIdManager.JOB_TYPE_SYNC)
    SyncJobService.schedule(this)
  }

  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
    initAcra()
  }

  fun initAcra() {
    if (!GeneralUtil.isDebugBuild()) {
      ACRA.init(this)
    } else if (SharedPreferencesHelper.getBoolean(PreferenceManager.getDefaultSharedPreferences(this),
            Constants.PREF_KEY_IS_ACRA_ENABLED, BuildConfig.IS_ACRA_ENABLED)) {
      ACRA.init(this)
    }
  }

  /**
   * Init the LeakCanary tools if the current build is debug and detect memory leaks enabled.
   */
  fun initLeakCanary() {
    if (SharedPreferencesHelper.getBoolean(PreferenceManager.getDefaultSharedPreferences(this),
            Constants.PREF_KEY_IS_DETECT_MEMORY_LEAK_ENABLED, false)) {
      if (LeakCanary.isInAnalyzerProcess(this)) {
        // This process is dedicated to LeakCanary for heap analysis.
        // You should not init your app in this process.
        return
      }
      LeakCanary.install(this)
    }
  }
}
