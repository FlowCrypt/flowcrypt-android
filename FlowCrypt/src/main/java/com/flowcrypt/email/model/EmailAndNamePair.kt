/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcel
import android.os.Parcelable

/**
 * This class describes a pair of email and name. This is a simple POJO object.
 *
 * @author DenBond7
 * Date: 22.05.2017
 * Time: 22:31
 * E-mail: DenBond7@gmail.com
 */

data class EmailAndNamePair constructor(val email: String? = null, val name: String? = null) : Parcelable {
  constructor(source: Parcel) : this(
      source.readString(),
      source.readString()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeString(email)
    writeString(name)
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<EmailAndNamePair> = object : Parcelable.Creator<EmailAndNamePair> {
      override fun createFromParcel(source: Parcel): EmailAndNamePair = EmailAndNamePair(source)
      override fun newArray(size: Int): Array<EmailAndNamePair?> = arrayOfNulls(size)
    }
  }
}
