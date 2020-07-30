/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

/**
 * The base exception class.
 *
 * @author Denis Bondarenko
 * Date: 23.01.2018
 * Time: 12:16
 * E-mail: DenBond7@gmail.com
 */
abstract class FlowCryptException : Exception {
  constructor()

  constructor(message: String) : super(message)

  constructor(message: String, cause: Throwable) : super(message, cause)

  constructor(cause: Throwable) : super(cause)

  constructor(message: String, cause: Throwable, enableSuppression: Boolean, writableStackTrace: Boolean)
      : super(message, cause, enableSuppression, writableStackTrace)
}
