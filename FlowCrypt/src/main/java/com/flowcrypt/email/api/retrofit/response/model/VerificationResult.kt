/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose

/**
 * @author Denis Bondarenko
 *         Date: 12/10/21
 *         Time: 10:09 AM
 *         E-mail: DenBond7@gmail.com
 */
data class VerificationResult(
  @Expose val hasEncryptedParts: Boolean,
  @Expose val hasSignedParts: Boolean,
  @Expose val hasMixedSignatures: Boolean,
  @Expose val isPartialSigned: Boolean,
  @Expose val keyIdOfSigningKeys: List<Long>,
  @Expose val hasBadSignatures: Boolean,
) : Parcelable {

  val hasUnverifiedSignatures: Boolean = keyIdOfSigningKeys.isNotEmpty()

  constructor(parcel: Parcel) : this(
    parcel.readByte() != 0.toByte(),
    parcel.readByte() != 0.toByte(),
    parcel.readByte() != 0.toByte(),
    parcel.readByte() != 0.toByte(),
    mutableListOf<Long>().apply {
      parcel.readList(
        this as List<*>,
        Long::class.java.classLoader
      )
    },
    parcel.readByte() != 0.toByte()
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeByte(if (hasEncryptedParts) 1 else 0)
    parcel.writeByte(if (hasSignedParts) 1 else 0)
    parcel.writeByte(if (hasMixedSignatures) 1 else 0)
    parcel.writeByte(if (isPartialSigned) 1 else 0)
    parcel.writeList(keyIdOfSigningKeys)
    parcel.writeByte(if (hasBadSignatures) 1 else 0)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<VerificationResult> {
    override fun createFromParcel(parcel: Parcel) = VerificationResult(parcel)
    override fun newArray(size: Int): Array<VerificationResult?> = arrayOfNulls(size)
  }
}
