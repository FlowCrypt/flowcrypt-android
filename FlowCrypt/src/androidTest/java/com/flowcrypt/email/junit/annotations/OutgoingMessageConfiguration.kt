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
annotation class OutgoingMessageConfiguration(
  val to: Array<String> = [],
  val cc: Array<String> = [],
  val bcc: Array<String> = [],
  val subject: String,
  val message: String,
  val isNew: Boolean = true,
  val timeoutToWaitSendingInMilliseconds: Long = 15000L,
  val timeoutBeforeMovingToComposeInMilliseconds: Long = 0L
)