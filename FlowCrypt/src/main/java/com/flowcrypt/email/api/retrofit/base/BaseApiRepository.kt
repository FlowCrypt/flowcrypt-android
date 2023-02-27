/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.base

import android.content.Context
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.util.exception.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Converter
import retrofit2.Response

/**
 * @author Denys Bondarenko
 */
interface BaseApiRepository {
  /**
   * Base implementation for the API calls
   */
  suspend fun <T> getResult(
    requestCode: Long = 0L,
    context: Context? = null,
    expectedResultClass: Class<T>? = null,
    call: suspend () -> Response<T>
  ): Result<T> {
    return try {
      val response = call()
      if (response.isSuccessful) {
        val body = response.body()
        if (body != null) {
          if (body is ApiResponse) {
            return if (body.apiError != null) {
              Result.error(data = body, requestCode = requestCode)
            } else {
              Result.success(data = body, requestCode = requestCode)
            }
          } else {
            Result.success(data = body, requestCode = requestCode)
          }
        } else {
          Result.exception(
            throwable = ApiException(ApiError(response.code(), response.message())),
            requestCode = requestCode
          )
        }
      } else {
        val errorBody = response.errorBody()
        if (errorBody == null) {
          return Result.exception(
            throwable = ApiException(ApiError(response.code(), "errorBody == null")),
            requestCode = requestCode
          )
        } else {
          val buffer = errorBody.bytes()
          val apiResponseWithError = parseError(
            context,
            expectedResultClass,
            buffer.toResponseBody(errorBody.contentType())
          )

          if (apiResponseWithError != null
            && apiResponseWithError is ApiResponse
            && apiResponseWithError.apiError != null
          ) {
            return Result.error(data = apiResponseWithError, requestCode = requestCode)
          } else {
            Result.exception(
              throwable = ApiException(ApiError(response.code(), String(buffer))),
              requestCode = requestCode
            )
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
      Result.exception(throwable = e, requestCode = requestCode)
    }
  }

  private suspend fun <T> parseError(
    context: Context?,
    exceptedClass: Class<T>?,
    responseBody: ResponseBody
  ): T? =
    withContext(Dispatchers.IO) {
      context ?: return@withContext null
      exceptedClass ?: return@withContext null
      try {
        val errorConverter: Converter<ResponseBody, T> =
          ApiHelper.getInstance(context).retrofit.responseBodyConverter(
            exceptedClass,
            arrayOfNulls(0)
          )
        return@withContext errorConverter.convert(responseBody)
      } catch (e: Exception) {
        e.printStackTrace()
      }
      return@withContext null
    }
}
