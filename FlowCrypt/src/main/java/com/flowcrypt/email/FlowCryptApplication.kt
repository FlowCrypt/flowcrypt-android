/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email

import android.app.Application
import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.flowcrypt.email.api.email.IMAPStoreManager
import com.flowcrypt.email.api.email.MsgsCacheManager
import com.flowcrypt.email.jetpack.workmanager.MsgsCacheCleanerWorker
import com.flowcrypt.email.jetpack.workmanager.sync.SyncInboxWorker
import com.flowcrypt.email.jobscheduler.JobIdManager
import com.flowcrypt.email.security.CryptoMigrationUtil
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.ui.notifications.NotificationChannelManager
import com.flowcrypt.email.util.CacheManager
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.SharedPreferencesHelper
import com.flowcrypt.email.util.acra.CustomReportSenderFactory
import leakcanary.LeakCanary
import org.acra.ACRA
import org.acra.ReportField
import org.acra.annotation.ReportsCrashes
import org.acra.sender.HttpSender
import java.util.*
import java.util.concurrent.TimeUnit

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
  ReportField.USER_EMAIL], httpMethod = HttpSender.Method.POST, reportType = HttpSender.Type.JSON, buildConfigClass = BuildConfig::class)
class FlowCryptApplication : Application(), Configuration.Provider {

  override fun onCreate() {
    super.onCreate()
    CryptoMigrationUtil.doMigrationIfNeeded(this)
    KeysStorageImpl.getInstance(this)
    initPerInstallationSharedPrefs()
    CacheManager.init(this)
    MsgsCacheManager.init(this)
    NotificationChannelManager.registerNotificationChannels(this)
    IMAPStoreManager.init(this)
    initLeakCanary()
    SyncInboxWorker.enqueuePeriodic(this)
    enqueueMsgsCacheCleanerWorker()
  }

  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
    initACRA()
  }

  override fun getWorkManagerConfiguration() =
      Configuration.Builder()
          .setJobSchedulerJobIdRange(JobIdManager.JOB_MAX_ID, JobIdManager.JOB_MAX_ID + 10000)
          .build()

  private fun initACRA() {
    if (!GeneralUtil.isDebugBuild()) {
      setupACRA()
    } else if (SharedPreferencesHelper.getBoolean(PreferenceManager.getDefaultSharedPreferences(this),
            Constants.PREF_KEY_IS_ACRA_ENABLED, BuildConfig.IS_ACRA_ENABLED)) {
      setupACRA()
    }
  }

  private fun setupACRA() {
    ACRA.init(this)
    val installVersion = SharedPreferencesHelper.getString(
        PreferenceManager.getDefaultSharedPreferences(this),
        Constants.PREF_KEY_INSTALL_VERSION, "unknown")
    ACRA.getErrorReporter().putCustomData(
        Constants.PREF_KEY_INSTALL_VERSION.toUpperCase(Locale.getDefault()), installVersion)
  }

  /**
   * Init the LeakCanary tools if the current build is debug and detect memory leaks enabled.
   */
  private fun initLeakCanary() {
    if (GeneralUtil.isDebugBuild()) {
      val isEnabled = SharedPreferencesHelper.getBoolean(
          PreferenceManager.getDefaultSharedPreferences(this),
          Constants.PREF_KEY_IS_DETECT_MEMORY_LEAK_ENABLED, false)
      LeakCanary.config = LeakCanary.config.copy(dumpHeap = isEnabled)
      LeakCanary.showLeakDisplayActivityLauncherIcon(showLauncherIcon = isEnabled)
    }
  }

  private fun initPerInstallationSharedPrefs() {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    if (sharedPreferences.all.isEmpty()) {
      if (!sharedPreferences.contains(Constants.PREF_KEY_INSTALL_VERSION)) {
        sharedPreferences
            .edit()
            .putString(Constants.PREF_KEY_INSTALL_VERSION, BuildConfig.VERSION_NAME)
            .apply()
      }
    }
  }

  private fun enqueueMsgsCacheCleanerWorker() {
    val periodicWorkRequestBuilder =
        PeriodicWorkRequestBuilder<MsgsCacheCleanerWorker>(1, TimeUnit.DAYS)

    val calendar = Calendar.getInstance().apply {
      add(Calendar.DAY_OF_YEAR, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 5)
    }

    val workRequest = periodicWorkRequestBuilder
        .setInitialDelay(calendar.timeInMillis, TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(this).enqueueUniquePeriodicWork(MsgsCacheCleanerWorker.NAME,
        ExistingPeriodicWorkPolicy.REPLACE, workRequest)
  }
}
