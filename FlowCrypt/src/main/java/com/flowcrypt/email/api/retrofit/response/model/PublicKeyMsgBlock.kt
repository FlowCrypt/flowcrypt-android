/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys

import com.flowcrypt.email.security.model.PgpKeyDetails

import com.google.gson.annotations.Expose

/**
 * It's a variant of [MsgBlock] which describes a public key.
 *
 * @author Denis Bondarenko
 * Date: 3/25/19
 * Time: 2:35 PM
 * E-mail: DenBond7@gmail.com
 */
data class PublicKeyMsgBlock constructor(
  @Expose override val content: String?,
  @Expose val keyDetails: PgpKeyDetails? = null,
  @Expose override val error: MsgBlockError? = null,
  @Expose override val isOpenPGPMimeSigned: Boolean
) : MsgBlock {
  @Expose
  override val type: MsgBlock.Type = MsgBlock.Type.PUBLIC_KEY

  var existingRecipientWithPubKeys: RecipientWithPubKeys? = null

  constructor(parcel: Parcel) : this(
    parcel.readString(),
    parcel.readParcelable(PgpKeyDetails::class.java.classLoader),
    parcel.readParcelable(MsgBlockError::class.java.classLoader),
    1 == parcel.readInt()
  ) {
    existingRecipientWithPubKeys =
      parcel.readParcelable(RecipientWithPubKeys::class.java.classLoader)
  }

  override fun writeToParcel(parcel: Parcel, flags: Int) =
    with(parcel) {
      writeParcelable(type, flags)
      writeString(content)
      writeParcelable(keyDetails, flags)
      writeParcelable(error, flags)
      writeParcelable(existingRecipientWithPubKeys, flags)
      writeInt(if (isOpenPGPMimeSigned) 1 else 0)
    }

  override fun describeContents(): Int = 0

  companion object CREATOR : Parcelable.Creator<PublicKeyMsgBlock> {
    override fun createFromParcel(parcel: Parcel): PublicKeyMsgBlock {
      parcel.readParcelable<MsgBlock.Type>(MsgBlock.Type::class.java.classLoader)
      return PublicKeyMsgBlock(parcel)
    }

    override fun newArray(size: Int): Array<PublicKeyMsgBlock?> = arrayOfNulls(size)
  }
}
