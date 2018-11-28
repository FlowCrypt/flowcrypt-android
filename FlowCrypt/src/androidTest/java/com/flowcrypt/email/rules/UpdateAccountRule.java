/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules;

import android.content.ContentValues;
import android.content.Context;

import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;

import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import androidx.test.platform.app.InstrumentationRegistry;

/**
 * This {@link Rule} updates <b>an existed account</b> with given {@link ContentValues}
 *
 * @author Denis Bondarenko
 * Date: 15.05.2018
 * Time: 10:19
 * E-mail: DenBond7@gmail.com
 */
public class UpdateAccountRule implements TestRule {
  private AccountDao accountDao;
  private ContentValues contentValues;

  public UpdateAccountRule(AccountDao accountDao, ContentValues contentValues) {
    this.accountDao = accountDao;
    this.contentValues = contentValues;
  }

  @Override
  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        updateAccount();
        base.evaluate();
      }
    };
  }

  private void updateAccount() {
    Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
    AccountDaoSource accountDaoSource = new AccountDaoSource();
    accountDaoSource.updateAccountInformation(targetContext, accountDao.getAccount(), contentValues);
  }
}
