package com.cryptup.api.retrofit;

import com.cryptup.api.retrofit.response.base.BaseApiResponse;

import java.io.Serializable;

import retrofit2.Response;

/**
 * A base response model class.
 *
 * @author Denis Bondarenko
 *         Date: 10.03.2015
 *         Time: 13:34
 *         E-mail: DenBond7@gmail.com
 */
public class BaseResponse<T extends BaseApiResponse> implements Serializable {
    private Response<T> baseResponseModelResponse;
    private Exception e;
    private ApiName apiName;

    public T getResponseModel() {
        return baseResponseModelResponse != null ? baseResponseModelResponse.body() : null;
    }

    public void setResponse(Response<T> response) {
        this.baseResponseModelResponse = response;
    }

    public Exception getException() {
        return e;
    }

    public void setException(Exception e) {
        this.e = e;
    }

    public ApiName getApiName() {
        return apiName;
    }

    public void setApiName(ApiName apiName) {
        this.apiName = apiName;
    }

    public int getResponseCode() {
        return baseResponseModelResponse != null ? baseResponseModelResponse.code() : -1;
    }
}
