/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.content.Intent
import android.os.Bundle

import com.flowcrypt.email.ui.activity.base.BaseActivity

/**
 * It's a base activity where we will use Node.js.
 *
 * @author Denis Bondarenko
 * Date: 4/4/19
 * Time: 10:34 AM
 * E-mail: DenBond7@gmail.com
 */
abstract class BaseNodeActivity : BaseActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    if (!isNodeReady) {
      startActivityForResult(Intent(this, NodeRunnerActivity::class.java), REQUEST_CODE_NODE_RUNNER)
    }

    super.onCreate(savedInstanceState)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_NODE_RUNNER -> {
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  companion object {
    const val REQUEST_CODE_NODE_RUNNER = 10000
  }
}
