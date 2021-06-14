/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings

import android.app.Activity
import android.os.Bundle
import android.view.View
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity
import com.flowcrypt.email.ui.activity.fragment.PrivateKeysListFragment

/**
 * This [Activity] shows information about available keys in the database.
 *
 * Here we can import new keys.
 *
 * @author DenBond7
 * Date: 29.05.2017
 * Time: 11:30
 * E-mail: DenBond7@gmail.com
 */
class KeysSettingsActivity : BaseBackStackActivity() {
  override val contentViewResourceId: Int
    get() = R.layout.activity_keys_settings

  override val rootView: View
    get() = View(this)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (savedInstanceState == null) {
      val keysListFragment = PrivateKeysListFragment.newInstance()
      supportFragmentManager.beginTransaction().replace(R.id.layoutContent, keysListFragment)
        .commitNow()
    }
  }
}
