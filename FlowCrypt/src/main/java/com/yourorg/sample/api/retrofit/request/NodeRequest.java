package com.yourorg.sample.api.retrofit.request;

import com.yourorg.sample.api.retrofit.NodeRequestBody;

/**
 * @author DenBond7
 */
public class NodeRequest {
  private int requestCode;
  private NodeRequestBody nodeRequestBody;

  public NodeRequest(int requestCode, NodeRequestBody nodeRequestBody) {
    this.requestCode = requestCode;
    this.nodeRequestBody = nodeRequestBody;
  }

  public int getRequestCode() {
    return requestCode;
  }

  public NodeRequestBody getNodeRequestBody() {
    return nodeRequestBody;
  }
}
