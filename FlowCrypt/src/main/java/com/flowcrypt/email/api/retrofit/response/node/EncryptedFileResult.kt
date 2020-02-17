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
 * It's a result for "encryptFile" requests.
 *
 * @author Denis Bondarenko
 * Date: 1/15/19
 * Time: 8:59 AM
 * E-mail: DenBond7@gmail.com
 */
data class EncryptedFileResult constructor(@SerializedName("error")
                                           @Expose override val apiError: ApiError?,
                                           var encryptBytes: ByteArray? = null) : BaseNodeResponse {

  override fun handleRawData(bufferedInputStream: BufferedInputStream) {
    encryptBytes = IOUtils.toByteArray(bufferedInputStream)
  }

  constructor(source: Parcel) : this(
      source.readParcelable<ApiError>(ApiError::class.java.classLoader),
      source.createByteArray()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(apiError, flags)
        writeByteArray(encryptBytes)
      }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as EncryptedFileResult

    if (apiError != other.apiError) return false
    if (encryptBytes != null) {
      if (other.encryptBytes == null) return false
      if (!encryptBytes!!.contentEquals(other.encryptBytes!!)) return false
    } else if (other.encryptBytes != null) return false

    return true
  }

  override fun hashCode(): Int {
    var result = apiError?.hashCode() ?: 0
    result = 31 * result + (encryptBytes?.contentHashCode() ?: 0)
    return result
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<EncryptedFileResult> = object : Parcelable.Creator<EncryptedFileResult> {
      override fun createFromParcel(source: Parcel): EncryptedFileResult = EncryptedFileResult(source)
      override fun newArray(size: Int): Array<EncryptedFileResult?> = arrayOfNulls(size)
    }
  }
}
