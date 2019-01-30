/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.api;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.base.BaseApiResponse;
import com.google.gson.annotations.Expose;

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

public class PostHelpFeedbackResponse extends BaseApiResponse {

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

  @Expose
  private boolean sent;

  @Expose
  private String text;

  public PostHelpFeedbackResponse() {
  }

  protected PostHelpFeedbackResponse(Parcel in) {
    super(in);
    this.sent = in.readByte() != 0;
    this.text = in.readString();
  }

  @Override
  public String toString() {
    return "PostHelpFeedbackResponse{" +
        "sent=" + sent +
        ", text='" + text + '\'' +
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
    dest.writeString(this.text);
  }

  public boolean isSent() {
    return sent;
  }

  public String getText() {
    return text;
  }
}
