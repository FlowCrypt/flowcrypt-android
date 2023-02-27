/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.base

import com.flowcrypt.email.util.exception.ApiException
import java.io.Serializable

/**
 * It's a wrapper for coroutines calls
 *
 * @author Denys Bondarenko
 */
data class Result<out T>(
  val status: Status,
  val data: T? = null,
  val exception: Throwable? = null,
  val requestCode: Long = 0,
  val resultCode: Int = 0,
  val progressMsg: String? = null,
  val progress: Double? = null
) : Serializable {

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

    fun <T> exception(throwable: Throwable, requestCode: Long = 0): Result<T> {
      return Result(
        status = Status.EXCEPTION,
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

    fun <T> loading(
      requestCode: Long = 0, resultCode: Int = 0, progressMsg: String? = null,
      progress: Double? = null
    ): Result<T> {
      return Result(
        status = Status.LOADING,
        requestCode = requestCode,
        resultCode = resultCode,
        progressMsg = progressMsg,
        progress = progress
      )
    }

    fun <T : ApiResponse> throwExceptionIfNotSuccess(result: Result<T>) {
      when (result.status) {
        Status.EXCEPTION -> result.exception?.let { throw it }
        Status.ERROR -> result.data?.apiError?.let { throw ApiException(it) }
        else -> {
          //do nothing
        }
      }
    }
  }
}
