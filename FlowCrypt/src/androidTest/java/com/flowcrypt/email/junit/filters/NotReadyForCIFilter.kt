/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.junit.filters

import com.flowcrypt.email.junit.annotations.NotReadyForCI
import org.junit.runner.Description

/**
 * @author Denys Bondarenko
 */
open class NotReadyForCIFilter : BaseCustomFilter() {
  override fun evaluateTest(description: Description?): Boolean {
    val annotationClass = NotReadyForCI::class.java
    return isAnnotationPresentAtClassOrMethod(description, annotationClass)
  }

  override fun describe() = "Filter tests that can't be run on CI yet"
}
