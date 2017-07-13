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
 * This class describes a response from the https://attester.cryptup.io/replace/request API.
 * <p>
 * <code>POST /replace/request
 * response(200): {
 * "saved" (True, False)  # successfuly saved request, sent email to confirm
 * [voluntary] "error" (<type 'str'>)  # error detail if not saved
 * }</code>
 *
 * @author Denis Bondarenko
 *         Date: 13.07.2017
 *         Time: 10:31
 *         E-mail: DenBond7@gmail.com
 */

public class ReplacerRequestResponse extends BaseApiResponse {

    public static final Creator<ReplacerRequestResponse> CREATOR = new
            Creator<ReplacerRequestResponse>() {
                @Override
                public ReplacerRequestResponse createFromParcel(Parcel source) {
                    return new ReplacerRequestResponse(source);
                }

                @Override
                public ReplacerRequestResponse[] newArray(int size) {
                    return new ReplacerRequestResponse[size];
                }
            };

    @Expose
    private boolean saved;

    public ReplacerRequestResponse() {
    }

    protected ReplacerRequestResponse(Parcel in) {
        super(in);
        this.saved = in.readByte() != 0;
    }

    @Override
    public String toString() {
        return "ReplacerRequestResponse{" +
                "saved=" + saved +
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
    }

    public boolean isSaved() {
        return saved;
    }
}
