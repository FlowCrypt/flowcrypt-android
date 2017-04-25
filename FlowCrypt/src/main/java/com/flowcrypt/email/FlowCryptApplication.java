package com.flowcrypt.email;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

/**
 * The application class for FlowCrypt. Base class for maintaining global application state.
 *
 * @author DenBond7
 *         Date: 25.04.2017
 *         Time: 11:34
 *         E-mail: DenBond7@gmail.com
 */
@ReportsCrashes(
        mailTo = Constants.ANDROID_DEVELOPER_SUPPORT_EMAIL,
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.application_crashed)
public class FlowCryptApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        if (BuildConfig.DEBUG) {
            ACRA.init(this);
        }
    }
}
