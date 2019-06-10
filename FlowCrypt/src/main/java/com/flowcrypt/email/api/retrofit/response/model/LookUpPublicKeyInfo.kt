/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * This POJO class describes information about a public key from the
 * API ` https://flowcrypt.com/attester/lookup/`
 *
 * @author Denis Bondarenko
 * Date: 05.05.2018
 * Time: 14:04
 * E-mail: DenBond7@gmail.com
 */
data class LookUpPublicKeyInfo constructor(@SerializedName("longid") @Expose val longId: String?,
                                           @SerializedName("pubkey") @Expose val pubKey: String?,
                                           @Expose val query: String?,
                                           @Expose val attests: ArrayList<String>?) : Parcelable {
  constructor(source: Parcel) : this(
      source.readString(),
      source.readString(),
      source.readString(),
      source.createStringArrayList()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeString(longId)
        writeString(pubKey)
        writeString(query)
        writeStringList(attests)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<LookUpPublicKeyInfo> = object : Parcelable.Creator<LookUpPublicKeyInfo> {
      override fun createFromParcel(source: Parcel): LookUpPublicKeyInfo = LookUpPublicKeyInfo(source)
      override fun newArray(size: Int): Array<LookUpPublicKeyInfo?> = arrayOfNulls(size)
    }
  }
}
