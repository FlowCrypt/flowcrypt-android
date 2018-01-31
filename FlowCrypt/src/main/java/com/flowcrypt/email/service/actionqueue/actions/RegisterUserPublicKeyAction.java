/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue.actions;

import android.content.Context;
import android.os.Parcel;

import com.flowcrypt.email.api.retrofit.ApiHelper;
import com.flowcrypt.email.api.retrofit.ApiService;
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel;
import com.flowcrypt.email.api.retrofit.response.attester.InitialLegacySubmitResponse;
import com.flowcrypt.email.util.exception.ApiException;

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
    public static final Creator<RegisterUserPublicKeyAction> CREATOR = new Creator<RegisterUserPublicKeyAction>() {
        @Override
        public RegisterUserPublicKeyAction createFromParcel(Parcel source) {
            return new RegisterUserPublicKeyAction(source);
        }

        @Override
        public RegisterUserPublicKeyAction[] newArray(int size) {
            return new RegisterUserPublicKeyAction[size];
        }
    };

    private String publicKey;

    public RegisterUserPublicKeyAction(String email, String publicKey) {
        super(email, ActionType.REGISTER_USER_PUBLIC_KEY);
        this.publicKey = publicKey;
    }


    protected RegisterUserPublicKeyAction(Parcel in) {
        super(in);
        this.publicKey = in.readString();
    }

    @Override
    public void run(Context context) throws IOException {
        ApiService apiService = ApiHelper.getInstance(context).getRetrofit().create(ApiService.class);
        Response<InitialLegacySubmitResponse> response = apiService.postInitialLegacySubmit(
                new InitialLegacySubmitModel(email, publicKey)).execute();

        InitialLegacySubmitResponse initialLegacySubmitResponse = response.body();
        if (initialLegacySubmitResponse == null) {
            throw new IllegalArgumentException("The response is null!");
        }

        if (initialLegacySubmitResponse.getApiError() != null) {
            throw new ApiException(initialLegacySubmitResponse.getApiError());
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(this.publicKey);
    }
}
