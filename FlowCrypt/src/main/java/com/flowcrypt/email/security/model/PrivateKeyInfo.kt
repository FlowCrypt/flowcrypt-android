/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.model

import android.os.Parcel
import android.os.Parcelable

import com.flowcrypt.email.model.PgpKeyInfo

/**
 * A simple private key information object which contais a [PgpKeyInfo] object and the
 * private key passphrase.
 *
 * @author DenBond7
 * Date: 16.05.2017
 * Time: 13:17
 * E-mail: DenBond7@gmail.com
 */

data class PrivateKeyInfo constructor(var pgpKeyInfo: PgpKeyInfo? = null,
                                      var passphrase: String? = null) : Parcelable {
  constructor(source: Parcel) : this(
      source.readParcelable<PgpKeyInfo>(PgpKeyInfo::class.java.classLoader),
      source.readString()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(pgpKeyInfo, 0)
        writeString(passphrase)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<PrivateKeyInfo> = object : Parcelable.Creator<PrivateKeyInfo> {
      override fun createFromParcel(source: Parcel): PrivateKeyInfo = PrivateKeyInfo(source)
      override fun newArray(size: Int): Array<PrivateKeyInfo?> = arrayOfNulls(size)
    }
  }
}
