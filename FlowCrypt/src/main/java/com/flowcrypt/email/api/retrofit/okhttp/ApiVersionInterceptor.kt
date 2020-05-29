/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.okhttp

import okhttp3.Interceptor
import okhttp3.Response

/**
 * This [Interceptor] add a custom header (`api-version: version-value`) to all
 * requests.
 *
 * @author Denis Bondarenko
 * Date: 12.07.2017
 * Time: 13:40
 * E-mail: DenBond7@gmail.com
 */
class ApiVersionInterceptor : Interceptor {

  override fun intercept(chain: Interceptor.Chain): Response {
    return chain.proceed(chain.request().newBuilder().addHeader(HEADER_NAME_API_VERSION, API_VERSION_VALUE).build())
  }

  companion object {
    private const val HEADER_NAME_API_VERSION = "api-version"
    private const val API_VERSION_VALUE = "3"
  }
}
