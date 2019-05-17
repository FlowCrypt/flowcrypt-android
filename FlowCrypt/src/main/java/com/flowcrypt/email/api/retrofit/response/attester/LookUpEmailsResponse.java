/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.attester;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailsModel;
import com.flowcrypt.email.api.retrofit.response.base.ApiError;
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Response from the API
 * "https://flowcrypt.com/attester/lookup/email" for {@link PostLookUpEmailsModel}
 *
 * @author Denis Bondarenko
 * Date: 13.11.2017
 * Time: 15:15
 * E-mail: DenBond7@gmail.com
 */

public class LookUpEmailsResponse implements ApiResponse {
  public static final Creator<LookUpEmailsResponse> CREATOR = new Creator<LookUpEmailsResponse>() {
    @Override
    public LookUpEmailsResponse createFromParcel(Parcel source) {
      return new LookUpEmailsResponse(source);
    }

    @Override
    public LookUpEmailsResponse[] newArray(int size) {
      return new LookUpEmailsResponse[size];
    }
  };

  @SerializedName("error")
  @Expose
  private ApiError apiError;

  @Expose
  private List<LookUpEmailResponse> results;


  public LookUpEmailsResponse() {
  }

  public LookUpEmailsResponse(Parcel in) {
    this.apiError = in.readParcelable(ApiError.class.getClassLoader());
    this.results = in.createTypedArrayList(LookUpEmailResponse.CREATOR);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(this.apiError, flags);
    dest.writeTypedList(this.results);
  }

  public List<LookUpEmailResponse> getResults() {
    return results;
  }

  @NotNull
  @Override
  public ApiError getApiError() {
    return apiError;
  }
}
