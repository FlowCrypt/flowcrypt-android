package com.flowcrypt.email.api.retrofit.response;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.base.BaseApiResponse;
import com.google.gson.annotations.Expose;

/**
 * The simple POJO object, which contains an information about a post feedback result.
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
 *         Date: 30.05.2017
 *         Time: 12:34
 *         E-mail: DenBond7@gmail.com
 */

public class PostHelpFeedbackResponse implements BaseApiResponse {

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

    public PostHelpFeedbackResponse(Parcel in) {
        this.sent = in.readByte() != 0;
        this.text = in.readString();
    }

    @Override
    public String toString() {
        return "PostHelpFeedbackResponse{" +
                "sent=" + sent +
                ", text='" + text + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
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
