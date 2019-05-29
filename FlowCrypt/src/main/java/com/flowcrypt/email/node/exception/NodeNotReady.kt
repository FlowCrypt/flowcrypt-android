/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.node.exception

/**
 * @author DenBond7
 */
class NodeNotReady : Exception {
  constructor(message: String, cause: Throwable) : super(message, cause)

  constructor(message: String) : super(message)
}
