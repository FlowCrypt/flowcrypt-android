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
  private Error error;

  public NodeException(Error error) {
    this.error = error;
  }

  @Override
  public String getMessage() {
    StringBuilder builder = new StringBuilder();
    if (error != null) {
      builder.append("Error :").append(error.getMsg()).append("\n");
      builder.append("Stack :").append(error.getStack()).append("\n");
      builder.append("Type :").append(error.getType());
    }
    return builder.toString();
  }
}
