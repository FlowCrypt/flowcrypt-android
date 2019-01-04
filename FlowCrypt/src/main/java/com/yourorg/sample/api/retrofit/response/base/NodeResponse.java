package com.yourorg.sample.api.retrofit.response.base;

import com.yourorg.sample.node.results.RawNodeResult;

/**
 * @author DenBond7
 */
public class NodeResponse<T extends RawNodeResult> {
  private int requestCode;
  private Throwable exception;
  private T rawNodeResult;

  public NodeResponse(int requestCode, Throwable exception, T rawNodeResult) {
    this.requestCode = requestCode;
    this.exception = exception;
    this.rawNodeResult = rawNodeResult;
  }

  public int getRequestCode() {
    return requestCode;
  }

  public Throwable getException() {
    return exception;
  }

  public T getRawNodeResult() {
    return rawNodeResult;
  }
}
