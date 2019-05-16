/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.flowcrypt.email.model.PgpKeyInfo;

/**
 * A simple private key information object which contais a {@link PgpKeyInfo} object and the
 * private key passphrase.
 *
 * @author DenBond7
 * Date: 16.05.2017
 * Time: 13:17
 * E-mail: DenBond7@gmail.com
 */

public class PrivateKeyInfo implements Parcelable {
  public static final Parcelable.Creator<PrivateKeyInfo> CREATOR = new Parcelable.Creator<PrivateKeyInfo>() {
    @Override
    public PrivateKeyInfo createFromParcel(Parcel source) {
      return new PrivateKeyInfo(source);
    }

    @Override
    public PrivateKeyInfo[] newArray(int size) {
      return new PrivateKeyInfo[size];
    }
  };

  private PgpKeyInfo pgpKeyInfo;
  private String passphrase;

  public PrivateKeyInfo() {
  }

  public PrivateKeyInfo(PgpKeyInfo pgpKeyInfo, String passphrase) {
    this.pgpKeyInfo = pgpKeyInfo;
    this.passphrase = passphrase;
  }

  protected PrivateKeyInfo(Parcel in) {
    this.pgpKeyInfo = in.readParcelable(PgpKeyInfo.class.getClassLoader());
    this.passphrase = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(this.pgpKeyInfo, flags);
    dest.writeString(this.passphrase);
  }

  public PgpKeyInfo getPgpKeyInfo() {
    return pgpKeyInfo;
  }

  public void setPgpKeyInfo(PgpKeyInfo pgpKeyInfo) {
    this.pgpKeyInfo = pgpKeyInfo;
  }

  public String getPassphrase() {
    return passphrase;
  }

  public void setPassphrase(String passphrase) {
    this.passphrase = passphrase;
  }
}
