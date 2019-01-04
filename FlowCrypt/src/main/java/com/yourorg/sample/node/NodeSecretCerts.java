package com.yourorg.sample.node;

public class NodeSecretCerts implements java.io.Serializable {

  private String ca;
  private String key;
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
