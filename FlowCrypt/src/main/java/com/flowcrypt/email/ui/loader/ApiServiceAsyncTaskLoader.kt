/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import com.flowcrypt.email.api.retrofit.request.api.PostHelpFeedbackRequest
import com.flowcrypt.email.api.retrofit.request.attester.LookUpEmailRequest
import com.flowcrypt.email.api.retrofit.request.attester.LookUpRequest
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel
import com.flowcrypt.email.api.retrofit.response.api.PostHelpFeedbackResponse
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailResponse
import com.flowcrypt.email.api.retrofit.response.attester.LookUpResponse
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
          ApiName.POST_LOOKUP_EMAIL_SINGLE -> {
            val lookUpEmailResponse = BaseResponse<LookUpEmailResponse>()
            lookUpEmailResponse.apiName = baseRequest.apiName

            val requestModel = baseRequest as LookUpEmailRequest?

            if (apiService != null) {
              try {
                lookUpEmailResponse.setResponse(apiService!!.postLookUpEmail(requestModel!!.requestModel).execute())
              } catch (e: Exception) {
                lookUpEmailResponse.exception = catchException(e)
              }

            }
            baseResponse = lookUpEmailResponse
          }

          ApiName.POST_HELP_FEEDBACK -> {
            val postHelpFeedbackResponse = BaseResponse<PostHelpFeedbackResponse>()
            postHelpFeedbackResponse.apiName = baseRequest.apiName

            val postHelpFeedbackRequest = baseRequest as PostHelpFeedbackRequest?

            if (apiService != null) {
              try {
                postHelpFeedbackResponse.setResponse(apiService!!.postHelpFeedbackResponse(postHelpFeedbackRequest!!
                    .requestModel).execute())
              } catch (e: Exception) {
                postHelpFeedbackResponse.exception = catchException(e)
              }

            }
            baseResponse = postHelpFeedbackResponse
          }

          ApiName.POST_INITIAL_LEGACY_SUBMIT -> {
            val initialLegacySubmitResponse = BaseResponse<InitialLegacySubmitResponse>()
            initialLegacySubmitResponse.apiName = baseRequest.apiName

            if (apiService != null) {
              try {
                initialLegacySubmitResponse.setResponse(apiService!!.postInitialLegacySubmit(
                    baseRequest.requestModel as InitialLegacySubmitModel).execute())
              } catch (e: Exception) {
                initialLegacySubmitResponse.exception = catchException(e)
              }

            }
            baseResponse = initialLegacySubmitResponse
          }

          ApiName.GET_LOOKUP -> {
            val lookUpResponse = BaseResponse<LookUpResponse>()
            lookUpResponse.apiName = baseRequest.apiName

            if (apiService != null) {
              try {
                lookUpResponse.setResponse(apiService!!.getLookUp((baseRequest as LookUpRequest).query).execute())
              } catch (e: Exception) {
                e.printStackTrace()
                ExceptionUtil.handleError(e)
                lookUpResponse.exception = e
              }

            }
            baseResponse = lookUpResponse
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
