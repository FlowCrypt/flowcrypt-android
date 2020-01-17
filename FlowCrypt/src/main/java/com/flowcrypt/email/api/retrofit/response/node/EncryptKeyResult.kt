/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.response.model.node.Error
import com.google.gson.annotations.Expose
import java.io.BufferedInputStream

/**
 * It's a result for "encryptKey" requests.
 *
 * @author Denis Bondarenko
 * Date: 2/18/19
 * Time: 9:28 AM
 * E-mail: DenBond7@gmail.com
 */
data class EncryptKeyResult constructor(@Expose val encryptedKey: String?,
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
        writeString(encryptedKey)
        writeParcelable(error, 0)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<EncryptKeyResult> = object : Parcelable.Creator<EncryptKeyResult> {
      override fun createFromParcel(source: Parcel): EncryptKeyResult = EncryptKeyResult(source)
      override fun newArray(size: Int): Array<EncryptKeyResult?> = arrayOfNulls(size)
    }
  }
}
