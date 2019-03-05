/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;

/**
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 1:38 PM
 * E-mail: DenBond7@gmail.com
 */
public class KeyId implements Parcelable {
  public static final Creator<KeyId> CREATOR = new Creator<KeyId>() {
    @Override
    public KeyId createFromParcel(Parcel source) {
      return new KeyId(source);
    }

    @Override
    public KeyId[] newArray(int size) {
      return new KeyId[size];
    }
  };

  @Expose
  private String fingerprint;

  @Expose
  private String longid;

  @Expose
  private String shortid;

  @Expose
  private String keywords;

  public KeyId() {
  }

  protected KeyId(Parcel in) {
    this.fingerprint = in.readString();
    this.longid = in.readString();
    this.shortid = in.readString();
    this.keywords = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.fingerprint);
    dest.writeString(this.longid);
    dest.writeString(this.shortid);
    dest.writeString(this.keywords);
  }

  public String getFingerprint() {
    return fingerprint;
  }

  public String getLongId() {
    return longid;
  }

  public String getShortId() {
    return shortid;
  }

  public String getKeywords() {
    return keywords;
  }
}
