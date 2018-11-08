/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.google;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.ui.activity.base.BaseActivity;
import com.flowcrypt.email.util.GeneralUtil;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

/**
 * This class describes methods which can be used to work with {@link GoogleApiClient}.
 *
 * @author Denis Bondarenko
 * Date: 09.10.2017
 * Time: 12:25
 * E-mail: DenBond7@gmail.com
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

  /**
   * Do sign in with Gmail account using OAuth2 mechanism.
   *
   * @param baseActivity    An instance of {@link BaseActivity}
   * @param googleApiClient An instance of {@link GoogleApiClient}
   * @param rootView        A view which will be used for showing an info {@link Snackbar}
   * @param requestCode     A request code for handling the result.
   */
  public static void signInWithGmailUsingOAuth2(BaseActivity baseActivity, GoogleApiClient googleApiClient,
                                                View rootView, int requestCode) {
    if (GeneralUtil.isInternetConnectionAvailable(baseActivity)) {
      if (googleApiClient != null && googleApiClient.isConnected()) {
        googleApiClient.clearDefaultAccountAndReconnect();
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        baseActivity.startActivityForResult(signInIntent, requestCode);
      } else {
        baseActivity.showInfoSnackbar(rootView,
            baseActivity.getString(R.string.google_api_is_not_available));
      }
    } else {
      baseActivity.showInfoSnackbar(rootView,
          baseActivity.getString(R.string.internet_connection_is_not_available));
    }
  }
}
