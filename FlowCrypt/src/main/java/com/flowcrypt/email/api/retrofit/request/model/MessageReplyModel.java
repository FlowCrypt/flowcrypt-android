/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import android.os.Parcel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The request model for the https://api.cryptup.io/message/reply API.
 *
 * @author Denis Bondarenko
 *         Date: 13.07.2017
 *         Time: 16:32
 *         E-mail: DenBond7@gmail.com
 */

public class MessageReplyModel extends BaseRequestModel {

    public static final Creator<MessageReplyModel> CREATOR = new Creator<MessageReplyModel>() {
        @Override
        public MessageReplyModel createFromParcel(Parcel source) {
            return new MessageReplyModel(source);
        }

        @Override
        public MessageReplyModel[] newArray(int size) {
            return new MessageReplyModel[size];
        }
    };

    @SerializedName("short")
    @Expose
    private String shortValue;

    @SerializedName("token")
    @Expose
    private String token;

    @SerializedName("message")
    @Expose
    private String message;

    @SerializedName("subject")
    @Expose
    private String subject;

    @SerializedName("from")
    @Expose
    private String from;

    @SerializedName("to")
    @Expose
    private String to;

    public MessageReplyModel(String shortValue, String token, String message, String subject,
                             String from, String to) {
        this.shortValue = shortValue;
        this.token = token;
        this.message = message;
        this.subject = subject;
        this.from = from;
        this.to = to;
    }

    protected MessageReplyModel(Parcel in) {
        this.shortValue = in.readString();
        this.token = in.readString();
        this.message = in.readString();
        this.subject = in.readString();
        this.from = in.readString();
        this.to = in.readString();
    }

    @Override
    public String toString() {
        return "MessageReplyModel{" +
                "shortValue='" + shortValue + '\'' +
                ", token='" + token + '\'' +
                ", message='" + message + '\'' +
                ", subject='" + subject + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                "} " + super.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.shortValue);
        dest.writeString(this.token);
        dest.writeString(this.message);
        dest.writeString(this.subject);
        dest.writeString(this.from);
        dest.writeString(this.to);
    }
}
