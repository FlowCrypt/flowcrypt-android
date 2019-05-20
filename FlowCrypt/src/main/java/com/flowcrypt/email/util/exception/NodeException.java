/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception;

import com.flowcrypt.email.api.retrofit.response.model.node.Error;

/**
 * It's a base Node exception.
 *
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 10:08 AM
 * E-mail: DenBond7@gmail.com
 */
public class NodeException extends Exception {
  private Error nodeError;

  public NodeException(Error nodeError) {
    this.nodeError = nodeError;
  }

  @Override
  public String getMessage() {
    StringBuilder builder = new StringBuilder();
    if (nodeError != null) {
      builder.append("Error :").append(nodeError.getMsg()).append("\n");
      builder.append("Stack :").append(nodeError.getStack()).append("\n");
      builder.append("Type :").append(nodeError.getType());
    }
    return builder.toString();
  }

  public Error getNodeError() {
    return nodeError;
  }
}
