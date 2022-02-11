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
  @Expose override val content: String?,
  @Expose val signature: String? = null,
  @Expose override val error: MsgBlockError? = null,
  @Expose override val isOpenPGPMimeSigned: Boolean
) : MsgBlock {

  var openPgpMetadata: OpenPgpMetadata? = null

  @Expose
  override val type: MsgBlock.Type = MsgBlock.Type.SIGNED_CONTENT

  constructor(source: Parcel) : this(
    source.readString(),
    source.readString(),
    source.readParcelable<MsgBlockError>(MsgBlockError::class.java.classLoader),
    1 == source.readInt()
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeParcelable(type, flags)
    writeString(content)
    writeString(signature)
    writeParcelable(error, flags)
    writeInt(if (isOpenPGPMimeSigned) 1 else 0)
  }

  companion object CREATOR : Parcelable.Creator<MsgBlock> {
    override fun createFromParcel(parcel: Parcel): MsgBlock {
      val partType = parcel.readParcelable<MsgBlock.Type>(MsgBlock.Type::class.java.classLoader)!!
      return MsgBlockFactory.fromParcel(partType, parcel)
    }

    override fun newArray(size: Int): Array<SignedMsgBlock?> = arrayOfNulls(size)
  }
}
