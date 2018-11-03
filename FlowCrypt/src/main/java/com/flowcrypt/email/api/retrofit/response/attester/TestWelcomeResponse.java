/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.attester;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.base.BaseApiResponse;
import com.google.gson.annotations.Expose;

/**
 * This class describes a response from the https://attester.flowcrypt.com/test/welcome API.
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

public class TestWelcomeResponse extends BaseApiResponse {
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

  @Expose
  private boolean sent;

  public TestWelcomeResponse() {
  }

  protected TestWelcomeResponse(Parcel in) {
    super(in);
    this.sent = in.readByte() != 0;
  }

  @Override
  public String toString() {
    return "TestWelcomeResponse{" +
        "sent=" + sent +
        "} " + super.toString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    super.writeToParcel(dest, flags);
    dest.writeByte(this.sent ? (byte) 1 : (byte) 0);
  }

  public boolean isSent() {
    return sent;
  }
}
