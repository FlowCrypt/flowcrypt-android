/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcel
import android.os.Parcelable

import com.flowcrypt.email.ui.loader.UpdateInfoAboutPgpContactsAsyncTaskLoader

/**
 * The simple POJO class which describes information about result from the
 * [UpdateInfoAboutPgpContactsAsyncTaskLoader]
 *
 * @author Denis Bondarenko
 * Date: 31.07.2017
 * Time: 17:31
 * E-mail: DenBond7@gmail.com
 */

data class UpdateInfoAboutPgpContactsResult constructor(val emails: List<String>,
                                                        val isAllInfoReceived: Boolean = false,
                                                        val updatedPgpContacts: List<PgpContact>) : Parcelable {
  constructor(source: Parcel) : this(
      source.createStringArrayList()!!,
      source.readInt() == 1,
      source.createTypedArrayList(PgpContact.CREATOR)!!
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeStringList(emails)
    writeInt((if (isAllInfoReceived) 1 else 0))
    writeTypedList(updatedPgpContacts)
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<UpdateInfoAboutPgpContactsResult> =
        object : Parcelable.Creator<UpdateInfoAboutPgpContactsResult> {
          override fun createFromParcel(source: Parcel): UpdateInfoAboutPgpContactsResult =
              UpdateInfoAboutPgpContactsResult(source)
      override fun newArray(size: Int): Array<UpdateInfoAboutPgpContactsResult?> = arrayOfNulls(size)
    }
  }
}
