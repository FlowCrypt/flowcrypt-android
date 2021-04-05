/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.junit.annotations

/**
 * Via this annotation, we can mark a class or a method that can be run on CI but fails for now.
 * Thanks to this annotation we can run all such methods by one call via the command line to check them.
 *
 * @author Denis Bondarenko
 *         Date: 9/23/20
 *         Time: 8:21 AM
 *         E-mail: DenBond7@gmail.com
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class CICandidateAnnotation