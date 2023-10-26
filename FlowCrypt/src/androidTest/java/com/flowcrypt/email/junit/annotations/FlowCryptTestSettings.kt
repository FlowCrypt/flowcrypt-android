/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.junit.annotations

import java.lang.annotation.Inherited

/**
 * @author Denys Bondarenko
 */
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@Target(
  AnnotationTarget.FUNCTION,
  AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.PROPERTY_SETTER,
  AnnotationTarget.CLASS
)
annotation class FlowCryptTestSettings(
  /**
   * Subscribe to the common idling.
   */
  val useCommonIdling: Boolean = true,

  /**
   * Enable validation and stabbing of intents sent out by the application under test.
   */
  val useIntents: Boolean = false
)