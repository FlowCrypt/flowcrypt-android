/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify
import com.google.gson.annotations.Expose

/**
 * @author Denis Bondarenko
 * Date: 3/26/19
 * Time: 3:30 PM
 * E-mail: DenBond7@gmail.com
 */
data class DecryptErrorDetails(
  @Expose val type: PgpDecryptAndOrVerify.DecryptionErrorType?,
  @Expose val message: String?
) : Parcelable {
  constructor(source: Parcel) : this(
    source.readParcelableViaExt(PgpDecryptAndOrVerify.DecryptionErrorType::class.java),
    source.readString()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeParcelable(type, flags)
    writeString(message)
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<DecryptErrorDetails> =
      object : Parcelable.Creator<DecryptErrorDetails> {
        override fun createFromParcel(source: Parcel): DecryptErrorDetails =
          DecryptErrorDetails(source)

        override fun newArray(size: Int): Array<DecryptErrorDetails?> = arrayOfNulls(size)
      }
  }
}
