/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author Denis Bondarenko
 * Date: 3/16/21
 * Time: 7:22 PM
 * E-mail: DenBond7@gmail.com
 */
class EmailUtilTest {
  @Test
  fun testGetGmailBackupSearchQuery() {
    val email = "junit@flowcrypt.com"
    val expectedString = """from:${email} to:${email}""" +
        """ (subject:"Your FlowCrypt Backup" """ +
        """OR subject: "Your CryptUp Backup" """ +
        """OR subject: "All you need to know about CryptUP (contains a backup)" """ +
        """OR subject: "CryptUP Account Backup") -is:spam"""
    val gotString = EmailUtil.getGmailBackupSearchQuery(email)
    assertEquals(expectedString, gotString)
  }
}