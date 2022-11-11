/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcelable
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.google.gson.annotations.Expose
import kotlinx.parcelize.Parcelize

/**
 * @author Denis Bondarenko
 * Date: 3/26/19
 * Time: 3:30 PM
 * E-mail: DenBond7@gmail.com
 */
@Parcelize
data class DecryptErrorDetails(
  @Expose val type: PgpDecryptAndOrVerify.DecryptionErrorType?,
  @Expose val message: String?
) : Parcelable
