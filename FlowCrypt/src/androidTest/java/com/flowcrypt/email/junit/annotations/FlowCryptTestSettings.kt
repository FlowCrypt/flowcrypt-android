/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.junit.annotations

/**
 * @author Denys Bondarenko
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.PROPERTY_SETTER,
  AnnotationTarget.CLASS
)
annotation class FlowCryptTestSettings(
  /**
   * Should we register the common idling.
   */
  val useCommonIdling: Boolean = true
)