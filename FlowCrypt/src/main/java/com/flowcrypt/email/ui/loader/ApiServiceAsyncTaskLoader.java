/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.content.Context;

import com.flowcrypt.email.api.retrofit.ApiHelper;
import com.flowcrypt.email.api.retrofit.ApiService;
import com.flowcrypt.email.api.retrofit.BaseResponse;
import com.flowcrypt.email.api.retrofit.request.BaseRequest;
import com.flowcrypt.email.api.retrofit.request.api.PostHelpFeedbackRequest;
import com.flowcrypt.email.api.retrofit.request.attester.LookUpEmailRequest;
import com.flowcrypt.email.api.retrofit.request.attester.LookUpRequest;
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel;
import com.flowcrypt.email.api.retrofit.response.api.PostHelpFeedbackResponse;
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse;
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailResponse;
import com.flowcrypt.email.api.retrofit.response.attester.LookUpResponse;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.util.exception.ExceptionUtil;

import androidx.loader.content.AsyncTaskLoader;

/**
 * A basic AsyncTaskLoader who make API calls.
 *
 * @author Denis Bondarenko
 * Date: 10.03.2015
 * Time: 13:42
 * E-mail: DenBond7@gmail.com
 */
public class ApiServiceAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {
  private ApiHelper apiHelper;
  private BaseRequest baseRequest;
  private ApiService apiService;

  public ApiServiceAsyncTaskLoader(Context context, BaseRequest baseRequest) {
    super(context);
    this.apiHelper = ApiHelper.getInstance(context);
    this.baseRequest = baseRequest;
    onContentChanged();
  }

  @Override
  public void onStartLoading() {
    if (takeContentChanged()) {
      forceLoad();
    }
  }

  @Override
  public LoaderResult loadInBackground() {
    BaseResponse baseResponse = null;

    if (apiHelper != null && apiHelper.getRetrofit() != null) {
      apiService = apiHelper.getRetrofit().create(ApiService.class);

      if (baseRequest != null && baseRequest.getApiName() != null) {
        switch (baseRequest.getApiName()) {
          case POST_LOOKUP_EMAIL_SINGLE:
            BaseResponse<LookUpEmailResponse> lookUpEmailResponse =
                new BaseResponse<>();
            lookUpEmailResponse.setApiName(baseRequest.getApiName());

            LookUpEmailRequest lookUpEmailRequest = (LookUpEmailRequest) baseRequest;

            if (apiService != null) {
              try {
                lookUpEmailResponse.setResponse(apiService.postLookUpEmail
                    (lookUpEmailRequest.getRequestModel()).execute());
              } catch (Exception e) {
                lookUpEmailResponse.setException(catchException(e));
              }
            }
            baseResponse = lookUpEmailResponse;
            break;

          case POST_HELP_FEEDBACK:
            BaseResponse<PostHelpFeedbackResponse> postHelpFeedbackResponse =
                new BaseResponse<>();
            postHelpFeedbackResponse.setApiName(baseRequest.getApiName());

            PostHelpFeedbackRequest postHelpFeedbackRequest = (PostHelpFeedbackRequest) baseRequest;

            if (apiService != null) {
              try {
                postHelpFeedbackResponse.setResponse(apiService
                    .postHelpFeedbackResponse(postHelpFeedbackRequest
                        .getRequestModel()).execute());
              } catch (Exception e) {
                postHelpFeedbackResponse.setException(catchException(e));
              }
            }
            baseResponse = postHelpFeedbackResponse;
            break;

          case POST_INITIAL_LEGACY_SUBMIT:
            BaseResponse<InitialLegacySubmitResponse> initialLegacySubmitResponse = new BaseResponse<>();
            initialLegacySubmitResponse.setApiName(baseRequest.getApiName());

            if (apiService != null) {
              try {
                initialLegacySubmitResponse.setResponse(apiService.postInitialLegacySubmit(
                    (InitialLegacySubmitModel) baseRequest.getRequestModel()).execute());
              } catch (Exception e) {
                initialLegacySubmitResponse.setException(catchException(e));
              }
            }
            baseResponse = initialLegacySubmitResponse;
            break;

          case GET_LOOKUP:
            BaseResponse<LookUpResponse> lookUpResponse = new BaseResponse<>();
            lookUpResponse.setApiName(baseRequest.getApiName());

            if (apiService != null) {
              try {
                lookUpResponse.setResponse(apiService.getLookUp(
                    ((LookUpRequest) baseRequest).getQuery()).execute());
              } catch (Exception e) {
                e.printStackTrace();
                ExceptionUtil.handleError(e);
                lookUpResponse.setException(e);
              }
            }
            baseResponse = lookUpResponse;
            break;
        }
      }
    }

    return new LoaderResult(baseResponse, null);
  }

  @Override
  public void onStopLoading() {
    cancelLoad();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ApiServiceAsyncTaskLoader)) return false;

    ApiServiceAsyncTaskLoader that = (ApiServiceAsyncTaskLoader) o;

    if (apiHelper != null ? !apiHelper.equals(that.apiHelper) : that
        .apiHelper != null) {
      return false;
    }
    if (baseRequest != null ? !baseRequest.equals(that.baseRequest) : that.baseRequest != null) {
      return false;
    }
    return !(apiService != null ? !apiService.equals(that.apiService) : that.apiService !=
        null);

  }

  @Override
  public int hashCode() {
    int result = apiHelper != null ? apiHelper.hashCode() : 0;
    result = 31 * result + (baseRequest != null ? baseRequest.hashCode() : 0);
    result = 31 * result + (apiService != null ? apiService.hashCode() : 0);
    return result;
  }

  private Exception catchException(Exception e) {
    e.printStackTrace();
    ExceptionUtil.handleError(e);
    return e;
  }
}
