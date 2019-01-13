package com.yourorg.sample.api.retrofit.request;

import com.flowcrypt.email.api.retrofit.request.node.BaseNodeRequest;

/**
 * @author DenBond7
 */
public class NodeRequestWrapper<T extends BaseNodeRequest> {
  private int requestCode;
  private T baseNodeRequest;

  public NodeRequestWrapper(int requestCode, T baseNodeRequest) {
    this.requestCode = requestCode;
    this.baseNodeRequest = baseNodeRequest;
  }

  public int getRequestCode() {
    return requestCode;
  }

  public T getRequest() {
    return baseNodeRequest;
  }
}
