/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.attester;

import com.flowcrypt.email.api.retrofit.ApiName;
import com.flowcrypt.email.api.retrofit.request.BaseRequest;
import com.flowcrypt.email.api.retrofit.request.model.InitialConfirmModel;

/**
 * This class describes a request to the https://attester.cryptup.io/initial/confirm API.
 * <p>
 * <code>POST /initial/confirm  {
 * [voluntary] "signed_message" (<type 'str'>)  # Signed attest packet if attesting first time
 * }</code>
 *
 * @author Denis Bondarenko
 *         Date: 12.07.2017
 *         Time: 17:09
 *         E-mail: DenBond7@gmail.com
 */

public class InitialConfirmRequest extends BaseRequest {
    private InitialConfirmModel initialConfirmModel;

    public InitialConfirmRequest(InitialConfirmModel initialConfirmModel) {
        super(ApiName.POST_INITIAL_CONFIRM);
        this.initialConfirmModel = initialConfirmModel;
    }

    public InitialConfirmModel getInitialConfirmModel() {
        return initialConfirmModel;
    }
}
