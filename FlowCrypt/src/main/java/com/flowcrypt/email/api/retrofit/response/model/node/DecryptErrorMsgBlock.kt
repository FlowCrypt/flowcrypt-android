/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose

/**
 * @author Denis Bondarenko
 * Date: 3/26/19
 * Time: 3:02 PM
 * E-mail: DenBond7@gmail.com
 */
data class DecryptErrorMsgBlock(@Expose override val type: MsgBlock.Type,
                                @Expose override val content: String?,
                                @Expose override val isComplete: Boolean,
                                @Expose val error: DecryptError?) : MsgBlock {
  constructor(source: Parcel) : this(
      source.readParcelable<MsgBlock.Type>(MsgBlock.Type::class.java.classLoader)!!,
      source.readString(),
      1 == source.readInt(),
      source.readParcelable<DecryptError>(DecryptError::class.java.classLoader)
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(type, flags)
        writeString(content)
        writeInt((if (isComplete) 1 else 0))
        writeParcelable(error, flags)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<DecryptErrorMsgBlock> = object : Parcelable.Creator<DecryptErrorMsgBlock> {
      override fun createFromParcel(source: Parcel): DecryptErrorMsgBlock = DecryptErrorMsgBlock(source)
      override fun newArray(size: Int): Array<DecryptErrorMsgBlock?> = arrayOfNulls(size)
    }
  }
}
