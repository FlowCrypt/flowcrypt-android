/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import org.json.JSONObject

/**
 * @author Denis Bondarenko
 *         Date: 7/12/19
 *         Time: 10:16 AM
 *         E-mail: DenBond7@gmail.com
 */
data class AttMeta(
  @Expose val name: String?,
  @Expose var data: ByteArray?,
  @Expose val length: Long,
  @Expose val type: String?,
  @Expose val contentId: String?,
  @Expose val url: String? = null
) : Parcelable {

  constructor(source: JSONObject) : this(
    name = if (source.has("name")) source.getString("name") else null,
    data = if (source.has("data")) source.getString("data").toByteArray() else null,
    length = if (source.has("size")) source.getLong("size") else 0L,
    type = if (source.has("type")) source.getString("type") else null,
    contentId = if (source.has("contentId")) source.getString("contentId") else null,
    url = if (source.has("url")) source.getString("url") else null
  )

  constructor(source: Parcel) : this(
    name = source.readString(),
    data = source.createByteArray(),
    length = source.readLong(),
    type = source.readString(),
    contentId = source.readString(),
    url = source.readString()
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeString(name)
    writeByteArray(data)
    writeLong(length)
    writeString(type)
    writeString(contentId)
    writeString(url)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AttMeta

    if (name != other.name) return false
    if (data != null) {
      if (other.data == null) return false
      if (!data.contentEquals(other.data)) return false
    } else if (other.data != null) return false
    if (length != other.length) return false
    if (type != other.type) return false
    if (contentId != other.contentId) return false
    if (url != other.url) return false

    return true
  }

  override fun hashCode(): Int {
    var result = name?.hashCode() ?: 0
    result = 31 * result + (data?.contentHashCode() ?: 0)
    result = 31 * result + length.hashCode()
    result = 31 * result + (type?.hashCode() ?: 0)
    result = 31 * result + (contentId?.hashCode() ?: 0)
    result = 31 * result + (url?.hashCode() ?: 0)
    return result
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<AttMeta> = object : Parcelable.Creator<AttMeta> {
      override fun createFromParcel(source: Parcel): AttMeta = AttMeta(source)
      override fun newArray(size: Int): Array<AttMeta?> = arrayOfNulls(size)
    }
  }
}
