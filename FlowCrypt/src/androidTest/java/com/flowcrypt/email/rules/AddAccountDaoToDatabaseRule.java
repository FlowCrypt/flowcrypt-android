/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules;

import android.support.test.InstrumentationRegistry;

import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.util.AccountDaoManager;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * @author Denis Bondarenko
 *         Date: 21.02.2018
 *         Time: 17:54
 *         E-mail: DenBond7@gmail.com
 */
public class AddAccountDaoToDatabaseRule implements TestRule {
    protected AccountDao accountDao;

    public AddAccountDaoToDatabaseRule() {
        accountDao = AccountDaoManager.getDefaultAccountDao();
    }

    public AddAccountDaoToDatabaseRule(AccountDao accountDao) {
        this.accountDao = accountDao;
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
        accountDaoSource.addRow(InstrumentationRegistry.getTargetContext(), accountDao.getAuthCredentials());
    }
}
