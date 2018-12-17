/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request;

import com.flowcrypt.email.api.retrofit.ApiName;

import java.util.LinkedHashMap;

/**
 * The base class for all requests.
 *
 * @author Denis Bondarenko
 * Date: 10.03.2015
 * Time: 13:41
 * E-mail: DenBond7@gmail.com
 */
public abstract class BaseRequest<T> {
  private LinkedHashMap<String, String> queryMap;
  private ApiName apiName;
  private T requestModel;

  public BaseRequest(ApiName apiName, T requestModel) {
    this(apiName);
    this.requestModel = requestModel;
  }

  public BaseRequest(ApiName apiName) {
    this.apiName = apiName;
    queryMap = new LinkedHashMap<>();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BaseRequest<?> that = (BaseRequest<?>) o;

    if (queryMap != null ? !queryMap.equals(that.queryMap) : that.queryMap != null) {
      return false;
    }
    if (apiName != that.apiName) {
      return false;
    }
    return requestModel != null ? requestModel.equals(that.requestModel) : that.requestModel == null;

  }

  @Override
  public int hashCode() {
    int result = queryMap != null ? queryMap.hashCode() : 0;
    result = 31 * result + (apiName != null ? apiName.hashCode() : 0);
    result = 31 * result + (requestModel != null ? requestModel.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "BaseRequest{" +
        "queryMap=" + queryMap +
        ", apiName=" + apiName +
        ", requestModel=" + requestModel +
        '}';
  }

  public LinkedHashMap<String, String> getQueryMap() {
    return queryMap;
  }

  public void setQueryMap(LinkedHashMap<String, String> queryMap) {
    this.queryMap = queryMap;
  }

  public ApiName getApiName() {
    return apiName;
  }

  /**
   * Get the request model which contains information to create the request.
   *
   * @return <tt>T</tt>
   */
  public T getRequestModel() {
    return requestModel;
  }
}
