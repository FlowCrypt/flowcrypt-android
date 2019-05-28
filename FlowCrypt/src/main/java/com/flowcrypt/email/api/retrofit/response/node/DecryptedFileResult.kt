/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node

import android.os.Parcel
import android.os.Parcelable

import com.flowcrypt.email.api.retrofit.response.model.node.Error
import com.google.gson.annotations.Expose

import org.apache.commons.io.IOUtils

import java.io.BufferedInputStream
import java.io.IOException

/**
 * It's a result for "decryptFile" requests.
 *
 * @author Denis Bondarenko
 * Date: 1/15/19
 * Time: 4:37 PM
 * E-mail: DenBond7@gmail.com
 */
data class DecryptedFileResult constructor(@Expose val isSuccess: Boolean,
                                           @Expose val name: String?,
                                           @Expose override val error: Error?,
                                           var decryptedBytes: ByteArray? = null) : BaseNodeResponse {
  @Throws(IOException::class)
  override fun handleRawData(bufferedInputStream: BufferedInputStream) {
    decryptedBytes = IOUtils.toByteArray(bufferedInputStream)
  }

  constructor(source: Parcel) : this(
      1 == source.readInt(),
      source.readString(),
      source.readParcelable<Error>(Error::class.java.classLoader),
      source.createByteArray()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeInt((if (isSuccess) 1 else 0))
        writeString(name)
        writeParcelable(error, 0)
        writeByteArray(decryptedBytes)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<DecryptedFileResult> = object : Parcelable.Creator<DecryptedFileResult> {
      override fun createFromParcel(source: Parcel): DecryptedFileResult = DecryptedFileResult(source)
      override fun newArray(size: Int): Array<DecryptedFileResult?> = arrayOfNulls(size)
    }
  }
}
