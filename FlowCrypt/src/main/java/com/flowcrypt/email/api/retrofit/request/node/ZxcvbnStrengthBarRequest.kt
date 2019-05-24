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
 * Using this class we can create a request to check is the passphrase is strong.
 *
 * @author Denis Bondarenko
 * Date: 4/1/19
 * Time: 3:06 PM
 * E-mail: DenBond7@gmail.com
 */
public class ZxcvbnStrengthBarRequest extends BaseNodeRequest {

  @Expose
  private double guesses;

  @Expose
  private String purpose;

  public ZxcvbnStrengthBarRequest(double guesses) {
    this.guesses = guesses;
    this.purpose = "passphrase";
  }

  @Override
  public String getEndpoint() {
    return "zxcvbnStrengthBar";
  }

  @Override
  public Response getResponse(NodeService nodeService) throws IOException {
    if (nodeService != null) {
      return nodeService.zxcvbnStrengthBar(this).execute();
    } else return null;
  }
}
