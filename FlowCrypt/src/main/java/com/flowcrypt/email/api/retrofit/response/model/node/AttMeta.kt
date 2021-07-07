/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   DenBond7
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.api.retrofit.response.model.node

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
  @Expose var data: String?,
  @Expose val length: Long,
  @Expose val type: String?,
  @Expose val contentId: String?,
  @Expose val url: String? = null
) : Parcelable {

  constructor(source: JSONObject) : this(
    name = if (source.has("name")) source.getString("name") else null,
    data = if (source.has("data")) source.getString("data") else null,
    length = if (source.has("size")) source.getLong("size") else 0L,
    type = if (source.has("type")) source.getString("type") else null,
    contentId = if (source.has("contentId")) source.getString("contentId") else null,
    url = if (source.has("url")) source.getString("url") else null
  )

  constructor(source: Parcel) : this(
    name = source.readString(),
    data =source.readString(),
    length = source.readLong(),
    type = source.readString(),
    contentId = source.readString(),
    url = source.readString()
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeString(name)
    writeString(data)
    writeLong(length)
    writeString(type)
    writeString(contentId)
    writeString(url)
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<AttMeta> = object : Parcelable.Creator<AttMeta> {
      override fun createFromParcel(source: Parcel): AttMeta = AttMeta(source)
      override fun newArray(size: Int): Array<AttMeta?> = arrayOfNulls(size)
    }
  }
}
