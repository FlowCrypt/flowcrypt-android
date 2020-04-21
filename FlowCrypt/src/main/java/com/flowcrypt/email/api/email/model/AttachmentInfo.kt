/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
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
                                      var type: String = Constants.MIME_TYPE_BINARY_DATA,
                                      var id: String? = null,
                                      var path: String = "0",
                                      var uri: Uri? = null,
                                      var isProtected: Boolean = false,
                                      var isForwarded: Boolean = false,
                                      var isDecrypted: Boolean = false,
                                      var isEncryptionAllowed: Boolean = true,
                                      var orderNumber: Int = 0) : Parcelable {

  val uniqueStringId: String
    get() = uid.toString() + "_" + id + "_" + path

  fun copy(newFolder: String, newUid: Int): AttachmentInfo {
    return copy(folder = newFolder, uid = newUid, fwdFolder = this.folder, fwdUid = this.uid, orderNumber = 0)
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
      source.readString()!!,
      source.readString(),
      source.readString()!!,
      source.readParcelable(Uri::class.java.classLoader),
      source.readByte() != 0.toByte(),
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
      writeByte(if (isEncryptionAllowed) 1.toByte() else 0.toByte())
      writeInt(orderNumber)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AttachmentInfo

    if (rawData != other.rawData) return false
    if (email != other.email) return false
    if (folder != other.folder) return false
    if (uid != other.uid) return false
    if (fwdFolder != other.fwdFolder) return false
    if (fwdUid != other.fwdUid) return false
    if (name != other.name) return false
    if (encodedSize != other.encodedSize) return false
    if (type != other.type) return false
    if (id != other.id) return false
    if (path != other.path) return false
    if (uri?.toString() != other.uri?.toString()) return false
    if (isProtected != other.isProtected) return false
    if (isForwarded != other.isForwarded) return false
    if (isDecrypted != other.isDecrypted) return false
    if (isEncryptionAllowed != other.isEncryptionAllowed) return false
    if (orderNumber != other.orderNumber) return false

    return true
  }

  override fun hashCode(): Int {
    var result = rawData?.hashCode() ?: 0
    result = 31 * result + (email?.hashCode() ?: 0)
    result = 31 * result + (folder?.hashCode() ?: 0)
    result = 31 * result + uid
    result = 31 * result + (fwdFolder?.hashCode() ?: 0)
    result = 31 * result + fwdUid
    result = 31 * result + (name?.hashCode() ?: 0)
    result = 31 * result + encodedSize.hashCode()
    result = 31 * result + type.hashCode()
    result = 31 * result + (id?.hashCode() ?: 0)
    result = 31 * result + path.hashCode()
    result = 31 * result + (uri?.hashCode() ?: 0)
    result = 31 * result + isProtected.hashCode()
    result = 31 * result + isForwarded.hashCode()
    result = 31 * result + isDecrypted.hashCode()
    result = 31 * result + isEncryptionAllowed.hashCode()
    result = 31 * result + orderNumber
    return result
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
