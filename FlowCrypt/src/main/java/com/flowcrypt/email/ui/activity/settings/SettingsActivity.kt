/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings

import android.view.View

import com.flowcrypt.email.R

/**
 * The settings activity which contains all application settings.
 *
 * @author DenBond7
 * Date: 26.05.2017
 * Time: 9:15
 * E-mail: DenBond7@gmail.com
 */

class SettingsActivity : BaseSettingsActivity() {

  override val contentViewResourceId: Int
    get() = R.layout.activity_settings

  override val rootView: View
    get() = View(this)
}
