/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import org.apache.commons.io.IOUtils
import java.io.BufferedInputStream

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
                                           @SerializedName("error")
                                           @Expose override val apiError: ApiError?,
                                           var decryptedBytes: ByteArray? = null) : BaseNodeResponse {
  override fun handleRawData(bufferedInputStream: BufferedInputStream) {
    decryptedBytes = IOUtils.toByteArray(bufferedInputStream)
  }

  constructor(source: Parcel) : this(
      1 == source.readInt(),
      source.readString(),
      source.readParcelable<ApiError>(ApiError::class.java.classLoader),
      source.createByteArray()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeInt((if (isSuccess) 1 else 0))
        writeString(name)
        writeParcelable(apiError, flags)
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
