/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util;

import com.flowcrypt.email.database.dao.source.AccountDao;

/**
 * @author Denis Bondarenko
 *         Date: 15.01.2018
 *         Time: 16:34
 *         E-mail: DenBond7@gmail.com
 */
public class AccountDaoManager {
    public static final String USER_WITH_MORE_THAN_21_LETTERS_ACCOUNT = "user_with_more_than_21_letters_account.json";

    public static AccountDao getDefaultAccountDao() {
        return TestGeneralUtil.readObjectFromResources("default_account.json", AccountDao.class);
    }

    public static AccountDao getAccountDao(String accountPath) {
        return TestGeneralUtil.readObjectFromResources(accountPath, AccountDao.class);
    }

    public static AccountDao getUserWitMoreThan21Letters() {
        return TestGeneralUtil.readObjectFromResources(USER_WITH_MORE_THAN_21_LETTERS_ACCOUNT, AccountDao.class);
    }
}
