/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.accounts

import android.accounts.AbstractAccountAuthenticator
import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.ApiService
import com.flowcrypt.email.ui.activity.SignInActivity
import java.util.concurrent.TimeUnit


/**
 * @author Denis Bondarenko
 *         Date: 8/12/20
 *         Time: 4:14 PM
 *         E-mail: DenBond7@gmail.com
 */
class FlowcryptAccountAuthenticator(val context: Context) : AbstractAccountAuthenticator(context) {
  override fun getAuthTokenLabel(authTokenType: String?): String {
    return BuildConfig.APPLICATION_ID + ".auth" + if (authTokenType.isNullOrEmpty()) "" else ".$authTokenType"
  }

  override fun confirmCredentials(response: AccountAuthenticatorResponse?, account: Account?, options: Bundle?): Bundle {
    return Bundle()
  }

  override fun updateCredentials(response: AccountAuthenticatorResponse?, account: Account?, authTokenType: String?, options: Bundle?): Bundle {
    return Bundle()
  }

  override fun getAuthToken(response: AccountAuthenticatorResponse?, account: Account?, authTokenType: String?, options: Bundle?): Bundle {
    account ?: return Bundle().apply {
      putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_ARGUMENTS)
      putString(AccountManager.KEY_ERROR_MESSAGE, "Should provided non-null account")
    }

    val accountManager = AccountManager.get(context)
    val email = accountManager.getUserData(account, KEY_ACCOUNT_EMAIL)

    if (email != account.name) {
      return Bundle().apply {
        putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_ARGUMENTS)
        putString(AccountManager.KEY_ERROR_MESSAGE, "Account email mismatch!")
      }
    }

    val expireAtInMillis = accountManager.getUserData(account, KEY_EXPIRES_AT)?.toLongOrNull() ?: 0
    var authToken = accountManager.peekAuthToken(account, authTokenType)

    if (authToken.isNullOrEmpty()) {
      val refreshToken = accountManager.getUserData(account, KEY_REFRESH_TOKEN)
      val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
      val apiResponse = apiService.refreshMicrosoftOAuth2Token(refreshToken).execute()
      if (apiResponse.isSuccessful) {
        val tokenResponse = apiResponse.body()
        authToken = tokenResponse?.accessToken
        accountManager.setUserData(account, KEY_REFRESH_TOKEN, tokenResponse?.refreshToken)
        accountManager.setUserData(account, KEY_EXPIRES_AT, (System.currentTimeMillis() + (tokenResponse?.expiresIn
            ?: 0L) - TimeUnit.MINUTES.toMillis(5)).toString())
      } else return Bundle().apply {
        putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_AUTHENTICATION)
        putString(AccountManager.KEY_ERROR_MESSAGE, "Couldn't fetch an access token")
      }
    }

    if (!authToken.isNullOrEmpty()) {
      return Bundle().apply {
        putString(AccountManager.KEY_ACCOUNT_NAME, account.name)
        putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
        putLong(KEY_CUSTOM_TOKEN_EXPIRY, expireAtInMillis)
        putString(AccountManager.KEY_AUTHTOKEN, authToken)
      }
    }

    return genBundleToAddNewAccount(response).apply { options?.let { putAll(options) } }
  }

  override fun hasFeatures(response: AccountAuthenticatorResponse?, account: Account?, features: Array<out String>?): Bundle {
    return Bundle()
  }

  override fun editProperties(response: AccountAuthenticatorResponse?, accountType: String?): Bundle {
    return Bundle()
  }

  override fun addAccount(response: AccountAuthenticatorResponse?, accountType: String?, authTokenType: String?, requiredFeatures: Array<out String>?, options: Bundle?): Bundle {
    if (ACCOUNT_TYPE != accountType) {
      throw IllegalArgumentException("Request to the wrong authenticator!")
    }

    return genBundleToAddNewAccount(response).apply { options?.let { putAll(options) } }
  }

  private fun genBundleToAddNewAccount(response: AccountAuthenticatorResponse?): Bundle {
    val intent = Intent(context, SignInActivity::class.java).apply {
      action = SignInActivity.ACTION_ADD_ACCOUNT_FROM_SETTINGS
      putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
    }

    return Bundle().apply {
      putParcelable(AccountManager.KEY_INTENT, intent)
    }
  }

  companion object {
    const val KEY_ACCOUNT_EMAIL = BuildConfig.APPLICATION_ID + ".KEY_ACCOUNT_EMAIL"
    const val KEY_REFRESH_TOKEN = BuildConfig.APPLICATION_ID + ".KEY_REFRESH_TOKEN"
    const val KEY_EXPIRES_AT = BuildConfig.APPLICATION_ID + ".KEY_EXPIRES_AT"
    const val ACCOUNT_TYPE = BuildConfig.APPLICATION_ID
    const val AUTH_TOKEN_TYPE_EMAIL = "email"
  }
}