/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.attester;

import com.flowcrypt.email.api.retrofit.ApiName;
import com.flowcrypt.email.api.retrofit.request.BaseRequest;
import com.flowcrypt.email.api.retrofit.request.model.InitialConfirmModel;

/**
 * This class describes a request to the https://attester.flowcrypt.com/initial/confirm API.
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

public class InitialConfirmRequest extends BaseRequest<InitialConfirmModel> {
    public InitialConfirmRequest(InitialConfirmModel initialConfirmModel) {
        super(ApiName.POST_INITIAL_CONFIRM, initialConfirmModel);
    }
}
