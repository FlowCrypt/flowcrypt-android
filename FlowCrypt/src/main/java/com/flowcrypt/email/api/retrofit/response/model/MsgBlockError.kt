/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose

/**
 * @author Denis Bondarenko
 *         Date: 10/1/21
 *         Time: 10:27 AM
 *         E-mail: DenBond7@gmail.com
 */
data class MsgBlockError(
  @Expose val errorMsg: String? = null
) : Parcelable {
  constructor(parcel: Parcel) : this(parcel.readString())

  override fun writeToParcel(parcel: Parcel, flags: Int) = parcel.writeString(errorMsg)

  override fun describeContents(): Int = 0

  companion object CREATOR : Parcelable.Creator<MsgBlockError> {
    override fun createFromParcel(parcel: Parcel): MsgBlockError = MsgBlockError(parcel)
    override fun newArray(size: Int): Array<MsgBlockError?> = arrayOfNulls(size)
  }
}
