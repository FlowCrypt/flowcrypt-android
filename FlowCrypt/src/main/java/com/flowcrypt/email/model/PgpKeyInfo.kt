/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model;

import android.os.Parcel;
import android.os.Parcelable;

public class PgpKeyInfo implements Parcelable {

  public static final Creator<PgpKeyInfo> CREATOR = new Creator<PgpKeyInfo>() {
    @Override
    public PgpKeyInfo createFromParcel(Parcel source) {
      return new PgpKeyInfo(source);
    }

    @Override
    public PgpKeyInfo[] newArray(int size) {
      return new PgpKeyInfo[size];
    }
  };

  private final String longid;
  private final String prvKey;
  private final String pubKey;

  public PgpKeyInfo(String longid, String prvKey, String pubKey) {
    this.longid = longid;
    this.prvKey = prvKey;
    this.pubKey = pubKey;
  }

  protected PgpKeyInfo(Parcel in) {
    this.longid = in.readString();
    this.prvKey = in.readString();
    this.pubKey = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.longid);
    dest.writeString(this.prvKey);
    dest.writeString(this.pubKey);
  }

  public String getLongid() {
    return longid;
  }

  public String getPrivate() {
    return prvKey;
  }

  public String getPubKey() {
    return pubKey;
  }
}
