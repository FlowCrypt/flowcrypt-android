/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

import android.content.Context
import com.flowcrypt.email.api.retrofit.request.api.DomainRulesRequest
import com.flowcrypt.email.api.retrofit.request.api.LoginRequest
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel
import com.flowcrypt.email.api.retrofit.response.api.DomainRulesResponse
import com.flowcrypt.email.api.retrofit.response.api.LoginResponse
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiResult
import com.flowcrypt.email.util.exception.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Implementation of Flowcrypt API
 *
 * @author Denis Bondarenko
 *         Date: 10/24/19
 *         Time: 6:10 PM
 *         E-mail: DenBond7@gmail.com
 */
class FlowcryptApiRepository : ApiRepository {
  override suspend fun login(context: Context, request: LoginRequest): ApiResult<LoginResponse> =
      withContext(Dispatchers.IO) {
        val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
        getResult { apiService.postLogin(request.requestModel, "Bearer ${request.tokenId}") }
      }

  override suspend fun getDomainRules(context: Context, request: DomainRulesRequest): ApiResult<DomainRulesResponse> =
      withContext(Dispatchers.IO) {
        val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
        getResult { apiService.getDomainRules(request.requestModel) }
      }

  override suspend fun submitPubKey(context: Context, model: InitialLegacySubmitModel): ApiResult<InitialLegacySubmitResponse> =
      withContext(Dispatchers.IO) {
        val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
        getResult { apiService.submitPubKey(model) }
      }

  /**
   * Base implementation for the API calls
   */
  private suspend fun <T> getResult(call: suspend () -> Response<T>): ApiResult<T> {
    return try {
      val response = call()
      if (response.isSuccessful) {
        val body = response.body()
        if (body != null) {
          if (body is ApiResponse) {
            return if (body.apiError != null) {
              ApiResult.error(body)
            } else {
              ApiResult.success(body)
            }
          } else {
            ApiResult.success(body)
          }
        } else {
          ApiResult.exception(ApiException(ApiError(response.code(), response.message())))
        }
      } else {
        ApiResult.exception(ApiException(ApiError(response.code(), response.message())))
      }
    } catch (e: Exception) {
      e.printStackTrace()
      ApiResult.exception(e)
    }
  }
}