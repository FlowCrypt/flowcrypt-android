/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.response.model.node.Error
import com.google.gson.annotations.Expose
import java.io.BufferedInputStream

/**
 * It's a result for "decryptKey" requests.
 *
 * @author Denis Bondarenko
 * Date: 2/12/19
 * Time: 4:40 PM
 * E-mail: DenBond7@gmail.com
 */
data class DecryptKeyResult constructor(@Expose val decryptedKey: String?,
                                        @Expose override val error: Error?) : BaseNodeResponse {
  override fun handleRawData(bufferedInputStream: BufferedInputStream) {

  }

  constructor(source: Parcel) : this(
      source.readString(),
      source.readParcelable<Error>(Error::class.java.classLoader)
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeString(decryptedKey)
        writeParcelable(error, 0)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<DecryptKeyResult> = object : Parcelable.Creator<DecryptKeyResult> {
      override fun createFromParcel(source: Parcel): DecryptKeyResult = DecryptKeyResult(source)
      override fun newArray(size: Int): Array<DecryptKeyResult?> = arrayOfNulls(size)
    }
  }
}
