/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.flowcrypt.email.api.retrofit.response.model.node.Error

/**
 * It's a base Node exception.
 *
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 10:08 AM
 * E-mail: DenBond7@gmail.com
 */
open class NodeException(val nodeError: Error?) : Exception() {
  override val message: String = {
    val builder = StringBuilder()
    if (nodeError != null) {
      builder.append("Error :").append(nodeError.msg).append("\n")
      builder.append("Stack :").append(nodeError.stack).append("\n")
      builder.append("Type :").append(nodeError.type)
    }
    builder
  }.toString()
}
