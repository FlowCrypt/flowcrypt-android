/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.view.MenuItem

/**
 * The base back stack sync [android.app.Activity]
 *
 * @author DenBond7
 * Date: 27.06.2017
 * Time: 13:26
 * E-mail: DenBond7@gmail.com
 */

abstract class BaseBackStackSyncActivity : BaseSyncActivity() {
  override val isDisplayHomeAsUpEnabled: Boolean = true

  override val isSyncEnabled: Boolean = true

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> {
        onBackPressed()
        return true
      }
    }
    return super.onOptionsItemSelected(item)
  }

  override fun onProgressReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {

  }

  override fun onSyncServiceConnected() {

  }
}
