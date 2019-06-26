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

/**
 * It's a result for "encryptFile" requests.
 *
 * @author Denis Bondarenko
 * Date: 1/15/19
 * Time: 8:59 AM
 * E-mail: DenBond7@gmail.com
 */
data class EncryptedFileResult constructor(@Expose override val error: Error?,
                                           var encryptedBytes: ByteArray? = null) : BaseNodeResponse {

  override fun handleRawData(bufferedInputStream: BufferedInputStream) {
    encryptedBytes = IOUtils.toByteArray(bufferedInputStream)
  }

  constructor(source: Parcel) : this(
      source.readParcelable<Error>(Error::class.java.classLoader),
      source.createByteArray()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(error, 0)
        writeByteArray(encryptedBytes)
      }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as EncryptedFileResult

    if (error != other.error) return false
    if (encryptedBytes != null) {
      if (other.encryptedBytes == null) return false
      if (!encryptedBytes!!.contentEquals(other.encryptedBytes!!)) return false
    } else if (other.encryptedBytes != null) return false

    return true
  }

  override fun hashCode(): Int {
    var result = error?.hashCode() ?: 0
    result = 31 * result + (encryptedBytes?.contentHashCode() ?: 0)
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
