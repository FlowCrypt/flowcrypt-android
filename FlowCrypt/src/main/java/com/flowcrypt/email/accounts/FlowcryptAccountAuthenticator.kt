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
import android.text.TextUtils
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.ui.activity.SignInActivity


/**
 * @author Denis Bondarenko
 *         Date: 8/12/20
 *         Time: 4:14 PM
 *         E-mail: DenBond7@gmail.com
 */
class FlowcryptAccountAuthenticator(val context: Context) : AbstractAccountAuthenticator(context) {
  override fun getAuthTokenLabel(authTokenType: String?): String {
    return ""
  }

  override fun confirmCredentials(response: AccountAuthenticatorResponse?, account: Account?, options: Bundle?): Bundle {
    return Bundle()
  }

  override fun updateCredentials(response: AccountAuthenticatorResponse?, account: Account?, authTokenType: String?, options: Bundle?): Bundle {
    return Bundle()
  }

  override fun getAuthToken(response: AccountAuthenticatorResponse?, account: Account?, authTokenType: String?, options: Bundle?): Bundle {
    // Extract the username and password from the Account Manager, and ask
    // the server for an appropriate AuthToken.
    // Extract the username and password from the Account Manager, and ask
    // the server for an appropriate AuthToken.
    val accountManager = AccountManager.get(context)

    var authToken = accountManager.peekAuthToken(account, authTokenType)

    // Lets give another try to authenticate the user

    // Lets give another try to authenticate the user
    if (TextUtils.isEmpty(authToken)) {
      val password = accountManager.getPassword(account)
      if (password != null) {
        authToken = "some token"//AuthTokenLoader.signIn(mContext, account.name, password);
      }
    }

    // If we get an authToken - we return it

    // If we get an authToken - we return it
    if (!TextUtils.isEmpty(authToken)) {
      val result = Bundle()
      result.putString(AccountManager.KEY_ACCOUNT_NAME, account!!.name)
      result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type)
      result.putString(AccountManager.KEY_AUTHTOKEN, authToken)
      return result
    }

    // If we get here, then we couldn't access the user's password - so we
    // need to re-prompt them for their credentials. We do that by creating
    // an intent to display our AuthenticatorActivity.

    // If we get here, then we couldn't access the user's password - so we
    // need to re-prompt them for their credentials. We do that by creating
    // an intent to display our AuthenticatorActivity.
    val intent = Intent(context, SignInActivity::class.java)
    //intent.putExtra(FlowcryptAuthenticatorActivity.ARG_ACCOUNT_TYPE, attr.accountType)
    //intent.putExtra(FlowcryptAuthenticatorActivity.ARG_AUTH_TYPE, authTokenType)
    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
    val bundle = Bundle()
    bundle.putParcelable(AccountManager.KEY_INTENT, intent)
    return bundle
  }

  override fun hasFeatures(response: AccountAuthenticatorResponse?, account: Account?, features: Array<out String>?): Bundle {
    return Bundle()
  }

  override fun editProperties(response: AccountAuthenticatorResponse?, accountType: String?): Bundle {
    return Bundle()
  }

  override fun addAccount(response: AccountAuthenticatorResponse?, accountType: String?, authTokenType: String?, requiredFeatures: Array<out String>?, options: Bundle?): Bundle {
    val intent = Intent(context, SignInActivity::class.java)
    //intent.putExtra(FlowcryptAuthenticatorActivity.ARG_ACCOUNT_TYPE, attr.accountType)
    //intent.putExtra(FlowcryptAuthenticatorActivity.ARG_AUTH_TYPE, authTokenType)
    intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response)
    val bundle = Bundle()
    bundle.putParcelable(AccountManager.KEY_INTENT, intent)
    return bundle
  }

  companion object {
    val ACCOUNT_TYPE = BuildConfig.APPLICATION_ID
    val AUTH_TOKEN_TYPE_EMAIL = "email"
  }
}