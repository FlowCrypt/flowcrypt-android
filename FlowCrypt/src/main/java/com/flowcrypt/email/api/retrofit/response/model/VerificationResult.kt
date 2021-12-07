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
class VerificationResult(
  @Expose val isEncrypted: Boolean,
  @Expose val isSigned: Boolean,
  @Expose val hasMixedSignatures: Boolean,
  @Expose val isPartialSigned: Boolean,
  @Expose val hasUnverifiedSignatures: Boolean,
) : Parcelable {
  constructor(parcel: Parcel) : this(
    parcel.readByte() != 0.toByte(),
    parcel.readByte() != 0.toByte(),
    parcel.readByte() != 0.toByte(),
    parcel.readByte() != 0.toByte(),
    parcel.readByte() != 0.toByte()
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeByte(if (isEncrypted) 1 else 0)
    parcel.writeByte(if (isSigned) 1 else 0)
    parcel.writeByte(if (hasMixedSignatures) 1 else 0)
    parcel.writeByte(if (isPartialSigned) 1 else 0)
    parcel.writeByte(if (hasUnverifiedSignatures) 1 else 0)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<VerificationResult> {
    override fun createFromParcel(parcel: Parcel) = VerificationResult(parcel)
    override fun newArray(size: Int): Array<VerificationResult?> = arrayOfNulls(size)
  }
}
