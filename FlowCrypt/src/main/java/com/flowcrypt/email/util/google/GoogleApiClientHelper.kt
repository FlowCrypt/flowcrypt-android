/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.google

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.base.BaseActivity
import com.flowcrypt.email.util.GeneralUtil
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.material.snackbar.Snackbar

/**
 * This class describes methods which can be used to work with [GoogleApiClient].
 *
 * @author Denis Bondarenko
 * Date: 09.10.2017
 * Time: 12:25
 * E-mail: DenBond7@gmail.com
 */

class GoogleApiClientHelper {

  companion object {
    @JvmStatic
    fun generateGoogleSignInOptions(): GoogleSignInOptions {
      return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
          .requestScopes(Scope(Constants.SCOPE_MAIL_GOOGLE_COM))
          .requestEmail()
          .build()
    }

    @JvmStatic
    fun generateGoogleApiClient(context: Context, fragmentActivity: FragmentActivity,
                                listener: GoogleApiClient.OnConnectionFailedListener,
                                connCallbacks: GoogleApiClient.ConnectionCallbacks,
                                googleSignInOptions: GoogleSignInOptions): GoogleApiClient {
      return GoogleApiClient.Builder(context)
          .enableAutoManage(fragmentActivity, listener)
          .addConnectionCallbacks(connCallbacks)
          .addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
          .build()
    }

    /**
     * Sign out from the Google account.
     */
    @JvmStatic
    fun signOutFromGoogleAccount(context: Context, googleApiClient: GoogleApiClient) {
      Auth.GoogleSignInApi.signOut(googleApiClient).setResultCallback { status ->
        if (!status.isSuccess) {
          Toast.makeText(context, R.string.error_occurred_while_this_action_running, Toast.LENGTH_SHORT).show()
        }
      }
    }

    /**
     * Do sign in with Gmail account using OAuth2 mechanism.
     *
     * @param baseActivity    An instance of [BaseActivity]
     * @param googleApiClient An instance of [GoogleApiClient]
     * @param rootView        A view which will be used for showing an info [Snackbar]
     * @param requestCode     A request code for handling the result.
     */
    @JvmStatic
    fun signInWithGmailUsingOAuth2(baseActivity: BaseActivity, googleApiClient: GoogleApiClient?,
                                   rootView: View, requestCode: Int) {
      if (GeneralUtil.isConnected(baseActivity)) {
        if (googleApiClient != null && googleApiClient.isConnected) {
          googleApiClient.clearDefaultAccountAndReconnect()
          val signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient)
          baseActivity.startActivityForResult(signInIntent, requestCode)
        } else {
          baseActivity.showInfoSnackbar(rootView, baseActivity.getString(R.string.google_api_is_not_available))
        }
      } else {
        baseActivity.showInfoSnackbar(rootView, baseActivity.getString(R.string.internet_connection_is_not_available))
      }
    }
  }
}
