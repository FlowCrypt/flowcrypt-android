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
public class BaseResponse implements Parcelable {

  public static final Creator<BaseResponse> CREATOR = new Creator<BaseResponse>() {
    @Override
    public BaseResponse createFromParcel(Parcel source) {
      return new BaseResponse(source);
    }

    @Override
    public BaseResponse[] newArray(int size) {
      return new BaseResponse[size];
    }
  };

  @SerializedName("error")
  @Expose
  private ServerError serverError;

  public BaseResponse() {
  }

  public BaseResponse(Parcel in) {
    this.serverError = in.readParcelable(ServerError.class.getClassLoader());
  }

  @Override
  public String toString() {
    return "BaseResponse{" +
        "serverError=" + serverError +
        '}';
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(this.serverError, flags);
  }

  public ServerError getServerError() {
    return serverError;
  }
}
