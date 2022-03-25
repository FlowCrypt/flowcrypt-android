/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.google

import com.flowcrypt.email.Constants
import com.flowcrypt.email.ui.activity.BaseActivity
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
    private const val SERVER_CLIENT_ID =
      "374364070962-n83b6asllhfkhij6slijr61576lqqi3v.apps.googleusercontent.com"

    fun generateGoogleSignInOptions(): GoogleSignInOptions {
      val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)

      builder.requestScopes(Scope(Constants.SCOPE_MAIL_GOOGLE_COM))
      builder.requestEmail()
      builder.requestIdToken(SERVER_CLIENT_ID)

      return builder.build()
    }

    /**
     * Do sign in with Gmail account using OAuth2 mechanism.
     *
     * @param activity        An instance of [BaseActivity]
     * @param client          An instance of [GoogleSignInClient]
     * @param rootView        A view which will be used for showing an info [Snackbar]
     * @param requestCode     A request code for handling the result.
     */
    /*fun signInWithGmailUsingOAuth2(
      activity: BaseActivity, client: GoogleSignInClient,
      rootView: View?, requestCode: Int
    ) {
      if (GeneralUtil.isConnected(activity)) {
        client.signOut()
        activity.startActivityForResult(client.signInIntent, requestCode)
      }
    }*/
  }
}
