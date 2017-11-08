/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.attester;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.base.BaseApiResponse;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Response from the API
 * "https://attester.flowcrypt.com/lookup/email"
 *
 * @author DenBond7
 *         Date: 24.04.2017
 *         Time: 13:20
 *         E-mail: DenBond7@gmail.com
 */

public class LookUpEmailResponse extends BaseApiResponse {

    public static final Creator<LookUpEmailResponse> CREATOR = new Creator<LookUpEmailResponse>() {
        @Override
        public LookUpEmailResponse createFromParcel(Parcel source) {
            return new LookUpEmailResponse(source);
        }

        @Override
        public LookUpEmailResponse[] newArray(int size) {
            return new LookUpEmailResponse[size];
        }
    };

    @Expose
    private boolean attested;

    @SerializedName("has_cryptup")
    @Expose
    private boolean hasCryptup;

    @Expose
    private String pubkey;

    @Expose
    private String email;

    public LookUpEmailResponse() {
    }

    protected LookUpEmailResponse(Parcel in) {
        super(in);
        this.attested = in.readByte() != 0;
        this.hasCryptup = in.readByte() != 0;
        this.pubkey = in.readString();
        this.email = in.readString();
    }

    @Override
    public String toString() {
        return "LookUpEmailResponse{" +
                "attested=" + attested +
                ", hasCryptup=" + hasCryptup +
                ", pubkey='" + pubkey + '\'' +
                ", email='" + email + '\'' +
                "} " + super.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeByte(this.attested ? (byte) 1 : (byte) 0);
        dest.writeByte(this.hasCryptup ? (byte) 1 : (byte) 0);
        dest.writeString(this.pubkey);
        dest.writeString(this.email);
    }

    public boolean isAttested() {
        return attested;
    }

    public boolean isHasCryptup() {
        return hasCryptup;
    }

    public String getPubkey() {
        return pubkey;
    }

    public String getEmail() {
        return email;
    }
}
