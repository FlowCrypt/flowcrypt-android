/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.request.node;

/**
 * @author DenBond7
 */
public class NodeRequestWrapper<T extends NodeRequest> {
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
