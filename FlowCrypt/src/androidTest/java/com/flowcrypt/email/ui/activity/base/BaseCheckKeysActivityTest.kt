/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import androidx.test.espresso.IdlingRegistry
import com.flowcrypt.email.base.BaseTest
import com.flowcrypt.email.ui.activity.CheckKeysActivity
import org.junit.After
import org.junit.Before

/**
 * @author Denis Bondarenko
 *         Date: 11/13/19
 *         Time: 3:49 PM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseCheckKeysActivityTest : BaseTest() {
  @Before
  fun registerIdlingForPassphraseChecking() {
    val activity = activityTestRule?.activity ?: return
    if (activity is CheckKeysActivity) {
      val idlingResources = activity.idlingForKeyChecking
      if (!IdlingRegistry.getInstance().resources.contains(idlingResources)) {
        IdlingRegistry.getInstance().register(activity.idlingForKeyChecking)
      }
    }
  }

  @After
  fun unregisterIdlingForPassphraseChecking() {
    val activity = activityTestRule?.activity ?: return
    if (activity is CheckKeysActivity) {
      IdlingRegistry.getInstance().unregister(activity.idlingForKeyChecking)
    }
  }
}