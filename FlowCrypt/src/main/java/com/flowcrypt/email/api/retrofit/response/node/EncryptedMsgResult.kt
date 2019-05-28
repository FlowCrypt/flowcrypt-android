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
import java.nio.charset.StandardCharsets

/**
 * It's a result for "encryptMsg" requests.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 12:51 PM
 * E-mail: DenBond7@gmail.com
 */
data class EncryptedMsgResult constructor(@Expose override val error: Error?,
                                          var encryptedMsg: String? = null) : BaseNodeResponse {
  @Throws(IOException::class)
  override fun handleRawData(bufferedInputStream: BufferedInputStream) {
    val bytes = IOUtils.toByteArray(bufferedInputStream) ?: return

    try {
      encryptedMsg = IOUtils.toString(bytes, StandardCharsets.UTF_8.displayName())
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }

  constructor(source: Parcel) : this(
      source.readParcelable<Error>(Error::class.java.classLoader),
      source.readString()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(error, 0)
        writeString(encryptedMsg)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<EncryptedMsgResult> = object : Parcelable.Creator<EncryptedMsgResult> {
      override fun createFromParcel(source: Parcel): EncryptedMsgResult = EncryptedMsgResult(source)
      override fun newArray(size: Int): Array<EncryptedMsgResult?> = arrayOfNulls(size)
    }
  }
}
