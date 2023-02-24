/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

/**
 * The base exception class.
 *
 * @author Denys Bondarenko
 */
abstract class FlowCryptException : Exception {
  constructor()

  constructor(message: String) : super(message)

  constructor(message: String, cause: Throwable) : super(message, cause)

  constructor(cause: Throwable) : super(cause)

  constructor(
    message: String,
    cause: Throwable,
    enableSuppression: Boolean,
    writableStackTrace: Boolean
  )
      : super(message, cause, enableSuppression, writableStackTrace)
}
