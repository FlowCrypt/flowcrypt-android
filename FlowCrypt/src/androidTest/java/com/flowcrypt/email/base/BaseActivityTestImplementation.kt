/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.base

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.rule.ActivityTestRule

/**
 * @author Denis Bondarenko
 *         Date: 9/15/20
 *         Time: 1:41 PM
 *         E-mail: DenBond7@gmail.com
 */
interface BaseActivityTestImplementation {
  //todo-denbond7 remove this after refactoring
  val activityTestRule: ActivityTestRule<*>?
    get() = null

  val activityScenarioRule: ActivityScenarioRule<*>?
    get() = null

  val useIntents: Boolean
    get() = false
}