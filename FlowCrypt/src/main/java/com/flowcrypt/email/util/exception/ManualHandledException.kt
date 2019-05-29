/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

/**
 * This class is using with ACRA when we handle some exception **manually** and send logs to the server.
 *
 * @author Denis Bondarenko
 * Date: 23.01.2018
 * Time: 12:17
 * E-mail: DenBond7@gmail.com
 */

class ManualHandledException : FlowCryptException {
  constructor(message: String) : super(message)

  constructor(cause: Throwable) : super("Handled manually:", cause)

}
