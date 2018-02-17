/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import android.os.Parcel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The request model for the https://attester.flowcrypt.com/replace/request API.
 *
 * @author Denis Bondarenko
 *         Date: 13.07.2017
 *         Time: 10:03
 *         E-mail: DenBond7@gmail.com
 */

public class ReplaceRequestModel extends BaseRequestModel {
    public static final Creator<ReplaceRequestModel> CREATOR = new Creator<ReplaceRequestModel>() {
        @Override
        public ReplaceRequestModel createFromParcel(Parcel source) {
            return new ReplaceRequestModel(source);
        }

        @Override
        public ReplaceRequestModel[] newArray(int size) {
            return new ReplaceRequestModel[size];
        }
    };

    @SerializedName("signed_message")
    @Expose
    private String signedMessage;

    @SerializedName("new_pubkey")
    @Expose
    private String newPubkey;

    @SerializedName("email")
    @Expose
    private String email;

    public ReplaceRequestModel(String signedMessage, String newPubkey, String email) {
        this.signedMessage = signedMessage;
        this.newPubkey = newPubkey;
        this.email = email;
    }

    protected ReplaceRequestModel(Parcel in) {
        this.signedMessage = in.readString();
        this.newPubkey = in.readString();
        this.email = in.readString();
    }

    @Override
    public String toString() {
        return "ReplaceRequestModel{" +
                "signedMessage='" + signedMessage + '\'' +
                ", newPubkey='" + newPubkey + '\'' +
                ", email='" + email + '\'' +
                "} " + super.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.signedMessage);
        dest.writeString(this.newPubkey);
        dest.writeString(this.email);
    }
}
