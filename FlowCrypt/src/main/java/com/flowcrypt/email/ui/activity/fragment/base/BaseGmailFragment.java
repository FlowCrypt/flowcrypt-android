/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base;

import android.accounts.Account;

/**
 * The base fragment which must used when we will work with Gmail email.
 *
 * @author DenBond7
 *         Date: 04.05.2017
 *         Time: 10:29
 *         E-mail: DenBond7@gmail.com
 */

public abstract class BaseGmailFragment extends BaseFragment {

    private Account account;

    /**
     * An abstract method which will be called when current Account will be updated.
     */
    public abstract void onAccountUpdated();

    public Account getAccount() {
        return account;
    }

    /**
     * Update current account.
     *
     * @param account A new current account.
     */
    public void updateAccount(Account account) {
        boolean isNeedCallAccountUpdate;

        isNeedCallAccountUpdate = !(this.account == null && account == null) && (this.account ==
                null || account == null || !account.name.equalsIgnoreCase(this.account.name));

        this.account = account;
        if (isNeedCallAccountUpdate) {
            onAccountUpdated();
        }
    }
}
