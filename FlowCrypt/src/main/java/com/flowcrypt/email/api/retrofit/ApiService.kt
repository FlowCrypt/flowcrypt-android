/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit

import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel
import com.flowcrypt.email.api.retrofit.request.model.PostHelpFeedbackModel
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailModel
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailsModel
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel
import com.flowcrypt.email.api.retrofit.response.api.PostHelpFeedbackResponse
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailResponse
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailsResponse
import com.flowcrypt.email.api.retrofit.response.attester.TestWelcomeResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * A base API interface for RETROFIT.
 *
 * @author Denis Bondarenko
 * Date: 10.03.2015
 * Time: 13:39
 * E-mail: DenBond7@gmail.com
 */
interface ApiService {

  /**
   * This method create a [Call] object for the API "https://flowcrypt.com/attester/lookup/email"
   *
   * @param body POJO model for requests
   * @return [<]
   */
  @POST("lookup/email")
  fun postLookUpEmail(@Body body: PostLookUpEmailModel): Call<LookUpEmailResponse>

  /**
   * This method create a [Call] object for the API "https://flowcrypt.com/attester/lookup/email"
   *
   * @param body POJO model for requests
   * @return [<]
   */
  @POST("lookup/email")
  fun postLookUpEmails(@Body body: PostLookUpEmailsModel): Call<LookUpEmailsResponse>

  /**
   * This method create a [Call] object for the API "https://flowcrypt.com/attester/initial/legacy_submit"
   *
   * @param body POJO model for requests
   * @return [<]
   */
  @POST("initial/legacy_submit")
  fun postInitialLegacySubmit(@Body body: InitialLegacySubmitModel): Call<InitialLegacySubmitResponse>

  /**
   * This method create a [Call] object for the API "https://flowcrypt.com/attester/test/welcome"
   *
   * @param body POJO model for requests
   * @return [<]
   */
  @POST("test/welcome")
  fun postTestWelcome(@Body body: TestWelcomeModel): Call<TestWelcomeResponse>

  /**
   * This method create a [Call] object for the API "https://flowcrypt.com/api/help/feedback"
   *
   * @param body POJO model for requests
   * @return [<]
   */
  @POST(Constants.FLOWCRYPT_API_URL + "/help/feedback")
  fun postHelpFeedback(@Body body: PostHelpFeedbackModel): Call<PostHelpFeedbackResponse>

  /**
   * This method create a [Call] object for the API "https://flowcrypt.com/attester/pub"
   *
   * @return [<]
   */
  @GET("pub/{keyIdOrEmail}")
  fun getPub(@Path("keyIdOrEmail") keyIdOrEmail: String): Call<String>
}
