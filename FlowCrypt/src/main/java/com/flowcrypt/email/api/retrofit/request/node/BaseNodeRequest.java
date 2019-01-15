package com.flowcrypt.email.api.retrofit.request.node;

import com.flowcrypt.email.api.retrofit.node.NodeService;

import java.io.IOException;

import retrofit2.Response;

/**
 * It's a base request for the Node backend.
 *
 * @author Denis Bondarenko
 * Date: 1/10/19
 * Time: 5:43 PM
 * E-mail: DenBond7@gmail.com
 */
public interface BaseNodeRequest {
  String getEndpoint();

  byte[] getData();

  Response getResponse(NodeService nodeService) throws IOException;

  Class getResponseClass();
}
