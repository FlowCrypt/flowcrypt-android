/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast

import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.base.BaseActivity

/**
 * This activity will be used by other activities to wait while Node.js is starting. If Node.js starts we will close
 * this activity. Usually Node.js starts in a few seconds. We have disabled the "Back" action here to prevent close
 * the app.
 *
 * @author Denis Bondarenko
 * Date: 4/4/19
 * Time: 8:48 AM
 * E-mail: DenBond7@gmail.com
 */
class NodeRunnerActivity : BaseActivity() {

  override val isDisplayHomeAsUpEnabled: Boolean
    get() = false

  override val contentViewResourceId: Int
    get() = 0

  override val rootView: View
    get() = View(this)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (isNodeReady) {
      finish()
    }
  }

  override fun onBackPressed() {
    //disabled
  }

  override fun onNodeStateChanged(isReady: Boolean) {
    super.onNodeStateChanged(isReady)
    if (isReady) {
      finish()
    } else {
      Toast.makeText(this, getString(R.string.internal_node_init_error), Toast.LENGTH_LONG).show()
    }
  }
}
