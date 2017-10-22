/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.model.AuthCredentials;

/**
 * The simple POJO object which describes an account information.
 *
 * @author Denis Bondarenko
 *         Date: 14.07.2017
 *         Time: 17:44
 *         E-mail: DenBond7@gmail.com
 */

public class AccountDao implements Parcelable {
    public static final String ACCOUNT_TYPE_GOOGLE = "com.google";
    public static final String ACCOUNT_TYPE_OUTLOOK = "outlook.com";
    public static final Creator<AccountDao> CREATOR = new Creator<AccountDao>() {
        @Override
        public AccountDao createFromParcel(Parcel source) {
            return new AccountDao(source);
        }

        @Override
        public AccountDao[] newArray(int size) {
            return new AccountDao[size];
        }
    };
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
        if (TextUtils.isEmpty(accountType)) {
            if (!TextUtils.isEmpty(email)) {
                this.accountType = email.substring(email.indexOf('@') + 1, email.length());
            }
        } else {
            this.accountType = accountType;
        }
        this.displayName = displayName;
        this.givenName = givenName;
        this.familyName = familyName;
        this.photoUrl = photoUrl;
        this.authCredentials = authCredentials;
    }

    protected AccountDao(Parcel in) {
        this.email = in.readString();
        this.accountType = in.readString();
        this.displayName = in.readString();
        this.givenName = in.readString();
        this.familyName = in.readString();
        this.photoUrl = in.readString();
        this.authCredentials = in.readParcelable(AuthCredentials.class.getClassLoader());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.email);
        dest.writeString(this.accountType);
        dest.writeString(this.displayName);
        dest.writeString(this.givenName);
        dest.writeString(this.familyName);
        dest.writeString(this.photoUrl);
        dest.writeParcelable(this.authCredentials, flags);
    }

    @Nullable
    public Account getAccount() {
        return this.email == null ? null : new Account(this.email, accountType);
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
