/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 *               DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt
import com.google.gson.annotations.Expose

data class EncryptedAttLinkMsgBlock(
  @Expose override val attMeta: AttMeta,
  @Expose override val error: MsgBlockError? = null,
  @Expose override val isOpenPGPMimeSigned: Boolean
) : AttMsgBlock {

  @Expose
  override val content: String = ""

  @Expose
  override val type: MsgBlock.Type = MsgBlock.Type.ENCRYPTED_ATT_LINK

  constructor(source: Parcel) : this(
    source.readParcelableViaExt(AttMeta::class.java)!!,
    source.readParcelableViaExt(MsgBlockError::class.java),
    1 == source.readInt()
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeParcelable(attMeta, flags)
    parcel.writeParcelable(error, flags)
    parcel.writeInt(if (isOpenPGPMimeSigned) 1 else 0)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<EncryptedAttLinkMsgBlock> {
    override fun createFromParcel(parcel: Parcel) = EncryptedAttLinkMsgBlock(parcel)
    override fun newArray(size: Int): Array<EncryptedAttLinkMsgBlock?> = arrayOfNulls(size)
  }
}
