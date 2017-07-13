/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.attester;

import com.flowcrypt.email.api.retrofit.ApiName;
import com.flowcrypt.email.api.retrofit.request.BaseRequest;
import com.flowcrypt.email.api.retrofit.request.model.InitialRequestModel;

/**
 * This class describes a request to the https://attester.cryptup.io/initial/request API.
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
