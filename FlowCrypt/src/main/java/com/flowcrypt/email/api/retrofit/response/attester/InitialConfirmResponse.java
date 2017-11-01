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
 * This class describes a response from the https://attester.flowcrypt.com/initial/confirm API.
 * <p>
 * <code>POST /initial/confirm
 * response(200): {
 * "attested" (True, False)  # successfuly attested initial entry
 * [voluntary] "error" (<type 'str'>)  # error detail, if not saved
 * }</code>
 *
 * @author Denis Bondarenko
 *         Date: 12.07.2017
 *         Time: 17:13
 *         E-mail: DenBond7@gmail.com
 */

public class InitialConfirmResponse extends BaseApiResponse {
    public static final Creator<InitialConfirmResponse> CREATOR = new
            Creator<InitialConfirmResponse>() {
                @Override
                public InitialConfirmResponse createFromParcel(Parcel source) {
                    return new InitialConfirmResponse(source);
                }

                @Override
                public InitialConfirmResponse[] newArray(int size) {
                    return new InitialConfirmResponse[size];
                }
            };

    @Expose
    private boolean attested;

    public InitialConfirmResponse() {
    }


    protected InitialConfirmResponse(Parcel in) {
        super(in);
        this.attested = in.readByte() != 0;
    }

    @Override
    public String toString() {
        return "InitialConfirmResponse{" +
                "attested=" + attested +
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
    }

    public boolean isAttested() {
        return attested;
    }
}
