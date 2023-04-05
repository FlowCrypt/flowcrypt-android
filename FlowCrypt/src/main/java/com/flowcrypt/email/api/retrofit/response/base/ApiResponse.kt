/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.base

import android.os.Parcelable

/**
 * @author Denys Bondarenko
 */
interface ApiResponse : Parcelable {
  val code: Int?
  val message: String?
  val details: String?
  val apiError: ApiError?
    get() = code?.let { ApiError(code = it, message = message, details = details) }
}
