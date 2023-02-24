/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.junit.annotations

/**
 * Via this annotation, we can mark a class or a method that can't be run on CI yet.
 * Thanks to this annotation we can run all such methods by one call via the command line to check them.
 *
 * @author Denys Bondarenko
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class NotReadyForCI
