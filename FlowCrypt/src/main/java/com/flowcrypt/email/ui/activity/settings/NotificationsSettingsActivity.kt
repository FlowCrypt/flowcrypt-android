/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings

import android.view.View

import com.flowcrypt.email.R

/**
 * This activity describes notification settings.
 *
 * @author Denis Bondarenko
 * Date: 19.07.2018
 * Time: 11:59
 * E-mail: DenBond7@gmail.com
 */
class NotificationsSettingsActivity : BaseSettingsActivity() {
  override val contentViewResourceId: Int
    get() = R.layout.activity_notifications_settings

  override val rootView: View
    get() = findViewById(R.id.layoutContent)
}
