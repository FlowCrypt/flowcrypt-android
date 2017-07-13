/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.attester;

import com.flowcrypt.email.api.retrofit.ApiName;
import com.flowcrypt.email.api.retrofit.request.BaseRequest;
import com.flowcrypt.email.api.retrofit.request.model.ReplaceRequestModel;

/**
 * This class describes a request to the https://attester.cryptup.io/replace/request API.
 * <p>
 * <code>POST /replace/request  {
 * "signed_message" (<type 'str'>)  # signed request_replace packet (using old/original key)
 * "new_pubkey" (<type 'str'>)  # new pubkey
 * "email" (<type 'str'>)  # email this request_replace packet is associated with
 * }</code>
 *
 * @author Denis Bondarenko
 *         Date: 13.07.2017
 *         Time: 10:02
 *         E-mail: DenBond7@gmail.com
 */

public class ReplaceRequest extends BaseRequest {
    private ReplaceRequestModel replaceRequestModel;

    public ReplaceRequest(ReplaceRequestModel replaceRequestModel) {
        super(ApiName.POST_REPLACE_REQUEST);
        this.replaceRequestModel = replaceRequestModel;
    }

    public ReplaceRequestModel getReplaceRequestModel() {
        return replaceRequestModel;
    }
}
