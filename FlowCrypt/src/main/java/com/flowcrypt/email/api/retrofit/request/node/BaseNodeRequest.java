package com.flowcrypt.email.api.retrofit.request.node;

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
}
