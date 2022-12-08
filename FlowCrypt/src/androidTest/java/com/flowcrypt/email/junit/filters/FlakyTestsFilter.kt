/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.junit.filters

import androidx.test.filters.FlakyTest
import org.junit.runner.Description

/**
 * @author Denis Bondarenko
 *         Date: 12/7/22
 *         Time: 1:25 PM
 *         E-mail: DenBond7@gmail.com
 */
class FlakyTestsFilter : BaseCustomFilter() {
  override fun evaluateTest(description: Description?): Boolean {
    val annotationClass = FlakyTest::class.java
    return description?.testClass?.isAnnotationPresent(annotationClass) == true
        || description?.getAnnotation(annotationClass) != null
  }

  override fun describe() = "Filter tests that are flaky"
}
