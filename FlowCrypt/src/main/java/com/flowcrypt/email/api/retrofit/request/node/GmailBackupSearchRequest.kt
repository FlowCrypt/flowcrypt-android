/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node;

import com.flowcrypt.email.api.retrofit.node.NodeService;
import com.google.gson.annotations.Expose;

import java.io.IOException;

import retrofit2.Response;

/**
 * Using this class we can create a request to receive a special search string for Gmail provider.
 *
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 9:53 AM
 * E-mail: DenBond7@gmail.com
 */
public class GmailBackupSearchRequest extends BaseNodeRequest {

  @Expose
  private String acctEmail;

  public GmailBackupSearchRequest(String acctEmail) {
    this.acctEmail = acctEmail;
  }

  @Override
  public String getEndpoint() {
    return "gmailBackupSearch";
  }


  @Override
  public Response getResponse(NodeService nodeService) throws IOException {
    if (nodeService != null) {
      return nodeService.gmailBackupSearch(this).execute();
    } else return null;
  }
}
