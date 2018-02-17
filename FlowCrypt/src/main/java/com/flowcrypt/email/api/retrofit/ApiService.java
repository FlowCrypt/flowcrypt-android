/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit;

import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel;
import com.flowcrypt.email.api.retrofit.request.model.PostHelpFeedbackModel;
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailModel;
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailsModel;
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel;
import com.flowcrypt.email.api.retrofit.response.api.PostHelpFeedbackResponse;
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse;
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailResponse;
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailsResponse;
import com.flowcrypt.email.api.retrofit.response.attester.TestWelcomeResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * A base API interface for RETROFIT.
 *
 * @author Denis Bondarenko
 *         Date: 10.03.2015
 *         Time: 13:39
 *         E-mail: DenBond7@gmail.com
 */
public interface ApiService {

    /**
     * This method create a {@link Call} object for the API "https://attester.flowcrypt.com/lookup/email"
     *
     * @param postLookUpEmailModel POJO model for requests
     * @return {@link Call<LookUpEmailResponse>}
     */
    @POST("/lookup/email")
    Call<LookUpEmailResponse> postLookUpEmail(@Body PostLookUpEmailModel postLookUpEmailModel);

    /**
     * This method create a {@link Call} object for the API "https://attester.flowcrypt.com/lookup/email"
     *
     * @param postLookUpEmailsModel POJO model for requests
     * @return {@link Call<LookUpEmailsResponse>}
     */
    @POST("/lookup/email")
    Call<LookUpEmailsResponse> postLookUpEmails(@Body PostLookUpEmailsModel postLookUpEmailsModel);

    /**
     * This method create a {@link Call} object for the API "https://attester.flowcrypt.com/initial/legacy_submit"
     *
     * @param initialLegacySubmitModel POJO model for requests
     * @return {@link Call<InitialLegacySubmitResponse>}
     */
    @POST("/initial/legacy_submit")
    Call<InitialLegacySubmitResponse> postInitialLegacySubmit(@Body InitialLegacySubmitModel initialLegacySubmitModel);

    /**
     * This method create a {@link Call} object for the API "https://attester.flowcrypt.com/test/welcome"
     *
     * @param testWelcomeModel POJO model for requests
     * @return {@link Call< TestWelcomeResponse >}
     */
    @POST("/test/welcome")
    Call<TestWelcomeResponse> postTestWelcome(@Body TestWelcomeModel testWelcomeModel);

    /**
     * This method create a {@link Call} object for the API "https://api.cryptup.io/help/feedback"
     *
     * @param postHelpFeedbackModel POJO model for requests
     * @return {@link Call<PostHelpFeedbackResponse>}
     */
    @POST("https://api.cryptup.io/help/feedback")
    Call<PostHelpFeedbackResponse> postHelpFeedbackResponse(@Body PostHelpFeedbackModel
                                                                    postHelpFeedbackModel);
}
