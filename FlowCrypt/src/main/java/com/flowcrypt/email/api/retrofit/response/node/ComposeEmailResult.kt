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
 * It's a result for "composeEmail" requests.
 *
 * @author Denis Bondarenko
 * Date: 3/27/19
 * Time: 3:00 PM
 * E-mail: DenBond7@gmail.com
 */
data class ComposeEmailResult constructor(@Expose override val error: Error?,
                                          var mimeMsg: String = "") : BaseNodeResponse {
  @Throws(IOException::class)
  override fun handleRawData(bufferedInputStream: BufferedInputStream) {
    val bytes = IOUtils.toByteArray(bufferedInputStream) ?: return

    try {
      mimeMsg = IOUtils.toString(bytes, StandardCharsets.UTF_8.displayName())
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }

  constructor(source: Parcel) : this(
      source.readParcelable<Error>(Error::class.java.classLoader),
      source.readString()!!
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(error, 0)
        writeString(mimeMsg)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<ComposeEmailResult> = object : Parcelable.Creator<ComposeEmailResult> {
      override fun createFromParcel(source: Parcel): ComposeEmailResult = ComposeEmailResult(source)
      override fun newArray(size: Int): Array<ComposeEmailResult?> = arrayOfNulls(size)
    }
  }
}
