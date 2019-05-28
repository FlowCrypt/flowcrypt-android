/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit;

import com.flowcrypt.email.api.retrofit.response.base.ApiResponse;

import java.io.Serializable;

import retrofit2.Response;

/**
 * A base response model class.
 *
 * @author Denis Bondarenko
 * Date: 10.03.2015
 * Time: 13:34
 * E-mail: DenBond7@gmail.com
 */
public class BaseResponse<T extends ApiResponse> implements Serializable {
  private Response<T> modelResponse;
  private Exception e;
  private ApiName apiName;

  public T getResponseModel() {
    return modelResponse != null ? modelResponse.body() : null;
  }

  public void setResponse(Response<T> response) {
    this.modelResponse = response;
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
    return modelResponse != null ? modelResponse.code() : -1;
  }
}
