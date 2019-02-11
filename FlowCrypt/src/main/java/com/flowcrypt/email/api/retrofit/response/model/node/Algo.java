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
 * Time: 1:42 PM
 * E-mail: DenBond7@gmail.com
 */
public class Algo implements Parcelable {
  public static final Creator<Algo> CREATOR = new Creator<Algo>() {
    @Override
    public Algo createFromParcel(Parcel source) {
      return new Algo(source);
    }

    @Override
    public Algo[] newArray(int size) {
      return new Algo[size];
    }
  };

  @Expose
  private String algorithm;

  @Expose
  private int algorithmId;

  @Expose
  private int bits;

  @Expose
  private String curve;

  public Algo() {
  }

  protected Algo(Parcel in) {
    this.algorithm = in.readString();
    this.algorithmId = in.readInt();
    this.bits = in.readInt();
    this.curve = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.algorithm);
    dest.writeInt(this.algorithmId);
    dest.writeInt(this.bits);
    dest.writeString(this.curve);
  }

  public String getAlgorithm() {
    return algorithm;
  }

  public int getAlgorithmId() {
    return algorithmId;
  }

  public int getBits() {
    return bits;
  }

  public String getCurve() {
    return curve;
  }
}
