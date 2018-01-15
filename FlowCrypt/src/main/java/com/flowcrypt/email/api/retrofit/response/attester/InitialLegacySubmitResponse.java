/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.attester;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.base.BaseApiResponse;
import com.google.gson.annotations.Expose;

/**
 * This class describes a response from the https://attester.flowcrypt.com/initial/legacy_submit API.
 * <p>
 * <code>POST /initial/legacy_submit
 * response(200): {
 * "saved" (True, False)  # successfuly saved pubkey
 * "attested" (True, False)  # previously went through full attestation using keys/attest
 * [voluntary] "error" (<type 'str'>)  # error detail, if not saved
 * }</code>
 *
 * @author Denis Bondarenko
 *         Date: 15.01.2018
 *         Time: 16:30
 *         E-mail: DenBond7@gmail.com
 */

public class InitialLegacySubmitResponse extends BaseApiResponse {

    public static final Creator<InitialLegacySubmitResponse> CREATOR = new Creator<InitialLegacySubmitResponse>() {
        @Override
        public InitialLegacySubmitResponse createFromParcel(Parcel source) {
            return new InitialLegacySubmitResponse(source);
        }

        @Override
        public InitialLegacySubmitResponse[] newArray(int size) {
            return new InitialLegacySubmitResponse[size];
        }
    };

    @Expose
    private boolean saved;

    @Expose
    private boolean attested;


    public InitialLegacySubmitResponse() {
    }

    protected InitialLegacySubmitResponse(Parcel in) {
        super(in);
        this.saved = in.readByte() != 0;
        this.attested = in.readByte() != 0;
    }

    @Override
    public String toString() {
        return "InitialLegacySubmitResponse{" +
                "saved=" + saved +
                ", attested=" + attested +
                "} " + super.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeByte(this.saved ? (byte) 1 : (byte) 0);
        dest.writeByte(this.attested ? (byte) 1 : (byte) 0);
    }

    public boolean isAttested() {
        return attested;
    }

    public boolean isSaved() {
        return saved;
    }
}
