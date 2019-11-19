/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.node

import com.flowcrypt.email.api.retrofit.request.node.DecryptKeyRequest
import com.flowcrypt.email.api.retrofit.response.node.DecryptKeyResult
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * @author Denis Bondarenko
 *         Date: 11/12/19
 *         Time: 3:58 PM
 *         E-mail: DenBond7@gmail.com
 */
interface NodeApiService {
  @POST("/")
  suspend fun decryptKey(@Body request: DecryptKeyRequest): Response<DecryptKeyResult>
}