/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.attester;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.base.BaseApiResponse;
import com.flowcrypt.email.api.retrofit.response.model.LookUpPublicKeyInfo;
import com.google.gson.annotations.Expose;

import java.util.ArrayList;

/**
 * Response from the API
 * "https://attester.flowcrypt.com/lookup/{id or email}"
 *
 * @author Denis Bondarenko
 * Date: 05.05.2018
 * Time: 14:01
 * E-mail: DenBond7@gmail.com
 */

public class LookUpResponse extends BaseApiResponse {

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

  @Expose
  private ArrayList<LookUpPublicKeyInfo> results;

  @Expose
  private String query;

  public LookUpResponse() {
  }

  protected LookUpResponse(Parcel in) {
    super(in);
    this.results = in.createTypedArrayList(LookUpPublicKeyInfo.CREATOR);
    this.query = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeTypedList(this.results);
    dest.writeString(this.query);
  }

  public ArrayList<LookUpPublicKeyInfo> getResults() {
    return results;
  }

  public String getQuery() {
    return query;
  }
}
