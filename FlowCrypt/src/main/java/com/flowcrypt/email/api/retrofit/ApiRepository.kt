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
import com.flowcrypt.email.api.retrofit.response.base.ApiResult

/**
 * It's a repository interface for the whole API calls over the app
 *
 * @author Denis Bondarenko
 *         Date: 10/24/19
 *         Time: 6:08 PM
 *         E-mail: DenBond7@gmail.com
 */
interface ApiRepository {
  /**
   * @param context Interface to global information about an application environment.
   * @param request An instance of [LoginRequest].
   */
  suspend fun login(context: Context, request: LoginRequest): ApiResult<LoginResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param request An instance of [DomainRulesRequest].
   */
  suspend fun getDomainRules(context: Context, request: DomainRulesRequest): ApiResult<DomainRulesResponse>

  /**
   * @param context Interface to global information about an application environment.
   * @param model An instance of [InitialLegacySubmitModel].
   */
  suspend fun submitPubKey(context: Context, model: InitialLegacySubmitModel): ApiResult<InitialLegacySubmitResponse>
}