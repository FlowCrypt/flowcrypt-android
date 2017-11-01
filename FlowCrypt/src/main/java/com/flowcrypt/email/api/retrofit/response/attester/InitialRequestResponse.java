/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.attester;

import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.response.base.BaseApiResponse;
import com.google.gson.annotations.Expose;

/**
 * This class describes a response from the https://attester.flowcrypt.com/initial/request API.
 * <p>
 * <code>POST /test/welcome
 * response(200): {
 * "saved" (True, False)  # successfully saved pubkey
 * "attested" (True, False)  # previously went through full attestation using keys/attest
 * [voluntary] "error" (<type 'str'>)  # error detail, if not saved
 * }</code>
 *
 * @author Denis Bondarenko
 *         Date: 12.07.2017
 *         Time: 16:53
 *         E-mail: DenBond7@gmail.com
 */

public class InitialRequestResponse extends BaseApiResponse {
    public static final Creator<InitialRequestResponse> CREATOR = new
            Creator<InitialRequestResponse>() {
                @Override
                public InitialRequestResponse createFromParcel(Parcel source) {
                    return new InitialRequestResponse(source);
                }

                @Override
                public InitialRequestResponse[] newArray(int size) {
                    return new InitialRequestResponse[size];
                }
            };
    @Expose
    private boolean saved;

    @Expose
    private boolean attested;

    public InitialRequestResponse() {
    }

    protected InitialRequestResponse(Parcel in) {
        super(in);
        this.saved = in.readByte() != 0;
        this.attested = in.readByte() != 0;
    }

    @Override
    public String toString() {
        return "InitialRequestResponse{" +
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

    public boolean isSaved() {
        return saved;
    }

    public boolean isAttested() {
        return attested;
    }
}
