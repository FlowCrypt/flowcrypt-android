/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.base;

import android.os.Parcel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The base API response implementation.
 *
 * @author Denis Bondarenko
 * Date: 24.04.2017
 * Time: 17:50
 * E-mail: DenBond7@gmail.com
 */
public class BaseApiResponse implements ApiResponse {

  public static final Creator<BaseApiResponse> CREATOR = new Creator<BaseApiResponse>() {
    @Override
    public BaseApiResponse createFromParcel(Parcel source) {
      return new BaseApiResponse(source);
    }

    @Override
    public BaseApiResponse[] newArray(int size) {
      return new BaseApiResponse[size];
    }
  };

  @SerializedName("error")
  @Expose
  private ApiError apiError;

  public BaseApiResponse() {
  }

  public BaseApiResponse(Parcel in) {
    this.apiError = in.readParcelable(ApiError.class.getClassLoader());
  }

  @Override
  public String toString() {
    return "BaseApiResponse{" +
        "apiError=" + apiError +
        '}';
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(this.apiError, flags);
  }

  public ApiError getApiError() {
    return apiError;
  }
}
