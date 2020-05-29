/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.base

/**
 * It's a wrapper for coroutines calls
 *
 * @author Denis Bondarenko
 *         Date: 10/28/19
 *         Time: 3:42 PM
 *         E-mail: DenBond7@gmail.com
 */
class Result<out T>(val status: Status, val data: T?, val exception: Throwable?, val requestCode: Long = 0) {

  enum class Status {
    SUCCESS,
    ERROR,
    EXCEPTION,
    LOADING
  }

  companion object {
    fun <T> success(data: T, requestCode: Long = 0): Result<T> {
      return Result(status = Status.SUCCESS, data = data, exception = null, requestCode = requestCode)
    }

    fun <T> exception(error: Throwable, data: T? = null, requestCode: Long = 0): Result<T> {
      return Result(status = Status.EXCEPTION, data = data, exception = error, requestCode = requestCode)
    }

    fun <T> error(data: T? = null, requestCode: Long = 0): Result<T> {
      return Result(status = Status.ERROR, data = data, exception = null, requestCode = requestCode)
    }

    fun <T> loading(data: T? = null, requestCode: Long = 0): Result<T> {
      return Result(status = Status.LOADING, data = data, exception = null, requestCode = requestCode)
    }
  }
}