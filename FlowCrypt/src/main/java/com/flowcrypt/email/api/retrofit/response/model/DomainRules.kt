/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose

/**
 * @author Denis Bondarenko
 *         Date: 10/29/19
 *         Time: 11:30 AM
 *         E-mail: DenBond7@gmail.com
 */
data class DomainRules constructor(@Expose val flags: List<String>?) : Parcelable {
  constructor(parcel: Parcel) : this(parcel.createStringArrayList())

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeStringList(this.flags)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<DomainRules> {
    override fun createFromParcel(parcel: Parcel): DomainRules {
      return DomainRules(parcel)
    }

    override fun newArray(size: Int): Array<DomainRules?> {
      return arrayOfNulls(size)
    }
  }
}