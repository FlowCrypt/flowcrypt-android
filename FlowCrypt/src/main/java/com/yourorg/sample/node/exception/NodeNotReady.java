package com.yourorg.sample.node.exception;

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
