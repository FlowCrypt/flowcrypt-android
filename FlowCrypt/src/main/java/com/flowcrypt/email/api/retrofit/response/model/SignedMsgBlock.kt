/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 *               DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt
import com.google.gson.annotations.Expose
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import org.pgpainless.decryption_verification.MessageMetadata

/**
 * Message block which represents content with a signature.
 */
@Parcelize
data class SignedMsgBlock(
  @Expose override val content: String?,
  @Expose val signature: String? = null,
  @Expose override val error: MsgBlockError? = null,
  @Expose override val isOpenPGPMimeSigned: Boolean
) : MsgBlock {

  @IgnoredOnParcel
  var openPgpMetadata: MessageMetadata? = null

  @IgnoredOnParcel
  @Expose
  override val type: MsgBlock.Type = MsgBlock.Type.SIGNED_CONTENT

  constructor(source: Parcel) : this(
    source.readString(),
    source.readString(),
    source.readParcelableViaExt(MsgBlockError::class.java),
    1 == source.readInt()
  )

  companion object : Parceler<SignedMsgBlock> {

    override fun SignedMsgBlock.write(parcel: Parcel, flags: Int) = with(parcel) {
      writeParcelable(type, flags)
      writeString(content)
      writeString(signature)
      writeParcelable(error, flags)
      writeInt(if (isOpenPGPMimeSigned) 1 else 0)
    }

    override fun create(parcel: Parcel): SignedMsgBlock {
      val partType = requireNotNull(parcel.readParcelableViaExt(MsgBlock.Type::class.java))
      return MsgBlockFactory.fromParcel(partType, parcel) as SignedMsgBlock
    }
  }
}
