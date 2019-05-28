/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel;
import com.flowcrypt.email.api.retrofit.request.model.PostHelpFeedbackModel;
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailModel;
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailsModel;
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel;
import com.flowcrypt.email.api.retrofit.response.api.PostHelpFeedbackResponse;
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse;
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailResponse;
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailsResponse;
import com.flowcrypt.email.api.retrofit.response.attester.LookUpResponse;
import com.flowcrypt.email.api.retrofit.response.attester.TestWelcomeResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

/**
 * A base API interface for RETROFIT.
 *
 * @author Denis Bondarenko
 * Date: 10.03.2015
 * Time: 13:39
 * E-mail: DenBond7@gmail.com
 */
public interface ApiService {

  /**
   * This method create a {@link Call} object for the API "https://flowcrypt.com/attester/lookup/email"
   *
   * @param body POJO model for requests
   * @return {@link Call<LookUpEmailResponse>}
   */
  @POST("lookup/email")
  Call<LookUpEmailResponse> postLookUpEmail(@Body PostLookUpEmailModel body);

  /**
   * This method create a {@link Call} object for the API "https://flowcrypt.com/attester/lookup/email"
   *
   * @param body POJO model for requests
   * @return {@link Call<LookUpEmailsResponse>}
   */
  @POST("lookup/email")
  Call<LookUpEmailsResponse> postLookUpEmails(@Body PostLookUpEmailsModel body);

  /**
   * This method create a {@link Call} object for the API "https://flowcrypt.com/attester/initial/legacy_submit"
   *
   * @param body POJO model for requests
   * @return {@link Call<InitialLegacySubmitResponse>}
   */
  @POST("initial/legacy_submit")
  Call<InitialLegacySubmitResponse> postInitialLegacySubmit(@Body InitialLegacySubmitModel body);

  /**
   * This method create a {@link Call} object for the API "https://flowcrypt.com/attester/test/welcome"
   *
   * @param body POJO model for requests
   * @return {@link Call<TestWelcomeResponse>}
   */
  @POST("test/welcome")
  Call<TestWelcomeResponse> postTestWelcome(@Body TestWelcomeModel body);

  /**
   * This method create a {@link Call} object for the API "https://flowcrypt.com/api/help/feedback"
   *
   * @param body POJO model for requests
   * @return {@link Call<PostHelpFeedbackResponse>}
   */
  @POST(Constants.FLOWCRYPT_API_URL + "/help/feedback")
  Call<PostHelpFeedbackResponse> postHelpFeedbackResponse(@Body PostHelpFeedbackModel body);

  /**
   * This method create a {@link Call} object for the API "https://flowcrypt.com/attester/lookup"
   *
   * @return {@link Call<LookUpResponse>}
   */
  @GET("lookup/{keyIdOrEmail}")
  Call<LookUpResponse> getLookUp(@Path("keyIdOrEmail") String keyIdOrEmail);
}
