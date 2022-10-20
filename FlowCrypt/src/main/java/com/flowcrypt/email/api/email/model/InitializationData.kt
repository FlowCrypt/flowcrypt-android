/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcel
import android.os.Parcelable

data class InitializationData(
  var subject: String? = null,
  var body: String? = null,
  val toAddresses: java.util.ArrayList<String> = arrayListOf(),
  val ccAddresses: java.util.ArrayList<String> = arrayListOf(),
  val bccAddresses: java.util.ArrayList<String> = arrayListOf()
) : Parcelable {
  constructor(parcel: Parcel) : this(
    parcel.readString(),
    parcel.readString(),
    parcel.createStringArrayList() ?: arrayListOf(),
    parcel.createStringArrayList() ?: arrayListOf(),
    parcel.createStringArrayList() ?: arrayListOf()
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeString(subject)
    parcel.writeString(body)
    parcel.writeStringList(toAddresses)
    parcel.writeStringList(ccAddresses)
    parcel.writeStringList(bccAddresses)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<InitializationData> {
    override fun createFromParcel(parcel: Parcel): InitializationData = InitializationData(parcel)
    override fun newArray(size: Int): Array<InitializationData?> = arrayOfNulls(size)
  }
}
