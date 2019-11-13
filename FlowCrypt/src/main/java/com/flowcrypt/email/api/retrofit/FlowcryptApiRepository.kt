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
import com.flowcrypt.email.api.retrofit.response.base.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of Flowcrypt API
 *
 * @author Denis Bondarenko
 *         Date: 10/24/19
 *         Time: 6:10 PM
 *         E-mail: DenBond7@gmail.com
 */
class FlowcryptApiRepository : ApiRepository {
  override suspend fun login(context: Context, request: LoginRequest): Result<LoginResponse> =
      withContext(Dispatchers.IO) {
        val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
        getResult { apiService.postLogin(request.requestModel, "Bearer ${request.tokenId}") }
      }

  override suspend fun getDomainRules(context: Context, request: DomainRulesRequest): Result<DomainRulesResponse> =
      withContext(Dispatchers.IO) {
        val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
        getResult { apiService.getDomainRules(request.requestModel) }
      }

  override suspend fun submitPubKey(context: Context, model: InitialLegacySubmitModel): Result<InitialLegacySubmitResponse> =
      withContext(Dispatchers.IO) {
        val apiService = ApiHelper.getInstance(context).retrofit.create(ApiService::class.java)
        getResult { apiService.submitPubKey(model) }
      }
}