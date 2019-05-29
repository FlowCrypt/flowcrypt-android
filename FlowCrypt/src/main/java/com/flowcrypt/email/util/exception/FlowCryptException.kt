/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import android.os.Build

import androidx.annotation.RequiresApi

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

  @RequiresApi(api = Build.VERSION_CODES.N)
  constructor(message: String, cause: Throwable, enableSuppression: Boolean, writableStackTrace: Boolean) : super(message, cause, enableSuppression, writableStackTrace)
}
