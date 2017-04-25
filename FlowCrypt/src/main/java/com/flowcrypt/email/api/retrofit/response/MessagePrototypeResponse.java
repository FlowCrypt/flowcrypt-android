package com.flowcrypt.email.api.retrofit.response;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.base.BaseApiResponse;
import com.flowcrypt.email.api.retrofit.response.model.MessagePrototypeError;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Response from the API
 * "https://api.cryptup.io/message/prototype"
 *
 * @author DenBond7
 *         Date: 24.04.2017
 *         Time: 13:57
 *         E-mail: DenBond7@gmail.com
 */

public class MessagePrototypeResponse implements BaseApiResponse {

    public static final Creator<MessagePrototypeResponse> CREATOR = new
            Creator<MessagePrototypeResponse>() {
                @Override
                public MessagePrototypeResponse createFromParcel(Parcel source) {
                    return new MessagePrototypeResponse(source);
                }

                @Override
                public MessagePrototypeResponse[] newArray(int size) {
                    return new MessagePrototypeResponse[size];
                }
            };

    public static final String GSON_KEY_SENT = "sent";
    public static final String GSON_KEY_ERROR = "error";

    @SerializedName(GSON_KEY_SENT)
    @Expose
    private boolean sent;

    @SerializedName(GSON_KEY_ERROR)
    @Expose
    private String error;

    @SerializedName(GSON_KEY_ERROR)
    @Expose
    private MessagePrototypeError messagePrototypeError;

    public MessagePrototypeResponse() {
    }

    protected MessagePrototypeResponse(Parcel in) {
        this.sent = in.readByte() != 0;
        this.error = in.readString();
        this.messagePrototypeError = in.readParcelable(MessagePrototypeError.class.getClassLoader
                ());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte(this.sent ? (byte) 1 : (byte) 0);
        dest.writeString(this.error);
        dest.writeParcelable(this.messagePrototypeError, flags);
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public MessagePrototypeError getMessagePrototypeError() {
        return messagePrototypeError;
    }

    public void setMessagePrototypeError(MessagePrototypeError messagePrototypeError) {
        this.messagePrototypeError = messagePrototypeError;
    }
}
