/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */
package com.flowcrypt.email.ui.activity

import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.R
import com.flowcrypt.email.jetpack.viewmodel.LauncherViewModel
import com.flowcrypt.email.ui.activity.base.BaseActivity
import com.flowcrypt.email.ui.activity.fragment.AddOtherAccountFragment
import com.flowcrypt.email.ui.activity.fragment.UserRecoverableAuthExceptionFragment
import com.flowcrypt.email.ui.activity.fragment.base.BaseOAuthFragment
import com.flowcrypt.email.util.GeneralUtil

/**
 * @author Denis Bondarenko
 * Date: 3/8/22
 * Time: 11:13 AM
 * E-mail: DenBond7@gmail.com
 */
class MainActivity : BaseActivity() {
  private val launcherViewModel: LauncherViewModel by viewModels()
  private var accountAuthenticatorResponse: AccountAuthenticatorResponse? = null
  private val resultBundle: Bundle? = null

  override val rootView: View
    get() = findViewById(R.id.fragmentContainerView)

  override val isDisplayHomeAsUpEnabled: Boolean
    get() = false

  override val contentViewResourceId: Int
    get() = R.layout.activity_main

  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen().apply {
      setKeepOnScreenCondition {
        launcherViewModel.isLoadingStateFlow.value
      }
    }

    super.onCreate(savedInstanceState)

    accountAuthenticatorResponse =
      intent.getParcelableExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE)
    accountAuthenticatorResponse?.onRequestContinued()
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
    val fragment = fragments?.firstOrNull {
      it is UserRecoverableAuthExceptionFragment
    } ?: fragments?.firstOrNull {
      it is AddOtherAccountFragment
    }
    val oAuthFragment = fragment as? BaseOAuthFragment
    oAuthFragment?.handleOAuth2Intent(intent)
  }

  companion object {
    const val ACTION_ADD_ONE_MORE_ACCOUNT =
      BuildConfig.APPLICATION_ID + ".ACTION_ADD_ONE_MORE_ACCOUNT"
    const val ACTION_ADD_ACCOUNT_VIA_SYSTEM_SETTINGS =
      BuildConfig.APPLICATION_ID + ".ACTION_ADD_ACCOUNT_VIA_SYSTEM_SETTINGS"

    val KEY_EXTRA_NEW_ACCOUNT =
      GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_NEW_ACCOUNT", MainActivity::class.java)
  }
}
