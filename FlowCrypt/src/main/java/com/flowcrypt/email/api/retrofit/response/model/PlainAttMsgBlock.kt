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

data class PlainAttMsgBlock(
  @Expose override val content: String?,
  @Expose override val attMeta: AttMeta
) : AttMsgBlock {

  var fileUri: Uri? = null

  @Expose
  override val complete: Boolean = true

  @Expose
  override val type: MsgBlock.Type = MsgBlock.Type.PLAIN_ATT

  constructor(source: Parcel) : this(
    source.readString(),
    source.readParcelable<AttMeta>(AttMeta::class.java.classLoader)!!
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
      writeParcelable(fileUri, flags)
    }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<PlainAttMsgBlock> = object : Parcelable.Creator<PlainAttMsgBlock> {
      override fun createFromParcel(source: Parcel): PlainAttMsgBlock {
        source.readParcelable<MsgBlock.Type>(MsgBlock.Type::class.java.classLoader)
        return PlainAttMsgBlock(source)
      }

      override fun newArray(size: Int): Array<PlainAttMsgBlock?> = arrayOfNulls(size)
    }
  }
}
