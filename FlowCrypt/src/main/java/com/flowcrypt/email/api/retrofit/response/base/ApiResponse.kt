/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.base

import android.os.Parcelable

/**
 * An interface for identification API response
 *
 * @author Denis Bondarenko
 * Date: 11.10.2016
 * Time: 16:41
 * E-mail: DenBond7@gmail.com
 */
interface ApiResponse : Parcelable {
  val apiError: ApiError?
}
