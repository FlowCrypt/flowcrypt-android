/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import com.flowcrypt.email.api.retrofit.response.base.Result

/**
 * @author Denys Bondarenko
 */
val <T> Result<T>.exceptionMsg: String
  get() {
    val stringBuilder = StringBuilder()

    exception?.let {
      stringBuilder.append(it.javaClass.simpleName)
      stringBuilder.append(":")
      stringBuilder.append(it.message)
    }

    exception?.cause?.let {
      if (stringBuilder.isNotEmpty()) {
        stringBuilder.append("\n\n")
      }
      stringBuilder.append(it.javaClass.simpleName)
      stringBuilder.append(":")
      stringBuilder.append(it.message)
    }

    if (stringBuilder.isEmpty()) {
      apiError?.let { stringBuilder.append(it.message) }
    }

    return stringBuilder.toString().ifEmpty { "Unknown error" }
  }

val <T> Result<T>.exceptionMsgWithStack: String
  get() {
    val stringBuilder = StringBuilder()
    stringBuilder.append(exceptionMsg)
    exception?.stackTrace?.firstOrNull()?.let { stringBuilder.append("\n" + it) }
    return stringBuilder.toString()
  }
