/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.attester;

import com.flowcrypt.email.api.retrofit.ApiName;
import com.flowcrypt.email.api.retrofit.request.BaseRequest;
import com.flowcrypt.email.api.retrofit.request.model.InitialRequestModel;

/**
 * This class describes a request to the https://attester.flowcrypt.com/initial/request API.
 * <p>
 * <code>POST /initial/request  {
 * "email" (<type 'str'>)  # email to use pubkey for
 * "pubkey" (<type 'str'>)  # ascii armored pubkey
 * [voluntary] "attest" (True, False)  # send attestation email (for backward compatibility,
 * ignored, treated as always True)
 * }</code>
 *
 * @author Denis Bondarenko
 *         Date: 12.07.2017
 *         Time: 16:47
 *         E-mail: DenBond7@gmail.com
 */

public class InitialRequest extends BaseRequest<InitialRequestModel> {
    public InitialRequest(InitialRequestModel initialRequestModel) {
        super(ApiName.POST_INITIAL_REQUEST, initialRequestModel);
    }
}
