/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.api;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.base.ApiError;
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.NotNull;

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

public class MessageReplyResponse implements ApiResponse {
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

  @SerializedName("error")
  @Expose
  private ApiError apiError;

  @Expose
  private boolean sent;

  public MessageReplyResponse() {
  }

  public MessageReplyResponse(Parcel in) {
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

  public boolean isSent() {
    return sent;
  }

  @NotNull
  @Override
  public ApiError getApiError() {
    return apiError;
  }
}
