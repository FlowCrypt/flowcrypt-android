/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.net.Uri
import android.os.Parcelable
import com.flowcrypt.email.Constants
import com.flowcrypt.email.core.msg.RawBlockParser
import com.flowcrypt.email.security.SecurityUtils
import kotlinx.parcelize.Parcelize

/**
 * Simple POJO which defines information about email attachments.
 *
 * @author Denys Bondarenko
 */
@Parcelize
data class AttachmentInfo constructor(
  val rawData: ByteArray? = null,
  val email: String? = null,
  val folder: String? = null,
  val uid: Long = 0,
  val fwdFolder: String? = null,
  val fwdUid: Long = 0,
  val name: String? = null,
  val encodedSize: Long = 0,
  val type: String = Constants.MIME_TYPE_BINARY_DATA,
  val id: String? = null,
  val path: String = "0",
  val uri: Uri? = null,
  val isProtected: Boolean = false,
  val isForwarded: Boolean = false,
  val isDecrypted: Boolean = false,
  val isEncryptionAllowed: Boolean = true,
  val orderNumber: Int = 0,
  val decryptWhenForward: Boolean = false,
) : Parcelable {

  val uniqueStringId: String
    get() = uid.toString() + "_" + id + "_" + path

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AttachmentInfo

    if (rawData != null) {
      if (other.rawData == null) return false
      if (!rawData.contentEquals(other.rawData)) return false
    } else if (other.rawData != null) return false
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
    if (uri != other.uri) return false
    if (isProtected != other.isProtected) return false
    if (isForwarded != other.isForwarded) return false
    if (isDecrypted != other.isDecrypted) return false
    if (isEncryptionAllowed != other.isEncryptionAllowed) return false
    if (orderNumber != other.orderNumber) return false

    return true
  }

  override fun hashCode(): Int {
    var result = rawData?.contentHashCode() ?: 0
    result = 31 * result + (email?.hashCode() ?: 0)
    result = 31 * result + (folder?.hashCode() ?: 0)
    result = 31 * result + uid.hashCode()
    result = 31 * result + (fwdFolder?.hashCode() ?: 0)
    result = 31 * result + fwdUid.hashCode()
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

  fun getSafeName(): String {
    return SecurityUtils.sanitizeFileName(name)
  }

  fun isHidden() =
    name.isNullOrEmpty() && type.lowercase() == "application/pgp-encrypted; name=\"\""

  fun isPossiblyEncrypted(): Boolean {
    return RawBlockParser.ENCRYPTED_FILE_REGEX.containsMatchIn(name ?: "")
  }

  @Suppress("ArrayInDataClass")
  data class Builder(
    var rawData: ByteArray? = null,
    var email: String? = null,
    var folder: String? = null,
    var uid: Long = 0,
    var fwdFolder: String? = null,
    var fwdUid: Long = 0,
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
    var orderNumber: Int = 0,
    var decryptWhenForward: Boolean = false,
  ) {
    fun build(): AttachmentInfo {
      return AttachmentInfo(
        rawData = rawData,
        email = email,
        folder = folder,
        uid = uid,
        fwdFolder = fwdFolder,
        fwdUid = fwdUid,
        name = name,
        encodedSize = encodedSize,
        type = type,
        id = id,
        path = path,
        uri = uri,
        isProtected = isProtected,
        isForwarded = isForwarded,
        isDecrypted = isDecrypted,
        isEncryptionAllowed = isEncryptionAllowed,
        orderNumber = orderNumber,
        decryptWhenForward = decryptWhenForward,
      )
    }
  }

  companion object {
    const val DEPTH_SEPARATOR = "/"
    const val INNER_ATTACHMENT_PREFIX = "inner_"
  }
}
