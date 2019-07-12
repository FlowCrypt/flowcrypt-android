/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * @author Denis Bondarenko
 *         Date: 7/12/19
 *         Time: 10:12 AM
 *         E-mail: DenBond7@gmail.com
 */
data class DecryptedAttMsgBlock(@Expose override val content: String?,
                                @Expose override val complete: Boolean,
                                @Expose val attMeta: AttMeta,
                                @SerializedName("decryptErr") @Expose val error: DecryptError?) : MsgBlock {

  var fileUri: Uri? = null

  @Expose
  override val type: MsgBlock.Type = MsgBlock.Type.DECRYPTED_ATT

  constructor(source: Parcel) : this(
      source.readString(),
      1 == source.readInt(),
      source.readParcelable<AttMeta>(AttMeta::class.java.classLoader),
      source.readParcelable<DecryptError>(DecryptError::class.java.classLoader)
  ) {
    fileUri = source.readParcelable<Uri>(Uri::class.java.classLoader)
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(type, flags)
        writeString(content)
        writeInt((if (complete) 1 else 0))
        writeParcelable(attMeta, flags)
        writeParcelable(error, flags)
        writeParcelable(fileUri, flags)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<DecryptedAttMsgBlock> = object : Parcelable.Creator<DecryptedAttMsgBlock> {
      override fun createFromParcel(source: Parcel): DecryptedAttMsgBlock {
        source.readParcelable<MsgBlock.Type>(MsgBlock.Type::class.java.classLoader)
        return DecryptedAttMsgBlock(source)
      }

      override fun newArray(size: Int): Array<DecryptedAttMsgBlock?> = arrayOfNulls(size)
    }
  }
}
