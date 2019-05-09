/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.attester;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.base.BaseApiResponse;
import com.google.gson.annotations.Expose;

/**
 * This class describes a response from the https://flowcrypt.com/attester/initial/legacy_submit API.
 * <p>
 * <code>POST /initial/legacy_submit
 * response(200): {
 * "saved" (True, False)  # successfuly saved pubkey
 * [voluntary] "error" (<type 'str'>)  # error detail, if not saved
 * }</code>
 *
 * @author Denis Bondarenko
 * Date: 15.01.2018
 * Time: 16:30
 * E-mail: DenBond7@gmail.com
 */

public class InitialLegacySubmitResponse extends BaseApiResponse {

  public static final Creator<InitialLegacySubmitResponse> CREATOR = new Creator<InitialLegacySubmitResponse>() {
    @Override
    public InitialLegacySubmitResponse createFromParcel(Parcel source) {
      return new InitialLegacySubmitResponse(source);
    }

    @Override
    public InitialLegacySubmitResponse[] newArray(int size) {
      return new InitialLegacySubmitResponse[size];
    }
  };

  @Expose
  private boolean saved;

  public InitialLegacySubmitResponse() {
  }

  protected InitialLegacySubmitResponse(Parcel in) {
    super(in);
    this.saved = in.readByte() != 0;
  }

  @Override
  public String toString() {
    return "InitialLegacySubmitResponse{" +
        "saved=" + saved +
        "} " + super.toString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeByte(this.saved ? (byte) 1 : (byte) 0);
  }

  public boolean isSaved() {
    return saved;
  }
}
