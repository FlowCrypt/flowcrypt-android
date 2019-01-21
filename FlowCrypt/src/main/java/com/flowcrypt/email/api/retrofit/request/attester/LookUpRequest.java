/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.attester;

import com.flowcrypt.email.api.retrofit.ApiName;
import com.flowcrypt.email.api.retrofit.request.BaseRequest;

/**
 * This class describes the request to the API "https://attester.flowcrypt.com/lookup"
 *
 * @author DenBond7
 * Date: 05.05.2018
 * Time: 14:46
 * E-mail: DenBond7@gmail.com
 */

public class LookUpRequest extends BaseRequest {
  private String query;

  public LookUpRequest(String query) {
    super(ApiName.GET_LOOKUP);
    this.query = query;
  }

  public String getQuery() {
    return query;
  }
}
