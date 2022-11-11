/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.model

import android.os.Parcelable
import com.google.gson.annotations.Expose
import kotlinx.parcelize.Parcelize

/**
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 1:42 PM
 * E-mail: DenBond7@gmail.com
 */
@Parcelize
data class Algo constructor(
  @Expose val algorithm: String?,
  @Expose val algorithmId: Int,
  @Expose val bits: Int,
  @Expose val curve: String?
) : Parcelable
