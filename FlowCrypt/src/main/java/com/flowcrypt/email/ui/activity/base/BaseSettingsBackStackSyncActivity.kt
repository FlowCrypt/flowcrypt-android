/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.content.Intent
import android.view.Menu
import android.view.MenuItem

import com.flowcrypt.email.R
import com.flowcrypt.email.service.EmailSyncService
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity

/**
 * A base settings activity which uses back stack and [EmailSyncService]
 *
 * @author Denis Bondarenko
 * Date: 07.08.2018
 * Time: 15:15
 * E-mail: DenBond7@gmail.com
 */
abstract class BaseSettingsBackStackSyncActivity : BaseBackStackSyncActivity() {

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
