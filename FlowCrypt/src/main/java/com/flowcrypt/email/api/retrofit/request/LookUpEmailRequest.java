/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request;

import com.flowcrypt.email.api.retrofit.ApiName;
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailModel;

/**
 * This class describes the request to the API "https://attester.cryptup.io/lookup/email"
 *
 * @author DenBond7
 *         Date: 24.04.2017
 *         Time: 13:24
 *         E-mail: DenBond7@gmail.com
 */

public class LookUpEmailRequest extends BaseRequest {

    private PostLookUpEmailModel postLookUpEmailModel;

    public LookUpEmailRequest(PostLookUpEmailModel postLookUpEmailModel) {
        super(ApiName.POST_LOOKUP_EMAIL);
        this.postLookUpEmailModel = postLookUpEmailModel;
    }

    public PostLookUpEmailModel getPostLookUpEmailModel() {
        return postLookUpEmailModel;
    }
}
