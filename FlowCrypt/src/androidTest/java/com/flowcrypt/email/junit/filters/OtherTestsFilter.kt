/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.junit.filters

import org.junit.runner.Description

/**
 * @author Denys Bondarenko
 */
class OtherTestsFilter : BaseCustomFilter() {
  private val dependsOnMailServerFilter = DependsOnMailServerFilter()
  private val doesNotNeedMailServerFilter = DoesNotNeedMailServerFilter()
  private val enterpriseTestsFilter = EnterpriseTestsFilter()

  override fun evaluateTest(description: Description?): Boolean {
    return !dependsOnMailServerFilter.shouldRun(description)
        && !doesNotNeedMailServerFilter.shouldRun(description)
        && !enterpriseTestsFilter.shouldRun(description)
  }

  override fun describe() = "Filter tests that are not related to any conditions"
}
