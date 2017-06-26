/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.base;

import android.accounts.Account;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.view.View;

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
    private Snackbar snackbar;

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

    /**
     * Show an information as Snackbar.
     *
     * @param view        The view to find a parent from.
     * @param messageText The text to show.  Can be formatted text.
     */
    public void showInfoSnackbar(View view, String messageText) {
        snackbar = Snackbar.make(view, messageText, Snackbar.LENGTH_INDEFINITE)
                .setAction(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                });
        snackbar.show();
    }

    /**
     * Show some information as Snackbar with custom message, action button mame and listener.
     *
     * @param view            he view to find a parent from
     * @param messageText     The text to show.  Can be formatted text
     * @param buttonName      The text of the Snackbar button
     * @param onClickListener The Snackbar button click listener.
     */
    public void showSnackbar(View view, String messageText, String buttonName,
                             @NonNull View.OnClickListener onClickListener) {
        snackbar = Snackbar.make(view, messageText, Snackbar.LENGTH_INDEFINITE)
                .setAction(buttonName, onClickListener);
        snackbar.show();
    }

    public Snackbar getSnackBar() {
        return snackbar;
    }
}
