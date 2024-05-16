/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.junit.filters

import androidx.test.filters.FlakyTest
import com.flowcrypt.email.junit.annotations.NotReadyForCI
import org.junit.runner.Description

/**
 * @author Denys Bondarenko
 */
open class ReadyForCIAndNonFlakyFilter : BaseCustomFilter() {
  override fun evaluateTest(description: Description?): Boolean {
    val annotationClass = NotReadyForCI::class.java
    val annotationClassFlakyTest = FlakyTest::class.java
    return !isAnnotationPresentAtClassOrMethod(description, annotationClass)
        && !isAnnotationPresentAtClassOrMethod(description, annotationClassFlakyTest)
  }

  override fun describe() = "Filter tests that are ready to be run on a CI server"
}
