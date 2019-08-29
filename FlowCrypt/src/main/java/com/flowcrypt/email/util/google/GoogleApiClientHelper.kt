/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.google

import android.view.View
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.base.BaseActivity
import com.flowcrypt.email.util.GeneralUtil
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.material.snackbar.Snackbar

/**
 * This class describes methods which can be used to work with Google API.
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

    /**
     * Do sign in with Gmail account using OAuth2 mechanism.
     *
     * @param activity        An instance of [BaseActivity]
     * @param client          An instance of [GoogleSignInClient]
     * @param rootView        A view which will be used for showing an info [Snackbar]
     * @param requestCode     A request code for handling the result.
     */
    @JvmStatic
    fun signInWithGmailUsingOAuth2(activity: BaseActivity, client: GoogleSignInClient,
                                   rootView: View, requestCode: Int) {
      if (GeneralUtil.isConnected(activity)) {
        client.signOut()
        activity.startActivityForResult(client.signInIntent, requestCode)
      } else {
        activity.showInfoSnackbar(rootView, activity.getString(R.string.internet_connection_is_not_available))
      }
    }
  }
}
