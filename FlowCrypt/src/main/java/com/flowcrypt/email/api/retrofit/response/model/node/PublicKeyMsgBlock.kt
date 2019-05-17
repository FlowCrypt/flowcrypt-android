/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.annotations.Expose

/**
 * It's a variant of [MsgBlock] which describes a public key.
 *
 * @author Denis Bondarenko
 * Date: 3/25/19
 * Time: 2:35 PM
 * E-mail: DenBond7@gmail.com
 */
data class PublicKeyMsgBlock constructor(@Expose override val type: MsgBlock.Type,
                                         @Expose override val content: String?,
                                         @Expose override val isComplete: Boolean,
                                         @Expose val keyDetails: NodeKeyDetails?) : MsgBlock {
  constructor(source: Parcel) : this(
      source.readParcelable<MsgBlock.Type>(MsgBlock.Type::class.java.classLoader)!!,
      source.readString(),
      1 == source.readInt(),
      source.readParcelable<NodeKeyDetails>(NodeKeyDetails::class.java.classLoader)
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(type, flags)
        writeString(content)
        writeInt((if (isComplete) 1 else 0))
        writeParcelable(keyDetails, 0)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<PublicKeyMsgBlock> = object : Parcelable.Creator<PublicKeyMsgBlock> {
      override fun createFromParcel(source: Parcel): PublicKeyMsgBlock = PublicKeyMsgBlock(source)
      override fun newArray(size: Int): Array<PublicKeyMsgBlock?> = arrayOfNulls(size)
    }
  }
}
