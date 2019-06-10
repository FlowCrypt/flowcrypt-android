/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings

import android.view.View

import com.flowcrypt.email.R

/**
 * @author DenBond7
 * Date: 29.09.2017.
 * Time: 22:42.
 * E-mail: DenBond7@gmail.com
 */
class ExperimentalSettingsActivity : BaseSettingsActivity() {
  override val contentViewResourceId: Int
    get() = R.layout.activity_experimental_settings

  override val rootView: View
    get() = View(this)
}
