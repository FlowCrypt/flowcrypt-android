/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.Constants

/**
 * Simple POJO which defines information about email attachments.
 *
 * @author Denis Bondarenko
 * Date: 07.08.2017
 * Time: 18:38
 * E-mail: DenBond7@gmail.com
 */
data class AttachmentInfo constructor(var rawData: String? = null,
                                      var email: String? = null,
                                      var folder: String? = null,
                                      var uid: Int = 0,
                                      var fwdFolder: String? = null,
                                      var fwdUid: Int = 0,
                                      var name: String? = null,
                                      var encodedSize: Long = 0,
                                      var type: String? = Constants.MIME_TYPE_BINARY_DATA,
                                      var id: String? = null,
                                      var path: String = "0",
                                      var uri: Uri? = null,
                                      var isProtected: Boolean = false,
                                      var isForwarded: Boolean = false,
                                      var isDecrypted: Boolean = false,
                                      var orderNumber: Int = 0) : Parcelable {

  val uniqueStringId: String
    get() = uid.toString() + "_" + id

  fun copy(newFolder: String): AttachmentInfo {
    return copy(folder = newFolder, uid = 0, fwdFolder = this.folder, fwdUid = this.uid, orderNumber = 0)
  }

  constructor(source: Parcel) : this(
      source.readString(),
      source.readString(),
      source.readString(),
      source.readInt(),
      source.readString(),
      source.readInt(),
      source.readString(),
      source.readLong(),
      source.readString(),
      source.readString(),
      source.readString()!!,
      source.readParcelable(Uri::class.java.classLoader),
      source.readByte() != 0.toByte(),
      source.readByte() != 0.toByte(),
      source.readByte() != 0.toByte(),
      source.readInt()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    with(dest) {
      writeString(rawData)
      writeString(email)
      writeString(folder)
      writeInt(uid)
      writeString(fwdFolder)
      writeInt(fwdUid)
      writeString(name)
      writeLong(encodedSize)
      writeString(type)
      writeString(id)
      writeString(path)
      writeParcelable(uri, flags)
      writeByte(if (isProtected) 1.toByte() else 0.toByte())
      writeByte(if (isForwarded) 1.toByte() else 0.toByte())
      writeByte(if (isDecrypted) 1.toByte() else 0.toByte())
      writeInt(orderNumber)
    }
  }

  companion object {
    const val DEPTH_SEPARATOR = "/"
    const val INNER_ATTACHMENT_PREFIX = "inner_"

    @JvmField
    @Suppress("unused")
    val CREATOR: Parcelable.Creator<AttachmentInfo> = object : Parcelable.Creator<AttachmentInfo> {
      override fun createFromParcel(source: Parcel): AttachmentInfo = AttachmentInfo(source)
      override fun newArray(size: Int): Array<AttachmentInfo?> = arrayOfNulls(size)
    }
  }
}
