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
 * It's a result for "parseKeys" requests.
 *
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 12:01 PM
 * E-mail: DenBond7@gmail.com
 */
data class ParseKeysResult constructor(@Expose val format: String? = null,
                                       @Expose @SerializedName("keyDetails") val nodeKeyDetails:
                                       MutableList<NodeKeyDetails> = mutableListOf(),
                                       @SerializedName("error")
                                       @Expose override val apiError: ApiError? = null) :
    BaseNodeResponse {
  override fun handleRawData(bufferedInputStream: BufferedInputStream) {

  }

  constructor(source: Parcel) : this(
      source.readString(),
      source.createTypedArrayList(NodeKeyDetails.CREATOR)!!,
      source.readParcelable<ApiError>(ApiError::class.java.classLoader)
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeString(format)
        writeTypedList(nodeKeyDetails)
        writeParcelable(apiError, flags)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<ParseKeysResult> = object : Parcelable.Creator<ParseKeysResult> {
      override fun createFromParcel(source: Parcel): ParseKeysResult = ParseKeysResult(source)
      override fun newArray(size: Int): Array<ParseKeysResult?> = arrayOfNulls(size)
    }
  }
}
