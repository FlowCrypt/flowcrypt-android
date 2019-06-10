/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcel
import android.os.Parcelable

/**
 * The message types.
 *
 * @author Denis Bondarenko
 * Date: 20.03.2018
 * Time: 12:55
 * E-mail: DenBond7@gmail.com
 */

enum class MessageType : Parcelable {
  NEW, REPLY, REPLY_ALL, FORWARD;

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeInt(ordinal)
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<MessageType> = object : Parcelable.Creator<MessageType> {
      override fun createFromParcel(source: Parcel): MessageType = values()[source.readInt()]
      override fun newArray(size: Int): Array<MessageType?> = arrayOfNulls(size)
    }
  }
}
