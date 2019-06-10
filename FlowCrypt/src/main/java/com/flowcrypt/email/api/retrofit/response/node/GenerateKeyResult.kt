/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node

import android.os.Parcel
import android.os.Parcelable

import com.flowcrypt.email.api.retrofit.response.model.node.Error
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.google.gson.annotations.Expose

import java.io.BufferedInputStream
import java.io.IOException

/**
 * It's a result for "generateKey" requests.
 *
 * @author Denis Bondarenko
 * Date: 4/1/19
 * Time: 9:44 AM
 * E-mail: DenBond7@gmail.com
 */
data class GenerateKeyResult constructor(@Expose val key: NodeKeyDetails?,
                                         @Expose override val error: Error?) : BaseNodeResponse {
  @Throws(IOException::class)
  override fun handleRawData(bufferedInputStream: BufferedInputStream) {

  }

  constructor(source: Parcel) : this(
      source.readParcelable<NodeKeyDetails>(NodeKeyDetails::class.java.classLoader),
      source.readParcelable<Error>(Error::class.java.classLoader)
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(key, 0)
        writeParcelable(error, 0)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<GenerateKeyResult> = object : Parcelable.Creator<GenerateKeyResult> {
      override fun createFromParcel(source: Parcel): GenerateKeyResult = GenerateKeyResult(source)
      override fun newArray(size: Int): Array<GenerateKeyResult?> = arrayOfNulls(size)
    }
  }
}
