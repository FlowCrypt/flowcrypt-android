/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/tree/master/src/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request;

import com.flowcrypt.email.api.retrofit.ApiName;

import java.util.LinkedHashMap;

/**
 * The base class for all requests.
 *
 * @author Denis Bondarenko
 *         Date: 10.03.2015
 *         Time: 13:41
 *         E-mail: DenBond7@gmail.com
 */
public abstract class BaseRequest {
    private LinkedHashMap<String, String> queryMap;
    private ApiName apiName;

    BaseRequest(ApiName apiName) {
        this.apiName = apiName;
        queryMap = new LinkedHashMap<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseRequest)) return false;

        BaseRequest that = (BaseRequest) o;

        if (queryMap != null ? !queryMap.equals(that.queryMap) : that.queryMap != null)
            return false;
        return apiName == that.apiName;

    }

    @Override
    public int hashCode() {
        int result = queryMap != null ? queryMap.hashCode() : 0;
        result = 31 * result + (apiName != null ? apiName.hashCode() : 0);
        return result;
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
}
