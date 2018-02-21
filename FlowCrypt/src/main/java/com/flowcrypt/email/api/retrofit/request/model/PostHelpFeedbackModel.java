/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.request.api.PostHelpFeedbackRequest;
import com.google.gson.annotations.Expose;

/**
 * The model of {@link PostHelpFeedbackRequest}.
 *
 * @author DenBond7
 *         Date: 30.05.2017
 *         Time: 12:42
 *         E-mail: DenBond7@gmail.com
 */

public class PostHelpFeedbackModel extends BaseRequestModel {
    public static final Creator<PostHelpFeedbackModel> CREATOR = new
            Creator<PostHelpFeedbackModel>() {
                @Override
                public PostHelpFeedbackModel createFromParcel(Parcel source) {
                    return new PostHelpFeedbackModel(source);
                }

                @Override
                public PostHelpFeedbackModel[] newArray(int size) {
                    return new PostHelpFeedbackModel[size];
                }
            };

    @Expose
    private String email;

    @Expose
    private String message;

    public PostHelpFeedbackModel() {
    }

    public PostHelpFeedbackModel(String email, String message) {
        this.email = email;
        this.message = message;
    }

    public PostHelpFeedbackModel(Parcel in) {
        this.email = in.readString();
        this.message = in.readString();
    }

    @Override
    public String toString() {
        return "PostHelpFeedbackModel{" +
                "email='" + email + '\'' +
                ", message='" + message + '\'' +
                "} " + super.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.email);
        dest.writeString(this.message);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
