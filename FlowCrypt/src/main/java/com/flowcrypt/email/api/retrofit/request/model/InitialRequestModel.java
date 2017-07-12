/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.model;

import android.os.Parcel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The request model for the https://attester.cryptup.io/initial/request API.
 *
 * @author Denis Bondarenko
 *         Date: 12.07.2017
 *         Time: 16:48
 *         E-mail: DenBond7@gmail.com
 */

public class InitialRequestModel extends BaseRequestModel {
    public static final Creator<InitialRequestModel> CREATOR = new Creator<InitialRequestModel>() {
        @Override
        public InitialRequestModel createFromParcel(Parcel source) {
            return new InitialRequestModel(source);
        }

        @Override
        public InitialRequestModel[] newArray(int size) {
            return new InitialRequestModel[size];
        }
    };

    @SerializedName("email")
    @Expose
    private String email;

    @SerializedName("pubkey")
    @Expose
    private String pubkey;

    @SerializedName("attest")
    @Expose
    private boolean attest;

    public InitialRequestModel(String email, String pubkey) {
        this.email = email;
        this.pubkey = pubkey;
    }

    protected InitialRequestModel(Parcel in) {
        this.email = in.readString();
        this.pubkey = in.readString();
        this.attest = in.readByte() != 0;
    }

    @Override
    public String toString() {
        return "InitialRequestModel{" +
                "email='" + email + '\'' +
                ", pubkey='" + pubkey + '\'' +
                ", attest=" + attest +
                "} " + super.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.email);
        dest.writeString(this.pubkey);
        dest.writeByte(this.attest ? (byte) 1 : (byte) 0);
    }

    public void setAttest(boolean attest) {
        this.attest = attest;
    }
}
