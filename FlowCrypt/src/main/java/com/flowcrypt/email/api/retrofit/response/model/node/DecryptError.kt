/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * @author Denis Bondarenko
 * Date: 3/26/19
 * Time: 3:30 PM
 * E-mail: DenBond7@gmail.com
 */
data class DecryptError constructor(@Expose val isSuccess: Boolean,
                                    @SerializedName("error") @Expose val details: DecryptErrorDetails?,
                                    @SerializedName("longids") @Expose val longids: Longids?,
                                    @Expose val isEncrypted: Boolean) : Parcelable {
  constructor(source: Parcel) : this(
      1 == source.readInt(),
      source.readParcelable<DecryptErrorDetails>(DecryptErrorDetails::class.java.classLoader),
      source.readParcelable<Longids>(Longids::class.java.classLoader),
      1 == source.readInt()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeInt((if (isSuccess) 1 else 0))
        writeParcelable(details, flags)
        writeParcelable(longids, flags)
        writeInt((if (isEncrypted) 1 else 0))
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<DecryptError> = object : Parcelable.Creator<DecryptError> {
      override fun createFromParcel(source: Parcel): DecryptError = DecryptError(source)
      override fun newArray(size: Int): Array<DecryptError?> = arrayOfNulls(size)
    }
  }
}
