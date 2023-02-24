/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.google

import com.flowcrypt.email.Constants
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope

/**
 * This class describes methods which can be used to work with Google API.
 *
 * @author Denys Bondarenko
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
  }
}
