/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.annotations.Expose

/**
 * @author Denis Bondarenko
 * Date: 4/1/19
 * Time: 3:31 PM
 * E-mail: DenBond7@gmail.com
 */
data class Word constructor(@Expose val match: String?,
                            @Expose val word: String?,
                            @Expose val bar: Int,
                            @Expose val color: String?,
                            @Expose val isPass: Boolean) : Parcelable {
  constructor(source: Parcel) : this(
      source.readString(),
      source.readString(),
      source.readInt(),
      source.readString(),
      1 == source.readInt()
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeString(match)
    writeString(word)
    writeInt(bar)
    writeString(color)
    writeInt((if (isPass) 1 else 0))
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<Word> = object : Parcelable.Creator<Word> {
      override fun createFromParcel(source: Parcel): Word = Word(source)
      override fun newArray(size: Int): Array<Word?> = arrayOfNulls(size)
    }
  }
}
