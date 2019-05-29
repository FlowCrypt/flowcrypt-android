/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.node

import com.google.gson.annotations.Expose

/**
 * It's a simple POJO which contains information about generated secret certs.
 */
class NodeSecretCerts private constructor() : java.io.Serializable {
  @Expose
  var ca: String? = null
    private set

  @Expose
  var key: String? = null
    private set

  @Expose
  var crt: String? = null
    private set

  companion object {
    fun fromNodeSecret(nodeSecret: NodeSecret): NodeSecretCerts {
      val nodeSecretCerts = NodeSecretCerts()
      nodeSecretCerts.ca = nodeSecret.ca
      nodeSecretCerts.crt = nodeSecret.crt
      nodeSecretCerts.key = nodeSecret.key
      return nodeSecretCerts
    }
  }
}
