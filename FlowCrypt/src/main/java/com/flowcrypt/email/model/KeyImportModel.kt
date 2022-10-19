/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt

/**
 * This model describes info about an imported key.
 *
 * @author Denis Bondarenko
 * Date: 09.08.2018
 * Time: 15:47
 * E-mail: DenBond7@gmail.com
 */
data class KeyImportModel constructor(
  val fileUri: Uri? = null,
  val keyString: String? = null,
  val isPrivateKey: Boolean = false,
  val sourceType: KeyImportDetails.SourceType
) : Parcelable {
  constructor(source: Parcel) : this(
    source.readParcelableViaExt<Uri>(Uri::class.java),
    source.readString(),
    source.readInt() == 1,
    source.readParcelableViaExt(KeyImportDetails.SourceType::class.java)!!
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
    with(dest) {
      writeParcelable(fileUri, flags)
      writeString(keyString)
      writeInt((if (isPrivateKey) 1 else 0))
      writeParcelable(sourceType, flags)
    }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<KeyImportModel> = object : Parcelable.Creator<KeyImportModel> {
      override fun createFromParcel(source: Parcel): KeyImportModel = KeyImportModel(source)
      override fun newArray(size: Int): Array<KeyImportModel?> = arrayOfNulls(size)
    }
  }
}
