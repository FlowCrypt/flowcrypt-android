/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions;

import android.content.Context;

import com.flowcrypt.email.api.retrofit.ApiHelper;
import com.flowcrypt.email.api.retrofit.ApiService;
import com.flowcrypt.email.api.retrofit.request.model.TestWelcomeModel;
import com.flowcrypt.email.api.retrofit.response.attester.TestWelcomeResponse;

import java.io.IOException;

import retrofit2.Response;

/**
 * This action describes a task which sends a welcome message to the user
 * using API "https://attester.flowcrypt.com/test/welcome".
 *
 * @author Denis Bondarenko
 *         Date: 30.01.2018
 *         Time: 18:10
 *         E-mail: DenBond7@gmail.com
 */

public class SendWelcomeTestEmailAction extends Action {
    private String publicKey;

    public SendWelcomeTestEmailAction(String email, String publicKey) {
        super(email, ActionType.SEND_WELCOME_TEST_EMAIL);
        this.publicKey = publicKey;
    }

    @Override
    public boolean run(Context context) {
        try {
            ApiService apiService = ApiHelper.getInstance(context).getRetrofit().create(ApiService.class);
            Response<TestWelcomeResponse> response = apiService.postTestWelcome(new TestWelcomeModel(email,
                    publicKey)).execute();

            TestWelcomeResponse testWelcomeResponse = response.body();
            return testWelcomeResponse != null && testWelcomeResponse.isSent();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}