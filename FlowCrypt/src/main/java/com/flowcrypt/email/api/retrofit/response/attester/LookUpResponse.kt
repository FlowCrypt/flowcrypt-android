/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.attester;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.base.ApiError;
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse;
import com.flowcrypt.email.api.retrofit.response.model.LookUpPublicKeyInfo;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

/**
 * Response from the API
 * "https://flowcrypt.com/attester/lookup/{id or email}"
 *
 * @author Denis Bondarenko
 * Date: 05.05.2018
 * Time: 14:01
 * E-mail: DenBond7@gmail.com
 */

public class LookUpResponse implements ApiResponse {

  public static final Creator<LookUpResponse> CREATOR = new Creator<LookUpResponse>() {
    @Override
    public LookUpResponse createFromParcel(Parcel source) {
      return new LookUpResponse(source);
    }

    @Override
    public LookUpResponse[] newArray(int size) {
      return new LookUpResponse[size];
    }
  };

  @SerializedName("error")
  @Expose
  private ApiError apiError;

  @Expose
  private ArrayList<LookUpPublicKeyInfo> results;

  @Expose
  private String query;

  public LookUpResponse() {
  }

  public LookUpResponse(Parcel in) {
    this.apiError = in.readParcelable(ApiError.class.getClassLoader());
    this.results = in.createTypedArrayList(LookUpPublicKeyInfo.CREATOR);
    this.query = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(this.apiError, flags);
    dest.writeTypedList(this.results);
    dest.writeString(this.query);
  }

  @NotNull
  @Override
  public ApiError getApiError() {
    return apiError;
  }

  public ArrayList<LookUpPublicKeyInfo> getResults() {
    return results;
  }

  public String getQuery() {
    return query;
  }
}
