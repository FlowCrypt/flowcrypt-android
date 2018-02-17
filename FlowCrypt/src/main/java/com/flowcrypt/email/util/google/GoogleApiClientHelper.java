/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.google;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;

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

    /**
     * Sign out from the Google account.
     */
    public static void signOutFromGoogleAccount(final Context context, GoogleApiClient googleApiClient) {
        Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (!status.isSuccess()) {
                            Toast.makeText(context,
                                    R.string.error_occurred_while_this_action_running, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
