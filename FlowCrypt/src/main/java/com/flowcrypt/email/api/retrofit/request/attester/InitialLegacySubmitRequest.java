/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.attester;

import com.flowcrypt.email.api.retrofit.ApiName;
import com.flowcrypt.email.api.retrofit.request.BaseRequest;
import com.flowcrypt.email.api.retrofit.request.model.InitialLegacySubmitModel;

/**
 * This class describes a request to the https://attester.flowcrypt.com/initial/legacy_submit API.
 * <p>
 * <code>POST /initial/legacy_submit  {
 * "email" (<type 'str'>)  # email to use pubkey for
 * "pubkey" (<type 'str'>)  # ascii armored pubkey
 * [voluntary] "attest" (True, False)  # send attestation email
 * }</code>
 *
 * @author Denis Bondarenko
 *         Date: 15.01.2018
 *         Time: 16:42
 *         E-mail: DenBond7@gmail.com
 */

public class InitialLegacySubmitRequest extends BaseRequest<InitialLegacySubmitModel> {
    public InitialLegacySubmitRequest(InitialLegacySubmitModel initialLegacySubmitModel) {
        super(ApiName.POST_INITIAL_LEGACY_SUBMIT, initialLegacySubmitModel);
    }
}
