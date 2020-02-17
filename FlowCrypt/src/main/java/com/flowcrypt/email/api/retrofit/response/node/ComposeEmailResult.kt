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
data class ComposeEmailResult constructor(@SerializedName("error")
                                          @Expose override val apiError: ApiError?,
                                          var mimeMsg: String = "") : BaseNodeResponse, Parcelable {
  override fun handleRawData(bufferedInputStream: BufferedInputStream) {
    val bytes = IOUtils.toByteArray(bufferedInputStream) ?: return

    try {
      mimeMsg = IOUtils.toString(bytes, StandardCharsets.UTF_8.displayName())
    } catch (e: IOException) {
      e.printStackTrace()
    }
  }

  constructor(source: Parcel) : this(
      source.readParcelable<ApiError>(ApiError::class.java.classLoader),
      source.readString()!!
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeParcelable(apiError, flags)
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
