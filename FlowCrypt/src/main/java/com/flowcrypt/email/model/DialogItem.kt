/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcel
import android.os.Parcelable

/**
 * Simple POJO class which describes a dialog item.
 *
 * @author Denis Bondarenko
 * Date: 01.08.2017
 * Time: 11:29
 * E-mail: DenBond7@gmail.com
 */
data class DialogItem constructor(
  val iconResourceId: Int = 0,
  val title: String = "",
  val id: Int = 0
) : Parcelable {
  constructor(source: Parcel) : this(
    source.readInt(),
    source.readString()!!,
    source.readInt()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
    with(dest) {
      writeInt(iconResourceId)
      writeString(title)
      writeInt(id)
    }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<DialogItem> = object : Parcelable.Creator<DialogItem> {
      override fun createFromParcel(source: Parcel): DialogItem = DialogItem(source)
      override fun newArray(size: Int): Array<DialogItem?> = arrayOfNulls(size)
    }
  }
}
