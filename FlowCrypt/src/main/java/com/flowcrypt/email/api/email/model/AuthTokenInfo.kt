/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * @author Denys Bondarenko
 */
@Parcelize
data class AuthTokenInfo constructor(
  val email: String?,
  val accessToken: String? = null,
  val expiresAt: Long? = null,
  val refreshToken: String? = null
) : Parcelable
