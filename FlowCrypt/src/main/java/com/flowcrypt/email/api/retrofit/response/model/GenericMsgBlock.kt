/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 *               DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt
import com.google.gson.annotations.Expose
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

/**
 * Generic message block represents any message block without a dedicated support.
 *
 * @author Denis Bondarenko
 * Date: 3/26/19
 * Time: 9:46 AM
 * E-mail: DenBond7@gmail.com
 *
 * @author Ivan Pizhenko
 */
@Parcelize
data class GenericMsgBlock(
  @Expose override val type: MsgBlock.Type = MsgBlock.Type.UNKNOWN,
  @Expose override val content: String?,
  @Expose override val error: MsgBlockError? = null,
  @Expose override val isOpenPGPMimeSigned: Boolean
) : MsgBlock {

  constructor(type: MsgBlock.Type, source: Parcel) : this(
    type,
    source.readString(),
    source.readParcelableViaExt(MsgBlockError::class.java),
    1 == source.readInt()
  )

  companion object : Parceler<GenericMsgBlock> {

    override fun GenericMsgBlock.write(parcel: Parcel, flags: Int) = with(parcel) {
      writeParcelable(type, flags)
      writeString(content)
      writeParcelable(error, flags)
      writeInt(if (isOpenPGPMimeSigned) 1 else 0)
    }

    override fun create(parcel: Parcel): GenericMsgBlock {
      val partType = parcel.readParcelableViaExt(MsgBlock.Type::class.java)!!
      return GenericMsgBlock(partType, parcel)
    }
  }
}
