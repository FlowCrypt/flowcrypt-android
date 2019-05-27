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
 * The simple POJO object, which contains information about a post feedback result.
 * <p>
 * This class describes the next response:
 * <p>
 * <pre>
 * <code>POST
 * response(200): {
 * "sent" (True, False)  # True if message was sent successfully
 * "text" (<type 'str'>)  # User friendly success or error text
 * }
 * </code>
 * </pre>
 *
 * @author DenBond7
 * Date: 30.05.2017
 * Time: 12:34
 * E-mail: DenBond7@gmail.com
 */

public class PostHelpFeedbackResponse implements ApiResponse {

  public static final Creator<PostHelpFeedbackResponse> CREATOR = new
      Creator<PostHelpFeedbackResponse>() {
        @Override
        public PostHelpFeedbackResponse createFromParcel(Parcel source) {
          return new PostHelpFeedbackResponse(source);
        }

        @Override
        public PostHelpFeedbackResponse[] newArray(int size) {
          return new PostHelpFeedbackResponse[size];
        }
      };

  @SerializedName("error")
  @Expose
  private ApiError apiError;

  @Expose
  private boolean sent;

  @Expose
  private String text;

  public PostHelpFeedbackResponse(Parcel in) {
    this.apiError = in.readParcelable(ApiError.class.getClassLoader());
    this.sent = in.readByte() != 0;
    this.text = in.readString();
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeParcelable(this.apiError, flags);
    dest.writeByte(this.sent ? (byte) 1 : (byte) 0);
    dest.writeString(this.text);
  }

  @NotNull
  @Override
  public ApiError getApiError() {
    return apiError;
  }

  public boolean isSent() {
    return sent;
  }

  public String getText() {
    return text;
  }
}
