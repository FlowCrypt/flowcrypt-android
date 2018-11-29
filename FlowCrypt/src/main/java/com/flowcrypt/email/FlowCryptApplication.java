/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email;

import android.app.Application;
import android.app.job.JobScheduler;
import android.content.Context;

import com.flowcrypt.email.jobscheduler.JobIdManager;
import com.flowcrypt.email.jobscheduler.SyncJobService;
import com.flowcrypt.email.js.JsForUiManager;
import com.flowcrypt.email.ui.NotificationChannelManager;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.SharedPreferencesHelper;
import com.squareup.leakcanary.LeakCanary;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

import androidx.fragment.app.FragmentManager;
import androidx.multidex.MultiDex;
import androidx.preference.PreferenceManager;

/**
 * The application class for FlowCrypt. Base class for maintaining global application state.
 *
 * @author DenBond7
 * Date: 25.04.2017
 * Time: 11:34
 * E-mail: DenBond7@gmail.com
 */
@ReportsCrashes(
    formUri = "https://flowcrypt.com/api/help/acra",
    customReportContent = {
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
        ReportField.LOGCAT,
        ReportField.PACKAGE_NAME,
        ReportField.PHONE_MODEL,
        ReportField.PRODUCT,
        ReportField.REPORT_ID,
        ReportField.STACK_TRACE,
        ReportField.TOTAL_MEM_SIZE,
        ReportField.USER_APP_START_DATE,
        ReportField.USER_CRASH_DATE,
        ReportField.USER_EMAIL
    },
    httpMethod = HttpSender.Method.POST,
    reportType = HttpSender.Type.JSON,
    buildConfigClass = BuildConfig.class)
public class FlowCryptApplication extends Application {

  @Override
  public void onCreate() {
    super.onCreate();
    JsForUiManager.init(this);
    NotificationChannelManager.registerNotificationChannels(this);

    intiLeakCanary();
    FragmentManager.enableDebugLogging(GeneralUtil.isDebug());

    JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
    if (scheduler != null) {
      scheduler.cancel(JobIdManager.JOB_TYPE_SYNC);
    }
    SyncJobService.schedule(this);
  }

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    MultiDex.install(this);

    if (!GeneralUtil.isDebug()) {
      ACRA.init(this);
    } else if (SharedPreferencesHelper.getBoolean(PreferenceManager.getDefaultSharedPreferences(this),
        Constants.PREFERENCES_KEY_IS_ACRA_ENABLE, BuildConfig.IS_ACRA_ENABLE)) {
      ACRA.init(this);
    }
  }

  /**
   * Init the LeakCanary tools if the current build is debug and detect memory leaks enabled.
   */
  private void intiLeakCanary() {
    if (SharedPreferencesHelper.getBoolean(
        PreferenceManager.getDefaultSharedPreferences(this),
        Constants.PREFERENCES_KEY_IS_DETECT_MEMORY_LEAK_ENABLE, false)) {
      if (LeakCanary.isInAnalyzerProcess(this)) {
        // This process is dedicated to LeakCanary for heap analysis.
        // You should not init your app in this process.
        return;
      }
      LeakCanary.install(this);
    }
  }


}
