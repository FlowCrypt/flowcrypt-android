/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.junit.filters

import com.flowcrypt.email.junit.annotations.EnterpriseTest
import org.junit.runner.Description

/**
 * @author Denys Bondarenko
 */
class EnterpriseTestsFilter : ReadyForCIAndNonFlakyFilter() {
  override fun evaluateTest(description: Description?): Boolean {
    val annotationClass = EnterpriseTest::class.java
    return super.evaluateTest(description)
        && isAnnotationPresentAtClassOrMethod(description, annotationClass)
  }

  override fun describe() = "Filter tests that should be run only for enterprise testing"
}
