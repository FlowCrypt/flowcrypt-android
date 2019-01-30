/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.attester;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailsModel;
import com.flowcrypt.email.api.retrofit.response.base.BaseApiResponse;
import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * Response from the API
 * "https://attester.flowcrypt.com/lookup/email" for {@link PostLookUpEmailsModel}
 *
 * @author Denis Bondarenko
 * Date: 13.11.2017
 * Time: 15:15
 * E-mail: DenBond7@gmail.com
 */

public class LookUpEmailsResponse extends BaseApiResponse {
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

  @Expose
  private List<LookUpEmailResponse> results;


  public LookUpEmailsResponse() {
  }

  public LookUpEmailsResponse(Parcel in) {
    super(in);
    this.results = in.createTypedArrayList(LookUpEmailResponse.CREATOR);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeTypedList(this.results);
  }

  public List<LookUpEmailResponse> getResults() {
    return results;
  }
}
