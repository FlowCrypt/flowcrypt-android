/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit;

import com.flowcrypt.email.api.retrofit.request.model.PostHelpFeedbackModel;
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailModel;
import com.flowcrypt.email.api.retrofit.response.api.PostHelpFeedbackResponse;
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailResponse;

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
     * This method create a Call object for the API "https://attester.flowcrypt.com/lookup/email"
     *
     * @param postLookUpEmailModel POJO model for requests
     * @return <tt>Call<LookUpEmailResponse></tt>
     */
    @POST("/lookup/email")
    Call<LookUpEmailResponse> postLookUpEmail(@Body PostLookUpEmailModel postLookUpEmailModel);

    /**
     * This method create a Call object for the API "https://api.cryptup.io/help/feedback"
     *
     * @param postHelpFeedbackModel POJO model for requests
     * @return <tt>Call<PostHelpFeedbackResponse></tt>
     */
    @POST("https://api.cryptup.io/help/feedback")
    Call<PostHelpFeedbackResponse> postHelpFeedbackResponse(@Body PostHelpFeedbackModel
                                                                    postHelpFeedbackModel);
}
