package com.flowcrypt.email.api.retrofit.response.node;

import androidx.annotation.Nullable;

/**
 * @author DenBond7
 */
public class NodeResponseWrapper<T extends BaseNodeResult> {
  private int requestCode;
  private Throwable exception;
  private T baseNodeResult;

  public NodeResponseWrapper(int requestCode, @Nullable Throwable exception, @Nullable T baseNodeResult) {
    this.requestCode = requestCode;
    this.exception = exception;
    this.baseNodeResult = baseNodeResult;
  }

  public int getRequestCode() {
    return requestCode;
  }

  public Throwable getException() {
    return exception;
  }

  public T getResult() {
    return baseNodeResult;
  }
}
