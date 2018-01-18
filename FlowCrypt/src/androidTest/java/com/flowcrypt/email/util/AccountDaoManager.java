/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
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

    public static AccountDao getDefaultAccountDao() {
        return TestGeneralUtil.readObjectFromResources("default_account.json", AccountDao.class);
    }
}
