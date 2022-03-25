/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.viewbinding.ViewBinding
import com.flowcrypt.email.R
import com.flowcrypt.email.api.oauth.OAuth2Helper
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.jetpack.viewmodel.OAuth2AuthCredentialsViewModel
import com.flowcrypt.email.util.GeneralUtil
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse

/**
 * @author Denis Bondarenko
 *         Date: 12/22/20
 *         Time: 5:24 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseOAuthFragment<T : ViewBinding> : BaseFragment<T>() {
  protected val oAuth2AuthCredentialsViewModel: OAuth2AuthCredentialsViewModel by viewModels()

  protected var authRequest: AuthorizationRequest? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    savedInstanceState?.let { restoreAuthRequest(it) }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(KEY_AUTH_REQUEST, authRequest?.jsonSerializeString())
  }

  fun handleOAuth2Intent(intent: Intent?) {
    intent?.let {
      val authResponse = AuthorizationResponse.fromIntent(intent)
      val authException = AuthorizationException.fromIntent(intent)
      if (authResponse != null) {
        val schema = authResponse.request.redirectUri.scheme
        if (schema !in OAuth2Helper.SUPPORTED_SCHEMAS) {
          return
        }

        val code = authResponse.authorizationCode
        if (code != null) {
          getOAuthToken(schema, code)
        } else {
          showInfoDialog(
            dialogTitle = "",
            dialogMsg = getString(R.string.could_not_verify_response),
            useLinkify = true
          )
        }
      } else if (authException != null) {
        showInfoDialog(
          dialogTitle = getString(R.string.error_with_value, authException.error),
          dialogMsg = authException.errorDescription,
          useLinkify = true
        )
      } else {
        showInfoDialog(
          dialogTitle = getString(R.string.oauth_error),
          dialogMsg = getString(
            R.string.could_not_verify_response,
            getString(R.string.support_email)
          ),
          useLinkify = true
        )
      }
    }
  }

  private fun restoreAuthRequest(state: Bundle) {
    val serializedAuthorizationRequest = state.getString(KEY_AUTH_REQUEST)
    serializedAuthorizationRequest?.let { jsonString ->
      try {
        authRequest = AuthorizationRequest.jsonDeserialize(jsonString)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  private fun getOAuthToken(schema: String?, code: String) {
    authRequest?.let { request ->
      when (schema) {
        OAuth2Helper.MICROSOFT_OAUTH2_SCHEMA -> {
          oAuth2AuthCredentialsViewModel.getMicrosoftOAuth2Token(
            authorizeCode = code,
            authRequest = request
          )
        }
      }
    }
  }

  companion object {
    val KEY_AUTH_REQUEST =
      GeneralUtil.generateUniqueExtraKey("KEY_AUTH_REQUEST", BaseOAuthFragment::class.java)
  }
}
