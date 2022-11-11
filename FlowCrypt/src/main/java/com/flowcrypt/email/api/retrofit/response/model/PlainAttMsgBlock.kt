/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 *               DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.net.Uri
import android.os.Parcel
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt
import com.google.gson.annotations.Expose
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlainAttMsgBlock(
  @Expose override val content: String?,
  @Expose override val attMeta: AttMeta,
  @Expose override val error: MsgBlockError? = null,
  @Expose override val isOpenPGPMimeSigned: Boolean
) : AttMsgBlock {

  var fileUri: Uri? = null

  @IgnoredOnParcel
  @Expose
  override val type: MsgBlock.Type = MsgBlock.Type.PLAIN_ATT

  constructor(source: Parcel) : this(
    source.readString(),
    source.readParcelableViaExt(AttMeta::class.java)!!,
    source.readParcelableViaExt(MsgBlockError::class.java),
    1 == source.readInt()
  ) {
    fileUri = source.readParcelableViaExt(Uri::class.java)
  }

  companion object : Parceler<PlainAttMsgBlock> {

    override fun PlainAttMsgBlock.write(parcel: Parcel, flags: Int) = with(parcel) {
      writeParcelable(type, flags)
      writeString(content)
      writeParcelable(attMeta, flags)
      writeParcelable(error, flags)
      writeInt(if (isOpenPGPMimeSigned) 1 else 0)
      writeParcelable(fileUri, flags)
    }

    override fun create(parcel: Parcel): PlainAttMsgBlock {
      parcel.readParcelableViaExt(MsgBlock.Type::class.java)
      return PlainAttMsgBlock(parcel)
    }
  }
}
