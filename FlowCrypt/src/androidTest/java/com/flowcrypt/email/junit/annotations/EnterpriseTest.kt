/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.junit.annotations

/**
 * Via this annotation, we can mark a class or a method that should be run only for enterprise testing.
 *
 * @author Denys Bondarenko
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class EnterpriseTest
