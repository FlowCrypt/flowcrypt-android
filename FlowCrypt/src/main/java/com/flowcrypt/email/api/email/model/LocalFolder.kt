/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcel
import android.os.Parcelable

/**
 * This is a simple POJO object, which describe information about the email folder.
 *
 * @author DenBond7
 * Date: 07.06.2017
 * Time: 14:49
 * E-mail: DenBond7@gmail.com
 */

data class LocalFolder constructor(val fullName: String,
                                   var folderAlias: String? = null,
                                   val attributes: List<String>? = null,
                                   val isCustom: Boolean = false,
                                   var msgCount: Int = 0,
                                   var searchQuery: String? = null) : Parcelable {

  constructor(source: Parcel) : this(
      source.readString()!!,
      source.readString(),
      source.createStringArrayList(),
      source.readInt() == 1,
      source.readInt(),
      source.readString()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeString(fullName)
        writeString(folderAlias)
        writeStringList(attributes)
        writeInt((if (isCustom) 1 else 0))
        writeInt(msgCount)
        writeString(searchQuery)
      }

  companion object {
    @JvmField
    @Suppress("unused")
    val CREATOR: Parcelable.Creator<LocalFolder> = object : Parcelable.Creator<LocalFolder> {
      override fun createFromParcel(source: Parcel): LocalFolder = LocalFolder(source)
      override fun newArray(size: Int): Array<LocalFolder?> = arrayOfNulls(size)
    }
  }
}
