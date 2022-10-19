/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * @author Denis Bondarenko
 * Date: 3/26/19
 * Time: 3:30 PM
 * E-mail: DenBond7@gmail.com
 */
data class DecryptError constructor(
  @SerializedName("error") @Expose val details: DecryptErrorDetails?,
  @Expose val fingerprints: List<String>?,
  @Expose val isEncrypted: Boolean
) : Parcelable {
  constructor(source: Parcel) : this(
    source.readParcelableViaExt<DecryptErrorDetails>(DecryptErrorDetails::class.java),
    source.createStringArrayList(),
    1 == source.readInt()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
    with(dest) {
      writeParcelable(details, flags)
      writeStringList(fingerprints)
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
