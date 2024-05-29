/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.junit.filters

import com.flowcrypt.email.junit.annotations.DebugTest
import org.junit.runner.Description

/**
 * @author Denys Bondarenko
 */
class DebugTestsFilter : BaseCustomFilter() {
  override fun evaluateTest(description: Description?): Boolean {
    val annotationClass = DebugTest::class.java
    return isAnnotationPresentAtClassOrMethod(description, annotationClass)
  }

  override fun describe() = "Filter tests for debugging"
}
