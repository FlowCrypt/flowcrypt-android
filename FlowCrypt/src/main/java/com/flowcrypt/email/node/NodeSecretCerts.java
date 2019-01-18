package com.flowcrypt.email.node;

import com.google.gson.annotations.Expose;

/**
 * It's a simple POJO which contains information about generated secret certs.
 */
class NodeSecretCerts implements java.io.Serializable {
  @Expose
  private String ca;

  @Expose
  private String key;

  @Expose
  private String crt;

  private NodeSecretCerts() {
  }

  static NodeSecretCerts fromNodeSecret(NodeSecret nodeSecret) {
    NodeSecretCerts nodeSecretCerts = new NodeSecretCerts();
    nodeSecretCerts.ca = nodeSecret.ca;
    nodeSecretCerts.crt = nodeSecret.crt;
    nodeSecretCerts.key = nodeSecret.key;
    return nodeSecretCerts;
  }

  String getCa() {
    return ca;
  }

  String getKey() {
    return key;
  }

  String getCrt() {
    return crt;
  }
}
