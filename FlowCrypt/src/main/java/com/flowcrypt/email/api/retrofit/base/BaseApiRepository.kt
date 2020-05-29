/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.base

import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.util.exception.ApiException
import retrofit2.Response

/**
 * @author Denis Bondarenko
 *         Date: 11/12/19
 *         Time: 4:07 PM
 *         E-mail: DenBond7@gmail.com
 */
interface BaseApiRepository {
  /**
   * Base implementation for the API calls
   */
  suspend fun <T> getResult(requestCode: Long = 0L, call: suspend () -> Response<T>): Result<T> {
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
          Result.exception(error = ApiException(ApiError(response.code(), response.message())), requestCode = requestCode)
        }
      } else {
        Result.exception(error = ApiException(ApiError(response.code(), response.message())), requestCode = requestCode)
      }
    } catch (e: Exception) {
      e.printStackTrace()
      Result.exception(error = e, requestCode = requestCode)
    }
  }
}