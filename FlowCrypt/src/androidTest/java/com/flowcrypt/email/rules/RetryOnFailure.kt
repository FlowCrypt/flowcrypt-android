/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

/**
 * An annotation for retry on failure. The value should be equal to or large than 0.
 *
 * @author Denis Bondarenko
 *         Date: 3/18/21
 *         Time: 12:35 PM
 *         E-mail: DenBond7@gmail.com
 */
@Target(
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.PROPERTY_SETTER,
  AnnotationTarget.ANNOTATION_CLASS
)
annotation class RetryOnFailure(val value: Int = 0)
