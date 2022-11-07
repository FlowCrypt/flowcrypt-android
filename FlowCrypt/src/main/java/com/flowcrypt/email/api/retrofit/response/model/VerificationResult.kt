/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcelable
import com.google.gson.annotations.Expose
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * @author Denis Bondarenko
 *         Date: 12/10/21
 *         Time: 10:09 AM
 *         E-mail: DenBond7@gmail.com
 */
@Parcelize
data class VerificationResult(
  @Expose val hasEncryptedParts: Boolean,
  @Expose val hasSignedParts: Boolean,
  @Expose val hasMixedSignatures: Boolean,
  @Expose val isPartialSigned: Boolean,
  @Expose val keyIdOfSigningKeys: List<Long>,
  @Expose val hasBadSignatures: Boolean,
) : Parcelable {
  @IgnoredOnParcel
  val hasUnverifiedSignatures: Boolean = keyIdOfSigningKeys.isNotEmpty()
}
