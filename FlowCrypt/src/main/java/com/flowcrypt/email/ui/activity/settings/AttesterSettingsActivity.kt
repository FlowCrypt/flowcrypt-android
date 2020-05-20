/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings

import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.R
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity
import com.flowcrypt.email.util.GeneralUtil

/**
 * Basically, this Activity gets all known addresses of the user, and then submits one call with all addresses to
 * /lookup/email/ Attester API, then compares the results.
 *
 * @author DenBond7
 * Date: 13.11.2017
 * Time: 15:07
 * E-mail: DenBond7@gmail.com
 */

class AttesterSettingsActivity : BaseBackStackActivity() {
  override val contentViewResourceId: Int
    get() = R.layout.activity_attester_settings

  override val rootView: View
    get() = findViewById(R.id.attesterSettingsFragment)

  @get:VisibleForTesting
  val idlingForAttester: CountingIdlingResource = CountingIdlingResource("idlingForAttester", GeneralUtil.isDebugBuild())
}
