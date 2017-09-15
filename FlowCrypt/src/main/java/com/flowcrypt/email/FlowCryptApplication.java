/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email;

import android.app.Application;
import android.content.Context;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.PreferenceManager;

import com.flowcrypt.email.util.SharedPreferencesHelper;
import com.squareup.leakcanary.LeakCanary;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.HttpSender;

/**
 * The application class for FlowCrypt. Base class for maintaining global application state.
 *
 * @author DenBond7
 *         Date: 25.04.2017
 *         Time: 11:34
 *         E-mail: DenBond7@gmail.com
 */
@ReportsCrashes(
        formUri = "https://api.cryptup.io/help/acra",
        httpMethod = HttpSender.Method.POST,
        reportType = HttpSender.Type.JSON,
        mailTo = Constants.ANDROID_DEVELOPER_SUPPORT_EMAIL,
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.application_crashed)
public class FlowCryptApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        intiLeakCanary();
        FragmentManager.enableDebugLogging(BuildConfig.DEBUG);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (BuildConfig.DEBUG) {
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
