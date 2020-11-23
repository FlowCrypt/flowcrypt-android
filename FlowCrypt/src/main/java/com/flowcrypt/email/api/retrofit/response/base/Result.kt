/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.base

import java.io.Serializable

/**
 * It's a wrapper for coroutines calls
 *
 * @author Denis Bondarenko
 *         Date: 10/28/19
 *         Time: 3:42 PM
 *         E-mail: DenBond7@gmail.com
 */
data class Result<out T>(val status: Status,
                         val data: T? = null,
                         val exception: Throwable? = null,
                         val requestCode: Long = 0,
                         val resultCode: Int = 0,
                         val progressMsg: String? = null,
                         val progress: Double? = null) : Serializable {

  enum class Status {
    SUCCESS,
    ERROR,
    EXCEPTION,
    LOADING,
    NONE
  }

  companion object {
    fun <T> none(): Result<T> {
      return Result(
          status = Status.NONE,
          requestCode = 0
      )
    }

    fun <T> success(data: T, requestCode: Long = 0): Result<T> {
      return Result(
          status = Status.SUCCESS,
          data = data,
          requestCode = requestCode
      )
    }

    fun <T> exception(throwable: Throwable, data: T? = null, requestCode: Long = 0): Result<T> {
      return Result(
          status = Status.EXCEPTION,
          data = data,
          exception = throwable,
          requestCode = requestCode
      )
    }

    fun <T> error(data: T? = null, requestCode: Long = 0): Result<T> {
      return Result(
          status = Status.ERROR,
          data = data,
          requestCode = requestCode
      )
    }

    fun <T> loading(requestCode: Long = 0, resultCode: Int = 0, progressMsg: String? = null,
                    progress: Double? = null): Result<T> {
      return Result(
          status = Status.LOADING,
          requestCode = requestCode,
          resultCode = resultCode,
          progressMsg = progressMsg,
          progress = progress
      )
    }
  }
}