/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions;

import android.content.Context;

import com.flowcrypt.email.api.retrofit.ApiHelper;
import com.flowcrypt.email.api.retrofit.ApiService;
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel;
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse;

import java.io.IOException;

import retrofit2.Response;

/**
 * This action describes a task which registers a user public key
 * using API "https://attester.flowcrypt.com/initial/legacy_submit".
 *
 * @author Denis Bondarenko
 *         Date: 30.01.2018
 *         Time: 18:01
 *         E-mail: DenBond7@gmail.com
 */

public class RegisterUserPublicKeyAction extends Action {
    private String publicKey;

    public RegisterUserPublicKeyAction(String email, String publicKey) {
        super(email, ActionType.REGISTER_USER_PUBLIC_KEY);
        this.publicKey = publicKey;
    }

    @Override
    public boolean run(Context context) {
        try {
            ApiService apiService = ApiHelper.getInstance(context).getRetrofit().create(ApiService.class);
            Response<InitialLegacySubmitResponse> response = apiService.postInitialLegacySubmit(
                    new InitialLegacySubmitModel(email, publicKey)).execute();

            InitialLegacySubmitResponse initialLegacySubmitResponse = response.body();
            return initialLegacySubmitResponse != null;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
