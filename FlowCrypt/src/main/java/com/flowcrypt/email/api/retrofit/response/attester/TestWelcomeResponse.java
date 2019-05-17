/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.attester;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.base.ApiError;
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

/**
 * This class describes a response from the https://flowcrypt.com/attester/test/welcome API.
 * <p>
 * <code>POST /test/welcome
 * response(200): {
 * "sent" (True, False)  # successfuly sent email
 * [voluntary] "error" (<type 'str'>)  # error detail, if not saved
 * }</code>
 *
 * @author Denis Bondarenko
 * Date: 12.07.2017
 * Time: 14:38
 * E-mail: DenBond7@gmail.com
 */

public class TestWelcomeResponse implements ApiResponse {
  public static final Creator<TestWelcomeResponse> CREATOR = new Creator<TestWelcomeResponse>() {
    @Override
    public TestWelcomeResponse createFromParcel(Parcel source) {
      return new TestWelcomeResponse(source);
    }

    @Override
    public TestWelcomeResponse[] newArray(int size) {
      return new TestWelcomeResponse[size];
    }
  };

  @SerializedName("error")
  @Expose
  private ApiError apiError;

  @Expose
  private boolean sent;

  public TestWelcomeResponse() {
  }

  public TestWelcomeResponse(Parcel in) {
    this.apiError = in.readParcelable(ApiError.class.getClassLoader());
    this.sent = in.readByte() != 0;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(this.apiError, flags);
    dest.writeByte(this.sent ? (byte) 1 : (byte) 0);
  }

  @NotNull
  @Override
  public ApiError getApiError() {
    return apiError;
  }

  public boolean isSent() {
    return sent;
  }
}
