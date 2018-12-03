/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.okhttp;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This {@link Interceptor} add a custom header (<code>api-version: version-value</code>) to all
 * requests.
 *
 * @author Denis Bondarenko
 * Date: 12.07.2017
 * Time: 13:40
 * E-mail: DenBond7@gmail.com
 */

public class ApiVersionInterceptor implements Interceptor {

  private static final String HEADER_NAME_API_VERSION = "api-version";
  private static final String API_VERSION_VALUE = "3";

  @Override
  public Response intercept(Chain chain) throws IOException {
    Request request = chain.request().newBuilder().addHeader(HEADER_NAME_API_VERSION, API_VERSION_VALUE).build();
    return chain.proceed(request);
  }
}
