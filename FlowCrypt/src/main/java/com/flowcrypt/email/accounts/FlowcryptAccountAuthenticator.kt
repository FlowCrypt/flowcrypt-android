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
import com.flowcrypt.email.R
import com.flowcrypt.email.api.oauth.OAuth2Helper
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.ApiService
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.ui.activity.EmailManagerActivity
import com.flowcrypt.email.ui.activity.MainActivity


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

  override fun confirmCredentials(
    response: AccountAuthenticatorResponse?,
    account: Account?,
    options: Bundle?
  ): Bundle {
    return Bundle()
  }

  override fun updateCredentials(
    response: AccountAuthenticatorResponse?,
    account: Account?,
    authTokenType: String?,
    options: Bundle?
  ): Bundle {
    return Bundle()
  }

  override fun getAuthToken(
    response: AccountAuthenticatorResponse?,
    account: Account?,
    authTokenType: String?,
    options: Bundle?
  ): Bundle {
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
    val isTokenExpired = System.currentTimeMillis() - expireAtInMillis > 0

    if (authToken.isNullOrEmpty() || isTokenExpired) {
      val encryptedRefreshToken = accountManager.getUserData(account, KEY_REFRESH_TOKEN)
      if (encryptedRefreshToken.isNullOrEmpty()) {
        return Bundle().apply {
          putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_ARGUMENTS)
          putString(
            AccountManager.KEY_ERROR_MESSAGE,
            context.getString(R.string.refrech_token_was_corrupted)
          )
        }
      }
      try {
        val refreshToken = KeyStoreCryptoManager.decrypt(encryptedRefreshToken)
        val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
        val apiResponse = apiService.refreshMicrosoftOAuth2Token(refreshToken).execute()
        if (apiResponse.isSuccessful) {
          val tokenResponse = apiResponse.body()
          authToken = KeyStoreCryptoManager.encrypt(tokenResponse?.accessToken)
          accountManager.setAuthToken(account, authTokenType, authToken)
          accountManager.setUserData(
            account,
            KEY_REFRESH_TOKEN,
            KeyStoreCryptoManager.encrypt(tokenResponse?.refreshToken)
          )
          accountManager.setUserData(
            account,
            KEY_EXPIRES_AT,
            OAuth2Helper.getExpiresAtTime(tokenResponse?.expiresIn).toString()
          )
        } else return Bundle().apply {
          //via clearing the current token we force the token updating in the next call
          accountManager.setAuthToken(account, authTokenType, null)

          val errorResponse = ApiHelper.parseAsError(context, apiResponse)
          //https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-oauth2-auth-code-flow#error-codes-for-token-endpoint-errors
          when (errorResponse?.error) {
            "invalid_grant", "invalid_client", "consent_required" -> {
              putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_BAD_AUTHENTICATION)
            }

            else -> {
              putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_NETWORK_ERROR)
            }
          }

          val errorMsg = errorResponse?.errorDescription
            ?: context.getString(R.string.could_not_fetch_access_token)
          putString(AccountManager.KEY_ERROR_MESSAGE, errorMsg)
        }
      } catch (e: Exception) {
        return Bundle().apply {
          putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_NETWORK_ERROR)
          putString(AccountManager.KEY_ERROR_MESSAGE, e.message)
        }
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

  override fun hasFeatures(
    response: AccountAuthenticatorResponse?,
    account: Account?,
    features: Array<out String>?
  ): Bundle {
    return Bundle()
  }

  override fun editProperties(
    response: AccountAuthenticatorResponse?,
    accountType: String?
  ): Bundle {
    return Bundle()
  }

  override fun addAccount(
    response: AccountAuthenticatorResponse?,
    accountType: String?,
    authTokenType: String?,
    requiredFeatures: Array<out String>?,
    options: Bundle?
  ): Bundle {
    if (ACCOUNT_TYPE != accountType) {
      throw IllegalArgumentException("Request to the wrong authenticator!")
    }

    return genBundleToAddNewAccount(response).apply { options?.let { putAll(options) } }
  }

  private fun genBundleToAddNewAccount(response: AccountAuthenticatorResponse?): Bundle {
    val intent = Intent(context, MainActivity::class.java).apply {
      action = MainActivity.ACTION_ADD_ACCOUNT_VIA_SYSTEM_SETTINGS
      putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
    }

    return Bundle().apply {
      putParcelable(AccountManager.KEY_INTENT, intent)
    }
  }

  override fun getAccountRemovalAllowed(
    response: AccountAuthenticatorResponse?,
    account: Account?
  ): Bundle {
    val bundle = super.getAccountRemovalAllowed(response, account)

    if (bundle?.containsKey(AccountManager.KEY_BOOLEAN_RESULT) == true) {
      val isRemovalAllowed = bundle.getBoolean(AccountManager.KEY_BOOLEAN_RESULT, false)
      if (isRemovalAllowed) {
        return Bundle().apply {
          putParcelable(
            AccountManager.KEY_INTENT,
            Intent(context, EmailManagerActivity::class.java).apply {
              action = EmailManagerActivity.ACTION_REMOVE_ACCOUNT_VIA_SYSTEM_SETTINGS
              putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
              putExtra(EmailManagerActivity.KEY_ACCOUNT, account)
            })
        }
      }
    }

    return Bundle().apply {
      putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false)
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
