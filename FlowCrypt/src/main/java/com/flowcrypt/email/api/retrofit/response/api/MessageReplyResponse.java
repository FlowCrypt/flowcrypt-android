/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.api;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.base.BaseApiResponse;
import com.google.gson.annotations.Expose;

/**
 * This class describes a response from the https://flowcrypt.com/api/message/reply API.
 * <p>
 * <code>POST /message/reply
 * response(200): {
 * "sent" (True, False)  # successfully sent message
 * [voluntary] "error" (<type 'str'>)  # Encountered error if any
 * }</code>
 *
 * @author Denis Bondarenko
 * Date: 13.07.2017
 * Time: 16:33
 * E-mail: DenBond7@gmail.com
 */

public class MessageReplyResponse extends BaseApiResponse {
  public static final Creator<MessageReplyResponse> CREATOR = new Creator<MessageReplyResponse>() {
    @Override
    public MessageReplyResponse createFromParcel(Parcel source) {
      return new MessageReplyResponse(source);
    }

    @Override
    public MessageReplyResponse[] newArray(int size) {
      return new MessageReplyResponse[size];
    }
  };

  @Expose
  private boolean sent;

  public MessageReplyResponse() {
  }

  protected MessageReplyResponse(Parcel in) {
    super(in);
    this.sent = in.readByte() != 0;
  }

  @Override
  public String toString() {
    return "MessageReplyResponse{" +
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
