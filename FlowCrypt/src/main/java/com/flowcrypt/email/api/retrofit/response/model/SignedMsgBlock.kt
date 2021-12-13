/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 *               DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import org.pgpainless.decryption_verification.OpenPgpMetadata

/**
 * Message block which represents content with a signature.
 */
data class SignedMsgBlock(
  @Expose val signedType: Type,
  @Expose override val content: String?,
  @Expose override val complete: Boolean,
  @Expose val signature: String?,
  @Expose override val error: MsgBlockError? = null
) : MsgBlock {

  var openPgpMetadata: OpenPgpMetadata? = null

  @Expose
  override val type: MsgBlock.Type = when (signedType) {
    Type.SIGNED_MSG -> MsgBlock.Type.SIGNED_MSG
    Type.SIGNED_TEXT -> MsgBlock.Type.SIGNED_TEXT
    Type.SIGNED_HTML -> MsgBlock.Type.SIGNED_HTML
  }

  constructor(source: Parcel) : this(
    source.readParcelable<Type>(Type::class.java.classLoader)
      ?: throw IllegalArgumentException("Undefined type"),
    source.readString(),
    1 == source.readInt(),
    source.readString(),
    source.readParcelable<MsgBlockError>(MsgBlockError::class.java.classLoader)
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeParcelable(type, flags)
    writeParcelable(signedType, flags)
    writeString(content)
    writeInt(if (complete) 1 else 0)
    writeString(signature)
    writeParcelable(error, flags)
  }

  enum class Type : Parcelable {
    SIGNED_MSG,
    SIGNED_TEXT,
    SIGNED_HTML;

    override fun describeContents(): Int {
      return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
      dest.writeInt(ordinal)
    }

    companion object {
      @JvmField
      val CREATOR: Parcelable.Creator<Type> = object : Parcelable.Creator<Type> {
        override fun createFromParcel(source: Parcel): Type = values()[source.readInt()]
        override fun newArray(size: Int): Array<Type?> = arrayOfNulls(size)
      }
    }
  }

  companion object CREATOR : Parcelable.Creator<MsgBlock> {
    override fun createFromParcel(parcel: Parcel): MsgBlock {
      val partType = parcel.readParcelable<MsgBlock.Type>(MsgBlock.Type::class.java.classLoader)!!
      return MsgBlockFactory.fromParcel(partType, parcel)
    }

    override fun newArray(size: Int): Array<SignedMsgBlock?> = arrayOfNulls(size)
  }
}
