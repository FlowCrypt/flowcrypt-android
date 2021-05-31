/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.flowcrypt.email.api.retrofit.response.base.ApiError

/**
 * It's a base Node exception.
 *
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 10:08 AM
 * E-mail: DenBond7@gmail.com
 */
open class NodeException(val nodeError: ApiError?) : Exception() {

  override val message: String = if (nodeError != null) {
    StringBuilder()
      .append("Error :").append(nodeError.msg).append("\n")
      .append("Stack :").append(nodeError.stack).append("\n")
      .append("Type :").append(nodeError.type).toString()
  } else ""
}
