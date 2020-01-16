/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.response.model.node.Error
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.BufferedInputStream

/**
 * It's a result for "version" requests.
 *
 * @author Denis Bondarenko
 * Date: 1/11/19
 * Time: 11:42 AM
 * E-mail: DenBond7@gmail.com
 */
data class VersionResult constructor(@SerializedName("http_parser") @Expose val httpParser: String?,
                                     @Expose val mobile: String?,
                                     @Expose val node: String?,
                                     @Expose val v8: String?,
                                     @Expose val uv: String?,
                                     @Expose val zlib: String?,
                                     @Expose val ares: String?,
                                     @Expose val modules: String?,
                                     @Expose val nghttp2: String?,
                                     @Expose val openssl: String?,
                                     @Expose override val error: Error?) : BaseNodeResponse {
  override fun handleRawData(bufferedInputStream: BufferedInputStream) {

  }

  constructor(source: Parcel) : this(
      source.readString(),
      source.readString(),
      source.readString(),
      source.readString(),
      source.readString(),
      source.readString(),
      source.readString(),
      source.readString(),
      source.readString(),
      source.readString(),
      source.readParcelable<Error>(Error::class.java.classLoader)
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeString(httpParser)
        writeString(mobile)
        writeString(node)
        writeString(v8)
        writeString(uv)
        writeString(zlib)
        writeString(ares)
        writeString(modules)
        writeString(nghttp2)
        writeString(openssl)
        writeParcelable(error, 0)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<VersionResult> = object : Parcelable.Creator<VersionResult> {
      override fun createFromParcel(source: Parcel): VersionResult = VersionResult(source)
      override fun newArray(size: Int): Array<VersionResult?> = arrayOfNulls(size)
    }
  }
}
