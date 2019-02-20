/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules;


import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.PgpContact;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.List;

import androidx.test.platform.app.InstrumentationRegistry;

/**
 * This {@link org.junit.Rule} can be used for saving {@link PgpContact} to the local database.
 *
 * @author Denis Bondarenko
 * Date: 2/20/19
 * Time: 5:16 PM
 * E-mail: DenBond7@gmail.com
 */
public class AddContactsToDatabaseRule implements TestRule {
  private List<PgpContact> pgpContacts;

  public AddContactsToDatabaseRule(List<PgpContact> pgpContacts) {
    this.pgpContacts = pgpContacts;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        ContactsDaoSource contactsDaoSource = new ContactsDaoSource();
        contactsDaoSource.addRows(InstrumentationRegistry.getInstrumentation().getTargetContext(), pgpContacts);
        base.evaluate();
      }
    };
  }
}
