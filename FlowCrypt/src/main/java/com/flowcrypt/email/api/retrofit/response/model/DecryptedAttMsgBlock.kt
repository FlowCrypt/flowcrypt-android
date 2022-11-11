/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.net.Uri
import android.os.Parcel
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize

/**
 * @author Denis Bondarenko
 *         Date: 7/12/19
 *         Time: 10:12 AM
 *         E-mail: DenBond7@gmail.com
 */
@Parcelize
data class DecryptedAttMsgBlock(
  @Expose override val content: String?,
  @Expose override val attMeta: AttMeta,
  @SerializedName("decryptErr") @Expose val decryptErr: DecryptError?,
  @Expose override val error: MsgBlockError? = null,
  @Expose override val isOpenPGPMimeSigned: Boolean
) : AttMsgBlock {

  var fileUri: Uri? = null

  @IgnoredOnParcel
  @Expose
  override val type: MsgBlock.Type = MsgBlock.Type.DECRYPTED_ATT

  constructor(source: Parcel) : this(
    source.readString(),
    source.readParcelableViaExt(AttMeta::class.java)!!,
    source.readParcelableViaExt(DecryptError::class.java),
    source.readParcelableViaExt(MsgBlockError::class.java),
    1 == source.readInt()
  ) {
    fileUri = source.readParcelableViaExt(Uri::class.java)
  }

  companion object : Parceler<DecryptedAttMsgBlock> {

    override fun DecryptedAttMsgBlock.write(parcel: Parcel, flags: Int) = with(parcel) {
      writeParcelable(type, flags)
      writeString(content)
      writeParcelable(attMeta, flags)
      writeParcelable(decryptErr, flags)
      writeParcelable(error, flags)
      writeInt(if (isOpenPGPMimeSigned) 1 else 0)
      writeParcelable(fileUri, flags)
    }

    override fun create(parcel: Parcel): DecryptedAttMsgBlock {
      parcel.readParcelableViaExt(MsgBlock.Type::class.java)
      return DecryptedAttMsgBlock(parcel)
    }
  }
}
