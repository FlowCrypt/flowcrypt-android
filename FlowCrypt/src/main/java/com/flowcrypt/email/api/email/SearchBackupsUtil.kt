/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email

import javax.mail.Message
import javax.mail.internet.InternetAddress
import javax.mail.search.AndTerm
import javax.mail.search.FromTerm
import javax.mail.search.OrTerm
import javax.mail.search.RecipientTerm
import javax.mail.search.SearchTerm
import javax.mail.search.SubjectTerm

/**
 * This class describes methods for search private key backups.
 *
 * @author Denis Bondarenko
 * Date: 26.09.2017
 * Time: 10:02
 * E-mail: DenBond7@gmail.com
 */
class SearchBackupsUtil {
  companion object {
    /**
     * Generate [SearchTerm] for search the private key backups.
     *
     * @param email The email which will be used to generate [SearchTerm].
     * @return Generated [SearchTerm].
     */
    fun genSearchTerms(email: String): SearchTerm {
      val subjectTerms = OrTerm(arrayOf(
          SubjectTerm("Your CryptUp Backup"),
          SubjectTerm("Your FlowCrypt Backup"),
          SubjectTerm("Your CryptUP Backup"),
          SubjectTerm("All you need to know about CryptUP (contains a backup)"),
          SubjectTerm("CryptUP Account Backup")))
      return AndTerm(arrayOf(
          subjectTerms,
          FromTerm(InternetAddress(email)),
          RecipientTerm(Message.RecipientType.TO, InternetAddress(email))))
    }
  }
}
