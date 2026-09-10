/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.google

import android.accounts.Account
import androidx.credentials.GetCredentialRequest
import com.flowcrypt.email.Constants
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption

/**
 * This class describes methods which can be used to work with Google API.
 *
 * @author Denys Bondarenko
 */
class GoogleApiClientHelper {

  companion object {
    const val SERVER_CLIENT_ID =
      "374364070962-n83b6asllhfkhij6slijr61576lqqi3v.apps.googleusercontent.com"

    const val ID_TOKEN_SCOPE = "audience:server:client_id:$SERVER_CLIENT_ID"

    fun generateGoogleSignInRequest(): GetCredentialRequest = GetCredentialRequest(
      listOf(GetSignInWithGoogleOption(SERVER_CLIENT_ID))
    )

    fun generateGoogleAuthorizationRequest(account: Account): AuthorizationRequest =
      AuthorizationRequest.builder()
        .setAccount(account)
        .setRequestedScopes(listOf(Scope(Constants.SCOPE_MAIL_GOOGLE_COM)))
        .build()
  }
}
