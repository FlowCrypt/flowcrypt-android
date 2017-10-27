/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This object describes information about an account alias.
 *
 * @author Denis Bondarenko
 *         Date: 26.10.2017
 *         Time: 16:04
 *         E-mail: DenBond7@gmail.com
 */

public class AccountAliasesDao implements Parcelable {
    public static final Creator<AccountAliasesDao> CREATOR = new Creator<AccountAliasesDao>() {
        @Override
        public AccountAliasesDao createFromParcel(Parcel source) {
            return new AccountAliasesDao(source);
        }

        @Override
        public AccountAliasesDao[] newArray(int size) {
            return new AccountAliasesDao[size];
        }
    };

    private String email;
    private String accountType;
    private String sendAsEmail;
    private String displayName;
    private boolean isDefault;
    private String verificationStatus;

    public AccountAliasesDao() {
    }

    protected AccountAliasesDao(Parcel in) {
        this.email = in.readString();
        this.accountType = in.readString();
        this.sendAsEmail = in.readString();
        this.displayName = in.readString();
        this.isDefault = in.readByte() != 0;
        this.verificationStatus = in.readString();
    }

    @Override
    public String toString() {
        return "AccountAliasesDao{" +
                "email='" + email + '\'' +
                ", accountType='" + accountType + '\'' +
                ", sendAsEmail='" + sendAsEmail + '\'' +
                ", displayName='" + displayName + '\'' +
                ", isDefault=" + isDefault +
                ", verificationStatus='" + verificationStatus + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.email);
        dest.writeString(this.accountType);
        dest.writeString(this.sendAsEmail);
        dest.writeString(this.displayName);
        dest.writeByte(this.isDefault ? (byte) 1 : (byte) 0);
        dest.writeString(this.verificationStatus);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getSendAsEmail() {
        return sendAsEmail;
    }

    public void setSendAsEmail(String sendAsEmail) {
        this.sendAsEmail = sendAsEmail;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }
}
