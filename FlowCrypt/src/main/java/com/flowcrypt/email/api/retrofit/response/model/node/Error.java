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
 * @author DenBond7
 */

public class Error implements Parcelable {

  public static final Creator<Error> CREATOR = new Creator<Error>() {
    @Override
    public Error createFromParcel(Parcel source) {
      return new Error(source);
    }

    @Override
    public Error[] newArray(int size) {
      return new Error[size];
    }
  };

  @SerializedName("message")
  @Expose
  private String msg;

  @Expose
  private String stack;

  @Expose
  private String type;

  public Error() {
  }

  protected Error(Parcel in) {
    this.msg = in.readString();
    this.stack = in.readString();
    this.type = in.readString();
  }

  @Override
  public String toString() {
    return "Error{" +
        "msg='" + msg + '\'' +
        ", stack='" + stack + '\'' +
        ", type='" + type + '\'' +
        '}';
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.msg);
    dest.writeString(this.stack);
    dest.writeString(this.type);
  }

  public String getMsg() {
    return msg;
  }

  public String getStack() {
    return stack;
  }

  public String getType() {
    return type;
  }
}
