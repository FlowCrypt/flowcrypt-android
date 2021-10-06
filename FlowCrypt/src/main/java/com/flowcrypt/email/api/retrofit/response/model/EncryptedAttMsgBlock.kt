/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 *               DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose

data class EncryptedAttMsgBlock(
  @Expose override val content: String?,
  @Expose override val attMeta: AttMeta,
  @Expose override val error: MsgBlockError? = null
) : AttMsgBlock {

  var fileUri: Uri? = null

  @Expose
  override val complete: Boolean = true

  @Expose
  override val type: MsgBlock.Type = MsgBlock.Type.ENCRYPTED_ATT

  constructor(source: Parcel) : this(
    source.readString(),
    source.readParcelable<AttMeta>(AttMeta::class.java.classLoader)!!,
    source.readParcelable<MsgBlockError>(MsgBlockError::class.java.classLoader)
  ) {
    fileUri = source.readParcelable(Uri::class.java.classLoader)
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
    with(dest) {
      writeParcelable(type, flags)
      writeString(content)
      writeParcelable(attMeta, flags)
      writeParcelable(error, flags)
      writeParcelable(fileUri, flags)
    }

  companion object CREATOR : Parcelable.Creator<EncryptedAttMsgBlock> {
    override fun createFromParcel(parcel: Parcel): EncryptedAttMsgBlock {
      parcel.readParcelable<MsgBlock.Type>(MsgBlock.Type::class.java.classLoader)
      return EncryptedAttMsgBlock(parcel)
    }

    override fun newArray(size: Int): Array<EncryptedAttMsgBlock?> = arrayOfNulls(size)
  }
}
