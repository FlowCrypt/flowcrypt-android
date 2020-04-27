/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader

import android.content.Context
import androidx.loader.content.AsyncTaskLoader
import com.flowcrypt.email.api.retrofit.ApiHelper
import com.flowcrypt.email.api.retrofit.ApiName
import com.flowcrypt.email.api.retrofit.ApiService
import com.flowcrypt.email.api.retrofit.BaseResponse
import com.flowcrypt.email.api.retrofit.request.BaseRequest
import com.flowcrypt.email.api.retrofit.request.attester.PubRequest
import com.flowcrypt.email.api.retrofit.response.attester.PubResponse
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.util.exception.ExceptionUtil

/**
 * A basic AsyncTaskLoader who make API calls.
 *
 * @author Denis Bondarenko
 * Date: 10.03.2015
 * Time: 13:42
 * E-mail: DenBond7@gmail.com
 */
class ApiServiceAsyncTaskLoader(context: Context,
                                private val baseRequest: BaseRequest<*>?) : AsyncTaskLoader<LoaderResult>(context) {
  private val apiHelper: ApiHelper?
  private var apiService: ApiService? = null

  init {
    this.apiHelper = ApiHelper.getInstance(context)
    onContentChanged()
  }

  public override fun onStartLoading() {
    if (takeContentChanged()) {
      forceLoad()
    }
  }

  override fun loadInBackground(): LoaderResult? {
    var baseResponse: BaseResponse<*>? = null

    if (apiHelper?.retrofit != null) {
      apiService = apiHelper.retrofit.create(ApiService::class.java)

      if (baseRequest?.apiName != null) {
        when (baseRequest.apiName) {
          ApiName.GET_PUB -> {
            val pubResponse = BaseResponse<PubResponse>()
            pubResponse.apiName = baseRequest.apiName

            if (apiService != null) {
              try {
                val response = apiService!!.getPub((baseRequest as PubRequest).query).execute()
                pubResponse.setResponse(retrofit2.Response.success(PubResponse(null, response.body())))
              } catch (e: Exception) {
                e.printStackTrace()
                ExceptionUtil.handleError(e)
                pubResponse.exception = e
              }

            }
            baseResponse = pubResponse
          }
          else -> throw IllegalStateException("Unsupported call")
        }
      }
    }

    return LoaderResult(baseResponse, null)
  }

  public override fun onStopLoading() {
    cancelLoad()
  }

  private fun catchException(e: Exception): Exception {
    e.printStackTrace()
    ExceptionUtil.handleError(e)
    return e
  }
}
