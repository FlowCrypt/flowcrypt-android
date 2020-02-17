/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.BufferedInputStream

/**
 * It's a result for "generateKey" requests.
 *
 * @author Denis Bondarenko
 * Date: 4/1/19
 * Time: 9:44 AM
 * E-mail: DenBond7@gmail.com
 */
data class GenerateKeyResult constructor(@Expose val key: NodeKeyDetails?,
                                         @SerializedName("error")
                                         @Expose override val apiError: ApiError?) : BaseNodeResponse {
  override fun handleRawData(bufferedInputStream: BufferedInputStream) {

  }

  constructor(source: Parcel) : this(
      source.readParcelable<NodeKeyDetails>(NodeKeyDetails::class.java.classLoader),
      source.readParcelable<ApiError>(ApiError::class.java.classLoader)
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(key, flags)
        writeParcelable(apiError, flags)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<GenerateKeyResult> = object : Parcelable.Creator<GenerateKeyResult> {
      override fun createFromParcel(source: Parcel): GenerateKeyResult = GenerateKeyResult(source)
      override fun newArray(size: Int): Array<GenerateKeyResult?> = arrayOfNulls(size)
    }
  }
}
