/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.base

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.rules.TestRule

/**
 * @author Denys Bondarenko
 */
interface BaseActivityTestImplementation {
  val activityScenarioRule: ActivityScenarioRule<*>?
    get() = null

  val activeActivityRule: TestRule?
    get() = null

  val activityScenario: ActivityScenario<*>?
    get() = activityScenarioRule?.scenario

  val useIntents: Boolean
    get() = false

  val useCommonIdling: Boolean
    get() = true
}
