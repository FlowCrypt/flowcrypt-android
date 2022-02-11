/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import org.pgpainless.decryption_verification.OpenPgpMetadata

/**
 * @author Denis Bondarenko
 *         Date: 12/8/21
 *         Time: 6:55 PM
 *         E-mail: DenBond7@gmail.com
 */
data class DecryptedAndOrSignedContentMsgBlock(
  @Expose override val error: MsgBlockError? = null,
  @Expose val blocks: List<MsgBlock> = listOf(),
  @Expose override val isOpenPGPMimeSigned: Boolean
) : MsgBlock {
  @Expose
  override val content: String? = null

  @Expose
  override val type: MsgBlock.Type = MsgBlock.Type.DECRYPTED_AND_OR_SIGNED_CONTENT

  var openPgpMetadata: OpenPgpMetadata? = null

  constructor(parcel: Parcel) : this(
    parcel.readParcelable(MsgBlockError::class.java.classLoader),
    mutableListOf<MsgBlock>().apply { parcel.readTypedList(this, GenericMsgBlock.CREATOR) },
    1 == parcel.readInt()
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeParcelable(error, flags)
    parcel.writeList(blocks)
    parcel.writeInt(if (isOpenPGPMimeSigned) 1 else 0)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<DecryptedAndOrSignedContentMsgBlock> {
    override fun createFromParcel(parcel: Parcel) = DecryptedAndOrSignedContentMsgBlock(parcel)
    override fun newArray(size: Int): Array<DecryptedAndOrSignedContentMsgBlock?> =
      arrayOfNulls(size)
  }
}
