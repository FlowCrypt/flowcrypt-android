/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.node;

import com.google.gson.annotations.Expose;

/**
 * It's a simple POJO which contains information about generated secret certs.
 */
final class NodeSecretCerts implements java.io.Serializable {
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
    nodeSecretCerts.ca = nodeSecret.getCa();
    nodeSecretCerts.crt = nodeSecret.getCrt();
    nodeSecretCerts.key = nodeSecret.getKey();
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
