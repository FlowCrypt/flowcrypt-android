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
 * This class describes a response from the https://attester.cryptup.io/replace/confirm API.
 * <p>
 * <code>POST /replace/confirm
 * response(200): {
 * "attested" (True, False)  # successfuly attested replacement
 * [voluntary] "error" (<type 'str'>)  # error detail if not attested
 * }</code>
 *
 * @author Denis Bondarenko
 *         Date: 13.07.2017
 *         Time: 11:51
 *         E-mail: DenBond7@gmail.com
 */

public class ReplaceConfirmResponse extends BaseApiResponse {
    public static final Creator<ReplaceConfirmResponse> CREATOR = new
            Creator<ReplaceConfirmResponse>() {
                @Override
                public ReplaceConfirmResponse createFromParcel(Parcel source) {
                    return new ReplaceConfirmResponse(source);
                }

                @Override
                public ReplaceConfirmResponse[] newArray(int size) {
                    return new ReplaceConfirmResponse[size];
                }
            };

    @Expose
    private boolean attested;

    public ReplaceConfirmResponse() {
    }


    protected ReplaceConfirmResponse(Parcel in) {
        super(in);
        this.attested = in.readByte() != 0;
    }

    @Override
    public String toString() {
        return "ReplaceConfirmResponse{" +
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
