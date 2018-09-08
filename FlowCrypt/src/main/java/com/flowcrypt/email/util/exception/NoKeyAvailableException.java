/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception;

import android.content.Context;

import com.flowcrypt.email.R;

/**
 * This exception means that no key available for a given email account or alias.
 *
 * @author Denis Bondarenko
 * Date: 01.08.2018
 * Time: 12:12
 * E-mail: DenBond7@gmail.com
 */
public class NoKeyAvailableException extends FlowCryptException {
    private String email;
    private String alias;

    public NoKeyAvailableException(Context context, String email, String alias) {
        super(context.getString(R.string.no_key_available_for_your_email_account,
                context.getString(R.string.support_email)));

        this.email = email;
        this.alias = alias;
    }

    public String getEmail() {
        return email;
    }

    public String getAlias() {
        return alias;
    }
}
