/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.api.retrofit.response.model.node

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose

/**
 * Generic message block represents plaintext or HTML message block with a signature.
 */
data class SignedMsgBlock(@Expose override val type: MsgBlock.Type = MsgBlock.Type.UNKNOWN,
                           @Expose override val content: String?,
                           @Expose override val complete: Boolean,
                           @Expose val signature: String?) : MsgBlock {

  constructor(type: MsgBlock.Type, source: Parcel) : this(
    type,
    source.readString(),
    1 == source.readInt(),
    source.readString()
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeParcelable(type, flags)
    writeString(content)
    writeInt(if (complete) 1 else 0)
    writeString(signature)
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<MsgBlock> = object : Parcelable.Creator<MsgBlock> {
      override fun createFromParcel(source: Parcel): MsgBlock {
        val partType = source.readParcelable<MsgBlock.Type>(MsgBlock.Type::class.java.classLoader)!!
        return MsgBlockFactory.fromParcel(partType, source)
      }

      override fun newArray(size: Int): Array<MsgBlock?> = arrayOfNulls(size)
    }
  }
}
