/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcel
import android.os.Parcelable

/**
 * The message encryption type.
 *
 * @author Denis Bondarenko
 * Date: 28.07.2017
 * Time: 15:41
 * E-mail: DenBond7@gmail.com
 */

enum class MessageEncryptionType : Parcelable {
  ENCRYPTED, STANDARD;

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeInt(ordinal)
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<MessageEncryptionType> = object : Parcelable.Creator<MessageEncryptionType> {
      override fun createFromParcel(source: Parcel): MessageEncryptionType = values()[source.readInt()]
      override fun newArray(size: Int): Array<MessageEncryptionType?> = arrayOfNulls(size)
    }
  }
}
