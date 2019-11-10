/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.base

/**
 * It's a wrapper for the API calls
 *
 * @author Denis Bondarenko
 *         Date: 10/28/19
 *         Time: 3:42 PM
 *         E-mail: DenBond7@gmail.com
 */
class ApiResult<out T>(val status: Status, val data: T?, val exception: Throwable?) {

  enum class Status {
    SUCCESS,
    ERROR,
    EXCEPTION,
    LOADING
  }

  companion object {
    fun <T> success(data: T): ApiResult<T> {
      return ApiResult(Status.SUCCESS, data, null)
    }

    fun <T> exception(error: Throwable, data: T? = null): ApiResult<T> {
      return ApiResult(Status.EXCEPTION, data, error)
    }

    fun <T> error(data: T? = null): ApiResult<T> {
      return ApiResult(Status.ERROR, data, null)
    }

    fun <T> loading(data: T? = null): ApiResult<T> {
      return ApiResult(Status.LOADING, data, null)
    }
  }
}