/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.yourorg.sample.api.retrofit.response.base;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * @author DenBond7
 */

public class ServerError implements Parcelable {

  public static final Creator<ServerError> CREATOR = new Creator<ServerError>() {
    @Override
    public ServerError createFromParcel(Parcel source) {
      return new ServerError(source);
    }

    @Override
    public ServerError[] newArray(int size) {
      return new ServerError[size];
    }
  };

  @SerializedName("message")
  @Expose
  private String msg;

  @Expose
  private String stack;

  public ServerError() {
  }

  protected ServerError(Parcel in) {
    this.msg = in.readString();
    this.stack = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.msg);
    dest.writeString(this.stack);
  }

  public String getMsg() {
    return msg;
  }

  public String getStack() {
    return stack;
  }

  @Override
  public String toString() {
    return "ServerError{" +
        "msg='" + msg + '\'' +
        ", stack='" + stack + '\'' +
        '}';
  }
}
