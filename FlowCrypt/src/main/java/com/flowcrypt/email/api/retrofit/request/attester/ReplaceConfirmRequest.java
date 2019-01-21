/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.attester;

import com.flowcrypt.email.api.retrofit.ApiName;
import com.flowcrypt.email.api.retrofit.request.BaseRequest;
import com.flowcrypt.email.api.retrofit.request.model.ReplaceConfirmModel;

/**
 * This class describes a request to the https://attester.flowcrypt.com/replace/confirm API.
 * <p>
 * <code>POST /replace/confirm  {
 * "signed_message" (<type 'str'>)  # signed confirm_replace packet (using new/replacement key)
 * }</code>
 *
 * @author Denis Bondarenko
 * Date: 13.07.2017
 * Time: 11:49
 * E-mail: DenBond7@gmail.com
 */

public class ReplaceConfirmRequest extends BaseRequest<ReplaceConfirmModel> {
  public ReplaceConfirmRequest(ReplaceConfirmModel replaceConfirmModel) {
    super(ApiName.POST_REPLACE_CONFIRM, replaceConfirmModel);
  }
}
