/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author Denis Bondarenko
 * Date: 6/12/20
 * Time: 12:23 PM
 * E-mail: DenBond7@gmail.com
 */
class SecurityUtilsTest {
  @Test
  fun sanitizeFileNameWithSlashes() {
    val fileName = ".**/../#./%%./../sy-s/bl\\a_\\_b\\\\_l.a?"
    val sanitizedFileName = SecurityUtils.sanitizeFileName(fileName)
    val theoreticalFileName = "bla__b_l.a?"
    assertEquals(theoreticalFileName, sanitizedFileName)
  }

  @Test
  fun sanitizeFileName() {
    val fileName = "bl\\a_\\_b\\\\_l.a?"
    val sanitizedFileName = SecurityUtils.sanitizeFileName(fileName)
    val theoreticalFileName = "bla__b_l.a?"
    assertEquals(theoreticalFileName, sanitizedFileName)
  }
}
