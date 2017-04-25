package com.flowcrypt.email.api.retrofit.request.model;

import android.os.Parcel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * This is a POJO object which used to make a request to the API "https://attester.cryptup
 * .io/lookup/email"
 *
 * @author DenBond7
 *         Date: 24.04.2017
 *         Time: 13:27
 *         E-mail: DenBond7@gmail.com
 */

public class PostLookUpEmailModel extends BaseRequestModel {
    public static final Creator<PostLookUpEmailModel> CREATOR
            = new Creator<PostLookUpEmailModel>() {
        @Override
        public PostLookUpEmailModel createFromParcel(Parcel source) {
            return new PostLookUpEmailModel(source);
        }

        @Override
        public PostLookUpEmailModel[] newArray(int size) {
            return new PostLookUpEmailModel[size];
        }
    };

    @SerializedName("email")
    @Expose
    private String email;

    public PostLookUpEmailModel() {
    }

    public PostLookUpEmailModel(Parcel in) {
        this.email = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.email);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
