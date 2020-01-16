/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import retrofit2.Response
import java.io.Serializable

/**
 * A base response model class.
 *
 * @author Denis Bondarenko
 * Date: 10.03.2015
 * Time: 13:34
 * E-mail: DenBond7@gmail.com
 */
class BaseResponse<T : ApiResponse> : Serializable {
  private var modelResponse: Response<T>? = null
  var exception: Exception? = null
  var apiName: ApiName? = null

  val responseModel: T?
    get() = if (modelResponse != null) modelResponse!!.body() else null

  val responseCode: Int
    get() = if (modelResponse != null) modelResponse!!.code() else -1

  fun setResponse(response: Response<T>) {
    this.modelResponse = response
  }
}
