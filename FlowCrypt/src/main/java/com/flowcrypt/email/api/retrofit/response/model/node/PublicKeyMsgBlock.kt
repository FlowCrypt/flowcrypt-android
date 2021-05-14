/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.model.PgpContact
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
data class PublicKeyMsgBlock constructor(@Expose override val content: String?,
                                         @Expose override val complete: Boolean,
                                         @Expose val keyDetails: PgpKeyDetails?) : MsgBlock {
  @Expose
  override val type: MsgBlock.Type = MsgBlock.Type.PUBLIC_KEY

  var existingPgpContact: PgpContact? = null

  constructor(source: Parcel) : this(
      source.readString(),
      1 == source.readInt(),
      source.readParcelable<PgpKeyDetails>(PgpKeyDetails::class.java.classLoader)
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeParcelable(type, flags)
        writeString(content)
        writeInt((if (complete) 1 else 0))
        writeParcelable(keyDetails, 0)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<PublicKeyMsgBlock> = object : Parcelable.Creator<PublicKeyMsgBlock> {
      override fun createFromParcel(source: Parcel): PublicKeyMsgBlock {
        source.readParcelable<MsgBlock.Type>(MsgBlock.Type::class.java.classLoader)
        return PublicKeyMsgBlock(source)
      }

      override fun newArray(size: Int): Array<PublicKeyMsgBlock?> = arrayOfNulls(size)
    }
  }
}
