/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * @author Denis Bondarenko
 * Date: 3/26/19
 * Time: 3:02 PM
 * E-mail: DenBond7@gmail.com
 */
data class DecryptErrorMsgBlock(
  @Expose override val content: String?,
  @Expose override val complete: Boolean,
  @SerializedName("decryptErr") @Expose val decryptErr: DecryptError?,
  @Expose override val error: MsgBlockError? = null
) : MsgBlock {

  @Expose
  override val type: MsgBlock.Type = MsgBlock.Type.DECRYPT_ERROR

  constructor(source: Parcel) : this(
    source.readString(),
    1 == source.readInt(),
    source.readParcelable<DecryptError>(DecryptError::class.java.classLoader),
    source.readParcelable(MsgBlockError::class.java.classLoader),
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeParcelable(type, flags)
    parcel.writeString(content)
    parcel.writeByte(if (complete) 1 else 0)
    parcel.writeParcelable(decryptErr, flags)
    parcel.writeParcelable(error, flags)
  }

  companion object CREATOR : Parcelable.Creator<DecryptErrorMsgBlock> {
    override fun createFromParcel(parcel: Parcel): DecryptErrorMsgBlock {
      parcel.readParcelable<MsgBlock.Type>(MsgBlock.Type::class.java.classLoader)
      return DecryptErrorMsgBlock(parcel)
    }

    override fun newArray(size: Int): Array<DecryptErrorMsgBlock?> = arrayOfNulls(size)
  }
}
