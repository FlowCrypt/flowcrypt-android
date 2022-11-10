/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.junit.filters

import androidx.test.filters.AbstractFilter
import com.flowcrypt.email.junit.annotations.NotReadyForCI
import org.junit.runner.Description

/**
 * @author Denis Bondarenko
 *         Date: 2/17/21
 *         Time: 5:24 PM
 *         E-mail: DenBond7@gmail.com
 */
open class NotReadyForCIFilter : AbstractFilter() {
  override fun evaluateTest(description: Description?): Boolean {
    val annotationClass = NotReadyForCI::class.java
    return (description?.testClass?.isAnnotationPresent(annotationClass) == true
        || description?.getAnnotation(annotationClass) != null)
  }

  override fun describe() = "Filter tests that can't be run on CI yet"
}
