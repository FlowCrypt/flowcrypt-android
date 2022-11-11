/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.google.gson.annotations.Expose
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

/**
 * It's a variant of [MsgBlock] which describes a public key.
 *
 * @author Denis Bondarenko
 * Date: 3/25/19
 * Time: 2:35 PM
 * E-mail: DenBond7@gmail.com
 */
@Parcelize
data class PublicKeyMsgBlock constructor(
  @Expose override val content: String?,
  @Expose val keyDetails: PgpKeyDetails? = null,
  @Expose override val error: MsgBlockError? = null,
  @Expose override val isOpenPGPMimeSigned: Boolean
) : MsgBlock {
  @IgnoredOnParcel
  @Expose
  override val type: MsgBlock.Type = MsgBlock.Type.PUBLIC_KEY

  var existingRecipientWithPubKeys: RecipientWithPubKeys? = null

  constructor(parcel: Parcel) : this(
    parcel.readString(),
    parcel.readParcelableViaExt(PgpKeyDetails::class.java),
    parcel.readParcelableViaExt(MsgBlockError::class.java),
    1 == parcel.readInt()
  ) {
    existingRecipientWithPubKeys =
      parcel.readParcelableViaExt(RecipientWithPubKeys::class.java)
  }

  companion object : Parceler<PublicKeyMsgBlock> {
    override fun PublicKeyMsgBlock.write(parcel: Parcel, flags: Int) =
      with(parcel) {
        writeParcelable(type, flags)
        writeString(content)
        writeParcelable(keyDetails, flags)
        writeParcelable(error, flags)
        writeInt(if (isOpenPGPMimeSigned) 1 else 0)
        writeParcelable(existingRecipientWithPubKeys, flags)
      }

    override fun create(parcel: Parcel): PublicKeyMsgBlock {
      parcel.readParcelableViaExt(MsgBlock.Type::class.java)
      return PublicKeyMsgBlock(parcel)
    }
  }
}
