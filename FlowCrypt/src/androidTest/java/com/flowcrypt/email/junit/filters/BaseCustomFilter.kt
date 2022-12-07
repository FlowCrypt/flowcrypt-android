/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.junit.filters

import androidx.test.filters.AbstractFilter
import org.junit.runner.Description

/**
 * @author Denis Bondarenko
 *         Date: 12/7/22
 *         Time: 11:12 AM
 *         E-mail: DenBond7@gmail.com
 */
abstract class BaseCustomFilter : AbstractFilter() {
  protected fun isAnnotationPresentAtClassOrMethod(
    description: Description?,
    annotationClass: Class<out Annotation?>
  ): Boolean {
    return description?.testClass?.isAnnotationPresent(annotationClass) == false
        && description.getAnnotation(annotationClass) == null
  }

  protected fun isAnnotationNotPresentAtClassOrMethod(
    description: Description?,
    annotationClass: Class<out Annotation?>,
    parentFunction: (Description?) -> Boolean
  ): Boolean {
    val hasClassAnnotation = description?.testClass?.isAnnotationPresent(annotationClass) == true
    if (hasClassAnnotation) return false
    return parentFunction(description) && description?.getAnnotation(annotationClass) == null
  }
}
