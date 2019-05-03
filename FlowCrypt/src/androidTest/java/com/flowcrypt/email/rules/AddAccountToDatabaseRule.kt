/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import androidx.test.platform.app.InstrumentationRegistry
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.util.AccountDaoManager
import org.junit.runner.Description

/**
 * @author Denis Bondarenko
 * Date: 21.02.2018
 * Time: 17:54
 * E-mail: DenBond7@gmail.com
 */
class AddAccountToDatabaseRule implements TestRule {
  protected AccountDao account

  public AddAccountToDatabaseRule() {
    account = AccountDaoManager.getDefaultAccountDao()
  }

  public AddAccountToDatabaseRule(AccountDao account) {
    this.account = account
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        saveAccountToDatabase()
        base.evaluate()
      }
    }
  }

  public AccountDao getAccount {
    return account
  }

  private void saveAccountToDatabase() throws Exception {
    AccountDaoSource accountDaoSource = new AccountDaoSource()
    accountDaoSource.addRow(InstrumentationRegistry.getInstrumentation().targetContext, account.getAuthCreds())
    accountDaoSource.setActiveAccount(InstrumentationRegistry.getInstrumentation().targetContext, account
        .getEmail())
  }
}
