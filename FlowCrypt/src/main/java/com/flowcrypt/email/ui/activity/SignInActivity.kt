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
import androidx.navigation.findNavController
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.R
import com.flowcrypt.email.SingInGraphDirections
import com.flowcrypt.email.ui.activity.base.BaseActivity
import com.flowcrypt.email.ui.activity.fragment.AddOtherAccountFragment
import com.flowcrypt.email.ui.activity.fragment.UserRecoverableAuthExceptionFragment
import com.flowcrypt.email.ui.activity.fragment.base.BaseOAuthFragment
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
class SignInActivity : BaseActivity() {
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

    accountAuthenticatorResponse =
      intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
    accountAuthenticatorResponse?.onRequestContinued()

    if (savedInstanceState == null) {
      when (intent.action) {
        ACTION_UPDATE_OAUTH_ACCOUNT -> {
          findNavController(R.id.fragmentContainerView)
            .navigate(SingInGraphDirections.actionGlobalUserRecoverableAuthExceptionFragment())
        }
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
    val fragments =
      supportFragmentManager.primaryNavigationFragment?.childFragmentManager?.fragments
    val fragment = when (intent?.action) {
      ACTION_UPDATE_OAUTH_ACCOUNT -> {
        fragments?.firstOrNull {
          it is UserRecoverableAuthExceptionFragment
        }
      }

      else -> {
        fragments?.firstOrNull {
          it is AddOtherAccountFragment
        }
      }
    }

    val oAuthFragment = fragment as? BaseOAuthFragment
    oAuthFragment?.handleOAuth2Intent(intent)
  }

  companion object {
    const val ACTION_ADD_ONE_MORE_ACCOUNT =
      BuildConfig.APPLICATION_ID + ".ACTION_ADD_ONE_MORE_ACCOUNT"
    const val ACTION_ADD_ACCOUNT_VIA_SYSTEM_SETTINGS =
      BuildConfig.APPLICATION_ID + ".ACTION_ADD_ACCOUNT_VIA_SYSTEM_SETTINGS"
    const val ACTION_UPDATE_OAUTH_ACCOUNT =
      BuildConfig.APPLICATION_ID + ".ACTION_UPDATE_OAUTH_ACCOUNT"

    val KEY_EXTRA_NEW_ACCOUNT =
      GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_NEW_ACCOUNT", SignInActivity::class.java)
  }
}
