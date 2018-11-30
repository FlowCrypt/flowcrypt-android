/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules;

import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.List;

import androidx.test.platform.app.InstrumentationRegistry;

/**
 * @author Denis Bondarenko
 * Date: 17.08.2018
 * Time: 09:19
 * E-mail: DenBond7@gmail.com
 */
public class AddLabelsToDatabaseRule implements TestRule {
  private AccountDao account;
  private List<Folder> folders;

  public AddLabelsToDatabaseRule(AccountDao account, List<Folder> folders) {
    this.account = account;
    this.folders = folders;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        saveLabelsToDatabase();
        base.evaluate();
      }
    };
  }

  private void saveLabelsToDatabase() {
    ImapLabelsDaoSource imapLabelsDaoSource = new ImapLabelsDaoSource();
    imapLabelsDaoSource.addRows(InstrumentationRegistry.getInstrumentation().getTargetContext(), account.getEmail
        (), folders);
  }
}

