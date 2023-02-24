/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

/**
 * This class is using with ACRA when we handle some exception **manually** and send logs to the server.
 *
 * @author Denys Bondarenko
 */
class ManualHandledException : FlowCryptException {
  constructor(message: String) : super(message)

  constructor(cause: Throwable) : super("Handled manually:", cause)

}
