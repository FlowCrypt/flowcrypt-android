/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

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
  private final String keyValue;

  public PgpKeyInfo(String keyValue, String longid) {
    this.keyValue = keyValue;
    this.longid = longid;
  }

  protected PgpKeyInfo(Parcel in) {
    this.longid = in.readString();
    this.keyValue = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.longid);
    dest.writeString(this.keyValue);
  }

  public String getLongid() {
    return longid;
  }

  public String getPrivate() {
    return keyValue;
  }
}
