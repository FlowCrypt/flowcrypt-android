/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.junit.filters

import androidx.test.filters.FlakyTest
import org.junit.runner.Description

/**
 * @author Denys Bondarenko
 */
open class NonFlakyTestsFilter : BaseCustomFilter() {
  override fun evaluateTest(description: Description?): Boolean {
    val annotationClass = FlakyTest::class.java
    return !isAnnotationPresentAtClassOrMethod(description, annotationClass)
  }

  override fun describe() = "Filter tests that are not flaky"
}
