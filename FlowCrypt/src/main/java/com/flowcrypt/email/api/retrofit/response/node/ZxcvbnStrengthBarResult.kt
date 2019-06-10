/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node

import android.os.Parcel
import android.os.Parcelable

import com.flowcrypt.email.api.retrofit.response.model.node.Error
import com.flowcrypt.email.api.retrofit.response.model.node.Word
import com.google.gson.annotations.Expose

import java.io.BufferedInputStream
import java.io.IOException

/**
 * It's a result for "zxcvbnStrengthBar" requests.
 *
 * @author Denis Bondarenko
 * Date: 4/1/19
 * Time: 3:06 PM
 * E-mail: DenBond7@gmail.com
 */
data class ZxcvbnStrengthBarResult constructor(@Expose val word: Word?,
                                               @Expose val seconds: Double,
                                               @Expose val time: String?,
                                               @Expose override val error: Error?) : BaseNodeResponse {
  @Throws(IOException::class)
  override fun handleRawData(bufferedInputStream: BufferedInputStream) {

  }

  constructor(source: Parcel) : this(
      source.readParcelable<Word>(Word::class.java.classLoader),
      source.readDouble(),
      source.readString(),
      source.readParcelable<Error>(Error::class.java.classLoader)
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(word, 0)
        writeDouble(seconds)
        writeString(time)
        writeParcelable(error, 0)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<ZxcvbnStrengthBarResult> = object : Parcelable.Creator<ZxcvbnStrengthBarResult> {
      override fun createFromParcel(source: Parcel): ZxcvbnStrengthBarResult = ZxcvbnStrengthBarResult(source)
      override fun newArray(size: Int): Array<ZxcvbnStrengthBarResult?> = arrayOfNulls(size)
    }
  }
}
