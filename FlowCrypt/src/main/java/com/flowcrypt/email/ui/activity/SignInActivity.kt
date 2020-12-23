/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.fragment.AddOtherAccountFragment
import com.flowcrypt.email.ui.activity.fragment.MainSignInFragment
import com.flowcrypt.email.ui.activity.fragment.UserRecoverableAuthExceptionFragment
import com.flowcrypt.email.ui.notifications.ErrorNotificationManager
import com.flowcrypt.email.util.GeneralUtil

/**
 * This [Activity] shows a screen where a user can sign in to his account.
 *
 * @author DenBond7
 * Date: 26.14.2017
 * Time: 14:50
 * E-mail: DenBond7@gmail.com
 */
class SignInActivity : BaseNodeActivity() {
  private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null
  private val resultBundle: Bundle? = null

  override val rootView: View
    get() = findViewById(R.id.fragmentContainerView)

  override val isDisplayHomeAsUpEnabled: Boolean
    get() = false

  override val contentViewResourceId: Int
    get() = R.layout.activity_sign_in

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ErrorNotificationManager.isShowingAuthErrorEnabled = false

    accountAuthenticatorResponse = intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
    accountAuthenticatorResponse?.onRequestContinued()

    if (savedInstanceState == null) {
      when (intent.action) {
        ACTION_UPDATE_OAUTH_ACCOUNT -> {
          supportFragmentManager.beginTransaction().add(
              R.id.fragmentContainerView,
              UserRecoverableAuthExceptionFragment().apply {
                arguments = intent.extras
              },
              UserRecoverableAuthExceptionFragment::class.java.simpleName
          ).commitNow()
        }

        else -> supportFragmentManager.beginTransaction().add(
            R.id.fragmentContainerView,
            MainSignInFragment(),
            MainSignInFragment::class.java.simpleName
        ).commitNow()
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    ErrorNotificationManager.isShowingAuthErrorEnabled = true
  }

  override fun finish() {
    accountAuthenticatorResponse?.let {
      if (resultBundle != null) {
        it.onResult(resultBundle)
      } else {
        it.onError(AccountManager.ERROR_CODE_CANCELED, "canceled")
      }
    }
    accountAuthenticatorResponse = null

    super.finish()
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    val fragment = when (intent?.action) {
      ACTION_UPDATE_OAUTH_ACCOUNT -> {
        supportFragmentManager
            .findFragmentByTag(UserRecoverableAuthExceptionFragment::class.java.simpleName) as UserRecoverableAuthExceptionFragment?
      }

      else -> {
        supportFragmentManager
            .findFragmentByTag(AddOtherAccountFragment::class.java.simpleName) as AddOtherAccountFragment?
      }
    }

    fragment?.handleOAuth2Intent(intent)
  }

  companion object {
    const val ACTION_ADD_ONE_MORE_ACCOUNT = BuildConfig.APPLICATION_ID + ".ACTION_ADD_ONE_MORE_ACCOUNT"
    const val ACTION_ADD_ACCOUNT_VIA_SYSTEM_SETTINGS = BuildConfig.APPLICATION_ID + ".ACTION_ADD_ACCOUNT_VIA_SYSTEM_SETTINGS"
    const val ACTION_UPDATE_OAUTH_ACCOUNT = BuildConfig.APPLICATION_ID + ".ACTION_UPDATE_OAUTH_ACCOUNT"

    val KEY_EXTRA_NEW_ACCOUNT =
        GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_NEW_ACCOUNT", SignInActivity::class.java)
  }
}
