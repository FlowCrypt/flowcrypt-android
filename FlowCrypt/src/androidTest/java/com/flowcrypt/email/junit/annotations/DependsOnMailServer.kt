/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.junit.annotations

/**
 * This annotation indicates that a whole class or a single method depends on an email server
 * for successful completion.
 *
 * @author Denys Bondarenko
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class DependsOnMailServer
