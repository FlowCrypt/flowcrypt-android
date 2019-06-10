/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings

import android.content.Intent
import android.view.Menu
import android.view.MenuItem

import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity

/**
 * The base settings activity which describes a general logic..
 *
 * @author DenBond7
 * Date: 30.05.2017
 * Time: 9:05
 * E-mail: DenBond7@gmail.com
 */

abstract class BaseSettingsActivity : BaseBackStackActivity() {
  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.activity_settings, menu)
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menuActionHelp -> {
        startActivity(Intent(this, FeedbackActivity::class.java))
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }
}
