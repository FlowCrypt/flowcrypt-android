/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source;

import android.accounts.Account;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import androidx.annotation.Nullable;

/**
 * The simple POJO object which describes an account information.
 *
 * @author Denis Bondarenko
 * Date: 14.07.2017
 * Time: 17:44
 * E-mail: DenBond7@gmail.com
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
  private boolean areContactsLoaded;
  private AuthCredentials authCreds;

  public AccountDao(String email, String accountType) {
    this(email, accountType, null, null, null, null, null, false);
  }

  public AccountDao(String email, String accountType, String displayName, String givenName,
                    String familyName, String photoUrl, AuthCredentials authCreds, boolean areContactsLoaded) {
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
    this.authCreds = authCreds;
    this.areContactsLoaded = areContactsLoaded;
  }

  public AccountDao(GoogleSignInAccount googleSignInAccount) {
    this.email = googleSignInAccount.getEmail();
    Account account = googleSignInAccount.getAccount();

    if (account != null && account.type != null) {
      this.accountType = account.type.toLowerCase();
    }

    this.displayName = googleSignInAccount.getDisplayName();
    this.givenName = googleSignInAccount.getGivenName();
    this.familyName = googleSignInAccount.getFamilyName();
    if (googleSignInAccount.getPhotoUrl() != null) {
      this.photoUrl = googleSignInAccount.getPhotoUrl().toString();
    }
  }

  protected AccountDao(Parcel in) {
    this.email = in.readString();
    this.accountType = in.readString();
    this.displayName = in.readString();
    this.givenName = in.readString();
    this.familyName = in.readString();
    this.photoUrl = in.readString();
    this.areContactsLoaded = in.readByte() != 0;
    this.authCreds = in.readParcelable(AuthCredentials.class.getClassLoader());
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
    dest.writeByte(this.areContactsLoaded ? (byte) 1 : (byte) 0);
    dest.writeParcelable(this.authCreds, flags);
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

  public AuthCredentials getAuthCreds() {
    return authCreds;
  }

  public boolean areContactsLoaded() {
    return areContactsLoaded;
  }
}
