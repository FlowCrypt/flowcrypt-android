/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.fragment.MainSignInFragment
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
  override val rootView: View
    get() = findViewById(R.id.fragmentContainerView)

  override val isDisplayHomeAsUpEnabled: Boolean
    get() = false

  override val contentViewResourceId: Int
    get() = R.layout.activity_sign_in

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction().add(
          R.id.fragmentContainerView, MainSignInFragment()).commitNow()
    }
  }

  companion object {
    const val ACTION_ADD_ONE_MORE_ACCOUNT = BuildConfig.APPLICATION_ID + ".ACTION_ADD_ONE_MORE_ACCOUNT"

    val KEY_EXTRA_NEW_ACCOUNT =
        GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_NEW_ACCOUNT", SignInActivity::class.java)
  }
}
