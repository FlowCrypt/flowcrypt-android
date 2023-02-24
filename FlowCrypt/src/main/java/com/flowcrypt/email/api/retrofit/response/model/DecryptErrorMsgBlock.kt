/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

/**
 * @author Denys Bondarenko
 */
@Parcelize
data class DecryptErrorMsgBlock(
  @Expose override val content: String?,
  @SerializedName("decryptErr") @Expose val decryptErr: DecryptError?,
  @Expose override val error: MsgBlockError? = null,
  @Expose override val isOpenPGPMimeSigned: Boolean
) : MsgBlock {

  @IgnoredOnParcel
  @Expose
  override val type: MsgBlock.Type = MsgBlock.Type.DECRYPT_ERROR

  constructor(source: Parcel) : this(
    source.readString(),
    source.readParcelableViaExt(DecryptError::class.java),
    source.readParcelableViaExt(MsgBlockError::class.java),
    1 == source.readInt()
  )

  companion object : Parceler<DecryptErrorMsgBlock> {
    override fun DecryptErrorMsgBlock.write(parcel: Parcel, flags: Int) {
      parcel.writeParcelable(type, flags)
      parcel.writeString(content)
      parcel.writeParcelable(decryptErr, flags)
      parcel.writeParcelable(error, flags)
      parcel.writeInt(if (isOpenPGPMimeSigned) 1 else 0)
    }

    override fun create(parcel: Parcel): DecryptErrorMsgBlock {
      parcel.readParcelableViaExt(MsgBlock.Type::class.java)
      return DecryptErrorMsgBlock(parcel)
    }
  }
}
