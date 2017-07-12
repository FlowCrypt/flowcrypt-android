/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.base;

import android.os.Parcel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * The base API response implementation.
 *
 * @author Denis Bondarenko
 *         Date: 05.02.2016
 *         Time: 10:16
 *         E-mail: DenBond7@gmail.com
 */
public class BaseApiResponse implements ApiResponse {

    public static final Creator<BaseApiResponse> CREATOR = new Creator<BaseApiResponse>() {
        @Override
        public BaseApiResponse createFromParcel(Parcel source) {
            return new BaseApiResponse(source);
        }

        @Override
        public BaseApiResponse[] newArray(int size) {
            return new BaseApiResponse[size];
        }
    };

    @SerializedName("error")
    @Expose
    private ApiError apiError;

    public BaseApiResponse() {
    }

    protected BaseApiResponse(Parcel in) {
        this.apiError = in.readParcelable(ApiError.class.getClassLoader());
    }

    @Override
    public String toString() {
        return "BaseApiResponse{" +
                "apiError=" + apiError +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.apiError, flags);
    }

    public ApiError getApiError() {
        return apiError;
    }
}
