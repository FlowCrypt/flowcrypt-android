/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.api;

import com.flowcrypt.email.api.retrofit.ApiName;
import com.flowcrypt.email.api.retrofit.request.BaseRequest;
import com.flowcrypt.email.api.retrofit.request.model.LinkMessageModel;

/**
 * This class describes a request to the https://flowcrypt.com/api/link/message API.
 * <p>
 * <code>POST /link/message  {
 * "short" (<type 'str'>)  # short id of the message
 * }</code>
 *
 * @author Denis Bondarenko
 *         Date: 13.07.2017
 *         Time: 15:12
 *         E-mail: DenBond7@gmail.com
 */

public class LinkMessageRequest extends BaseRequest<LinkMessageModel> {

    public LinkMessageRequest(LinkMessageModel requestModel) {
        super(ApiName.POST_LINK_MESSAGE, requestModel);
    }
}
