/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.base

import android.content.Context
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.response.base.ApiError
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
    call: suspend () -> Response<T>
  ): Result<T> {
    return try {
      val response = call()
      if (response.isSuccessful) {
        val body = response.body()
        if (body != null) {
          Result.success(data = body, requestCode = requestCode)
        } else {
          Result.exception(
            throwable = ApiException(
              ApiError(code = response.code(), message = response.message())
            ),
            requestCode = requestCode
          )
        }
      } else {
        val errorBody = response.errorBody()
        if (errorBody == null) {
          return Result.exception(
            throwable = ApiException(
              ApiError(code = response.code(), message = "errorBody == null")
            ),
            requestCode = requestCode
          )
        } else {
          val buffer = errorBody.bytes()
          val apiError = parseApiError(
            context,
            buffer.toResponseBody(errorBody.contentType())
          )

          if (apiError != null) {
            return Result.error(apiError = apiError, requestCode = requestCode)
          } else {
            Result.exception(
              throwable = ApiException(ApiError(code = response.code(), message = String(buffer))),
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

  private suspend fun parseApiError(
    context: Context?,
    responseBody: ResponseBody
  ): ApiError? =
    withContext(Dispatchers.IO) {
      context ?: return@withContext null
      try {
        val errorConverter: Converter<ResponseBody, ApiError> =
          ApiHelper.getInstance(context).retrofit.responseBodyConverter(
            ApiError::class.java,
            arrayOfNulls(0)
          )
        return@withContext errorConverter.convert(responseBody)
      } catch (e: Exception) {
        e.printStackTrace()
      }
      return@withContext null
    }
}
