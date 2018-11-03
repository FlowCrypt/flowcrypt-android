/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email;

import android.support.annotation.NonNull;

import javax.mail.Message;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.search.AndTerm;
import javax.mail.search.FromTerm;
import javax.mail.search.OrTerm;
import javax.mail.search.RecipientTerm;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

/**
 * This class describes methods for search private key backups.
 *
 * @author Denis Bondarenko
 * Date: 26.09.2017
 * Time: 10:02
 * E-mail: DenBond7@gmail.com
 */

public class SearchBackupsUtil {

  /**
   * Generate {@link SearchTerm} for search the private key backups.
   *
   * @param email The email which will be used to generate {@link SearchTerm}.
   * @return Generated {@link SearchTerm}.
   */
  @NonNull
  public static SearchTerm generateSearchTerms(String email) throws AddressException {
    SearchTerm subjectTerms = new OrTerm(new SearchTerm[]{
        new SubjectTerm("Your CryptUp Backup"),
        new SubjectTerm("Your FlowCrypt Backup"),
        new SubjectTerm("Your CryptUP Backup"),
        new SubjectTerm("All you need to know about CryptUP (contains a backup)"),
        new SubjectTerm("CryptUP Account Backup")});


    return new AndTerm(new SearchTerm[]{subjectTerms, new FromTerm(new InternetAddress(email)),
        new RecipientTerm(Message.RecipientType.TO, new InternetAddress(email))
    });
  }
}
