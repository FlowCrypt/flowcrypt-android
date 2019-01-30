/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.node.exception;

/**
 * @author DenBond7
 */
public class NodeNotReady extends Exception {
  public NodeNotReady(String message, Throwable cause) {
    super(message, cause);
  }

  public NodeNotReady(String message) {
    super(message);
  }
}
