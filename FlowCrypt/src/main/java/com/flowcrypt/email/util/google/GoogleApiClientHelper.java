/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.google;

import android.content.Context;
import android.support.v4.app.FragmentActivity;

import com.flowcrypt.email.Constants;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Scope;

/**
 * This class describes methods which can be used to work with {@link GoogleApiClient}.
 *
 * @author Denis Bondarenko
 *         Date: 09.10.2017
 *         Time: 12:25
 *         E-mail: DenBond7@gmail.com
 */

public class GoogleApiClientHelper {
    public static GoogleSignInOptions generateGoogleSignInOptions() {
        return new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Constants.SCOPE_MAIL_GOOGLE_COM))
                .requestEmail()
                .build();
    }

    public static GoogleApiClient generateGoogleApiClient(Context context, FragmentActivity fragmentActivity,
                                                          GoogleApiClient.OnConnectionFailedListener
                                                                  onConnectionFailedListener,
                                                          GoogleApiClient.ConnectionCallbacks connectionCallbacks,
                                                          GoogleSignInOptions googleSignInOptions) {
        return new GoogleApiClient.Builder(context)
                .enableAutoManage(fragmentActivity, onConnectionFailedListener)
                .addConnectionCallbacks(connectionCallbacks)
                .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .build();
    }
}
