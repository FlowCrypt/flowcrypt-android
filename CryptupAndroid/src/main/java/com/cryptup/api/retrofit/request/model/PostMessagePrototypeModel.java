package com.cryptup.api.retrofit.request.model;

import android.os.Parcel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * This is a POJO object which used to make a request to the API
 * "https://api.cryptup.io/message/prototype"
 *
 * @author DenBond7
 *         Date: 24.04.2017
 *         Time: 13:51
 *         E-mail: DenBond7@gmail.com
 */

public class PostMessagePrototypeModel extends BaseRequestModel {

    public static final Creator<PostMessagePrototypeModel> CREATOR =
            new Creator<PostMessagePrototypeModel>() {
                @Override
                public PostMessagePrototypeModel createFromParcel(Parcel source) {
                    return new PostMessagePrototypeModel(source);
                }

                @Override
                public PostMessagePrototypeModel[] newArray(int size) {
                    return new PostMessagePrototypeModel[size];
                }
            };

    @SerializedName("message_token_account")
    @Expose
    private String messageTokenAccount;

    @SerializedName("message_token")
    @Expose
    private String messageToken;

    @SerializedName("to")
    @Expose
    private String to;

    @SerializedName("message")
    @Expose
    private String message;

    public PostMessagePrototypeModel() {
    }

    protected PostMessagePrototypeModel(Parcel in) {
        this.messageTokenAccount = in.readString();
        this.messageToken = in.readString();
        this.to = in.readString();
        this.message = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.messageTokenAccount);
        dest.writeString(this.messageToken);
        dest.writeString(this.to);
        dest.writeString(this.message);
    }

    public String getMessageTokenAccount() {
        return messageTokenAccount;
    }

    public void setMessageTokenAccount(String messageTokenAccount) {
        this.messageTokenAccount = messageTokenAccount;
    }

    public String getMessageToken() {
        return messageToken;
    }

    public void setMessageToken(String messageToken) {
        this.messageToken = messageToken;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
