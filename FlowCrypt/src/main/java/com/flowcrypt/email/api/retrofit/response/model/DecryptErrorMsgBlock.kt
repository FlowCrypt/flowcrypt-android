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
  @SerializedName("decryptErr") @Expose val error: DecryptError?
) : MsgBlock {

  @Expose
  override val type: MsgBlock.Type = MsgBlock.Type.DECRYPT_ERROR

  constructor(source: Parcel) : this(
    source.readString(),
    1 == source.readInt(),
    source.readParcelable<DecryptError>(DecryptError::class.java.classLoader)
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
    with(dest) {
      writeParcelable(type, flags)
      writeString(content)
      writeInt(if (complete) 1 else 0)
      writeParcelable(error, flags)
    }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<DecryptErrorMsgBlock> =
      object : Parcelable.Creator<DecryptErrorMsgBlock> {
        override fun createFromParcel(source: Parcel): DecryptErrorMsgBlock {
          source.readParcelable<MsgBlock.Type>(MsgBlock.Type::class.java.classLoader)
          return DecryptErrorMsgBlock(source)
        }

        override fun newArray(size: Int): Array<DecryptErrorMsgBlock?> = arrayOfNulls(size)
      }
  }
}
