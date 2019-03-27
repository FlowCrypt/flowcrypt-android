/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @author Denis Bondarenko
 * Date: 3/26/19
 * Time: 3:30 PM
 * E-mail: DenBond7@gmail.com
 */
public class DecryptError implements Parcelable {

  public static final Creator<DecryptError> CREATOR = new Creator<DecryptError>() {
    @Override
    public DecryptError createFromParcel(Parcel source) {
      return new DecryptError(source);
    }

    @Override
    public DecryptError[] newArray(int size) {
      return new DecryptError[size];
    }
  };
  @Expose
  private boolean success;

  @SerializedName("error")
  @Expose
  private DecryptErrorDetails details;

  @SerializedName("longids")
  @Expose
  private Longids longids;

  @Expose
  private boolean isEncrypted;

  public DecryptError() {
  }

  protected DecryptError(Parcel in) {
    this.success = in.readByte() != 0;
    this.details = in.readParcelable(DecryptErrorDetails.class.getClassLoader());
    this.longids = in.readParcelable(Longids.class.getClassLoader());
    this.isEncrypted = in.readByte() != 0;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeByte(this.success ? (byte) 1 : (byte) 0);
    dest.writeParcelable(this.details, flags);
    dest.writeParcelable(this.longids, flags);
    dest.writeByte(this.isEncrypted ? (byte) 1 : (byte) 0);
  }

  public boolean isSuccess() {
    return success;
  }

  public DecryptErrorDetails getDetails() {
    return details;
  }

  public Longids getLongids() {
    return longids;
  }

  public boolean isEncrypted() {
    return isEncrypted;
  }
}
