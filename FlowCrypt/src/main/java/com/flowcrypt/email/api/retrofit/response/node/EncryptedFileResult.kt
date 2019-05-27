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
 * It's a result for "encryptFile" requests.
 *
 * @author Denis Bondarenko
 * Date: 1/15/19
 * Time: 8:59 AM
 * E-mail: DenBond7@gmail.com
 */
data class EncryptedFileResult constructor(@Expose override val error: Error?) : BaseNodeResponse {
  var encryptedBytes: ByteArray? = null
    private set

  @Throws(IOException::class)
  override fun handleRawData(bufferedInputStream: BufferedInputStream) {
    encryptedBytes = IOUtils.toByteArray(bufferedInputStream)
  }

  constructor(source: Parcel) : this(
      source.readParcelable<Error>(Error::class.java.classLoader)
  ) {
    source.readByteArray(encryptedBytes)
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(error, 0)
        writeByteArray(encryptedBytes)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<EncryptedFileResult> = object : Parcelable.Creator<EncryptedFileResult> {
      override fun createFromParcel(source: Parcel): EncryptedFileResult = EncryptedFileResult(source)
      override fun newArray(size: Int): Array<EncryptedFileResult?> = arrayOfNulls(size)
    }
  }
}
