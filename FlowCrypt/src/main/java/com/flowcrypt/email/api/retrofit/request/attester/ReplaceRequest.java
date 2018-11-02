/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.attester;

import com.flowcrypt.email.api.retrofit.ApiName;
import com.flowcrypt.email.api.retrofit.request.BaseRequest;
import com.flowcrypt.email.api.retrofit.request.model.ReplaceRequestModel;

/**
 * This class describes a request to the https://attester.flowcrypt.com/replace/request API.
 * <p>
 * <code>POST /replace/request  {
 * "signed_message" (<type 'str'>)  # signed request_replace packet (using old/original key)
 * "new_pubkey" (<type 'str'>)  # new pubkey
 * "email" (<type 'str'>)  # email this request_replace packet is associated with
 * }</code>
 *
 * @author Denis Bondarenko
 * Date: 13.07.2017
 * Time: 10:02
 * E-mail: DenBond7@gmail.com
 */

public class ReplaceRequest extends BaseRequest<ReplaceRequestModel> {
  public ReplaceRequest(ReplaceRequestModel replaceRequestModel) {
    super(ApiName.POST_REPLACE_REQUEST, replaceRequestModel);
  }
}
