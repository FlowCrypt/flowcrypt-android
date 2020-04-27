/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose

/**
 * It's a base [MsgBlock]
 *
 * @author Denis Bondarenko
 * Date: 3/26/19
 * Time: 9:46 AM
 * E-mail: DenBond7@gmail.com
 */
data class BaseMsgBlock(@Expose override val type: MsgBlock.Type = MsgBlock.Type.UNKNOWN,
                        @Expose override val content: String?,
                        @Expose override val complete: Boolean) : MsgBlock {

  constructor(source: Parcel, type: MsgBlock.Type) : this(
      type,
      source.readString(),
      1 == source.readInt())

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeParcelable(type, flags)
    writeString(content)
    writeInt((if (complete) 1 else 0))
  }

  companion object {
    const val TAG_TYPE = "type"

    val handledMsgBlockTypes = listOf(
        MsgBlock.Type.PUBLIC_KEY,
        MsgBlock.Type.DECRYPT_ERROR,
        MsgBlock.Type.DECRYPTED_ATT
    )

    @JvmField
    val CREATOR: Parcelable.Creator<MsgBlock> = object : Parcelable.Creator<MsgBlock> {
      override fun createFromParcel(source: Parcel): MsgBlock {
        val partType = source.readParcelable<MsgBlock.Type>(MsgBlock.Type::class.java.classLoader)!!
        return genMsgBlockFromType(source, partType)
      }

      override fun newArray(size: Int): Array<MsgBlock?> = arrayOfNulls(size)
    }

    fun genMsgBlockFromType(source: Parcel, type: MsgBlock.Type): MsgBlock {
      return when (type) {
        MsgBlock.Type.PUBLIC_KEY -> PublicKeyMsgBlock(source)

        MsgBlock.Type.DECRYPT_ERROR -> DecryptErrorMsgBlock(source)

        MsgBlock.Type.DECRYPTED_ATT -> DecryptedAttMsgBlock(source)

        else -> BaseMsgBlock(source, type)
      }
    }
  }
}
