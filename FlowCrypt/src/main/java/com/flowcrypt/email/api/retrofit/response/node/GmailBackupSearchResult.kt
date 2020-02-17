/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.node

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.BufferedInputStream

/**
 * It's a result for "gmailBackupSearch" requests.
 *
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 9:55 AM
 * E-mail: DenBond7@gmail.com
 */
data class GmailBackupSearchResult constructor(@Expose val query: String?,
                                               @SerializedName("error")
                                               @Expose override val apiError: ApiError?) :
    BaseNodeResponse {
  override fun handleRawData(bufferedInputStream: BufferedInputStream) {

  }

  constructor(source: Parcel) : this(
      source.readString(),
      source.readParcelable<ApiError>(ApiError::class.java.classLoader)
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeString(query)
        writeParcelable(apiError, flags)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<GmailBackupSearchResult> = object : Parcelable.Creator<GmailBackupSearchResult> {
      override fun createFromParcel(source: Parcel): GmailBackupSearchResult = GmailBackupSearchResult(source)
      override fun newArray(size: Int): Array<GmailBackupSearchResult?> = arrayOfNulls(size)
    }
  }
}
