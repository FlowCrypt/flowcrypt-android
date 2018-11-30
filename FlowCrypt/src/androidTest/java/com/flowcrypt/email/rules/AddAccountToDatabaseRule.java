/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules;

import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.util.AccountDaoManager;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import androidx.test.platform.app.InstrumentationRegistry;

/**
 * @author Denis Bondarenko
 * Date: 21.02.2018
 * Time: 17:54
 * E-mail: DenBond7@gmail.com
 */
public class AddAccountToDatabaseRule implements TestRule {
  protected AccountDao account;

  public AddAccountToDatabaseRule() {
    account = AccountDaoManager.getDefaultAccountDao();
  }

  public AddAccountToDatabaseRule(AccountDao account) {
    this.account = account;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        saveAccountToDatabase();
        base.evaluate();
      }
    };
  }

  private void saveAccountToDatabase() throws Exception {
    AccountDaoSource accountDaoSource = new AccountDaoSource();
    accountDaoSource.addRow(InstrumentationRegistry.getInstrumentation().getTargetContext(), account
        .getAuthCredentials());
    accountDaoSource.setActiveAccount(InstrumentationRegistry.getInstrumentation().getTargetContext(), account
        .getEmail());
  }
}
