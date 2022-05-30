/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions

import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.base.Result

/**
 * @author Denis Bondarenko
 *         Date: 6/15/21
 *         Time: 3:46 PM
 *         E-mail: DenBond7@gmail.com
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
      val apiResponse = data as? ApiResponse
      apiResponse?.apiError?.let { apiError ->
        stringBuilder.append(apiError.msg)
      }
    }

    return if (stringBuilder.isEmpty()) "Unknown error" else stringBuilder.toString()
  }
