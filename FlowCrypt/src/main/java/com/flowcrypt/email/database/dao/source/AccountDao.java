/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source;

import android.accounts.Account;
import android.support.annotation.Nullable;

import com.flowcrypt.email.api.email.model.AuthCredentials;

/**
 * The simple POJO object which describes an account information.
 *
 * @author Denis Bondarenko
 *         Date: 14.07.2017
 *         Time: 17:44
 *         E-mail: DenBond7@gmail.com
 */

public class AccountDao {
    public static final String ACCOUNT_TYPE_GOOGLE = "com.google";

    private String email;
    private String accountType;
    private String displayName;
    private String givenName;
    private String familyName;
    private String photoUrl;
    private AuthCredentials authCredentials;

    public AccountDao(String email, String accountType, String displayName, String givenName,
                      String familyName, String photoUrl, AuthCredentials authCredentials) {
        this.email = email;
        this.accountType = accountType;
        this.displayName = displayName;
        this.givenName = givenName;
        this.familyName = familyName;
        this.photoUrl = photoUrl;
        this.authCredentials = authCredentials;
    }

    @Nullable
    public Account getAccount() {
        return this.email == null ? null : new Account(this.email, ACCOUNT_TYPE_GOOGLE);
    }

    public String getEmail() {
        return email;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public AuthCredentials getAuthCredentials() {
        return authCredentials;
    }
}
