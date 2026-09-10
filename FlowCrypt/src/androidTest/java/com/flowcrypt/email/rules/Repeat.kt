/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.rules

/**
 * @author Denys Bondarenko
 */
@Retention()
@Target(
  AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER,
  AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS
)
annotation class Repeat(val value: Int = 1)
