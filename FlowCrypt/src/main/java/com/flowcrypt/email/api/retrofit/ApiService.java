package com.flowcrypt.email.api.retrofit;

import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailModel;
import com.flowcrypt.email.api.retrofit.request.model.PostMessagePrototypeModel;
import com.flowcrypt.email.api.retrofit.response.LookUpEmailResponse;
import com.flowcrypt.email.api.retrofit.response.MessagePrototypeResponse;

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
     * This method create a Call object for the API "https://attester.cryptup.io/lookup/email"
     *
     * @param postLookUpEmailModel POJO model for requests
     * @return <tt>Call<LookUpEmailResponse></tt>
     */
    @POST("/lookup/email")
    Call<LookUpEmailResponse> postLookUpEmail(@Body PostLookUpEmailModel postLookUpEmailModel);

    /**
     * This method create a Call object for the API "https://api.cryptup.io/message/prototype"
     *
     * @param PostMessagePrototypeModel POJO model for requests
     * @return <tt>Call<LookUpEmailResponse></tt>
     */
    @POST("https://api.cryptup.io/message/prototype")
    Call<MessagePrototypeResponse> postMessagePrototype(@Body PostMessagePrototypeModel
                                                                PostMessagePrototypeModel);
}
