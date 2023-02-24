/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.base

import android.os.Parcelable

/**
 * An interface for identification API response
 *
 * @author Denys Bondarenko
 */
interface ApiResponse : Parcelable {
  val apiError: ApiError?
}
