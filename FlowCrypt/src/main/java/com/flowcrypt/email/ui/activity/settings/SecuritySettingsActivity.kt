/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings

import android.view.View

import com.flowcrypt.email.R

/**
 * This activity contains actions which related to Security options.
 *
 * @author DenBond7
 * Date: 08.08.2018.
 * Time: 10:48.
 * E-mail: DenBond7@gmail.com
 */
class SecuritySettingsActivity : BaseSettingsActivity() {
  override val contentViewResourceId: Int
    get() = R.layout.activity_security_settings

  override val rootView: View
    get() = View(this)
}
