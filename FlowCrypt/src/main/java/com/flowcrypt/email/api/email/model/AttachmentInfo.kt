/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.net.Uri
import android.os.Parcelable
import android.webkit.MimeTypeMap
import com.flowcrypt.email.Constants
import com.flowcrypt.email.core.msg.RawBlockParser
import com.flowcrypt.email.extensions.kotlin.asContentTypeOrNull
import com.flowcrypt.email.providers.EmbeddedAttachmentsProvider
import com.flowcrypt.email.security.SecurityUtils
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.apache.commons.io.FilenameUtils

/**
 * Simple POJO which defines information about email attachments.
 *
 * @author Denys Bondarenko
 */
@Parcelize
data class AttachmentInfo(
  @Expose val rawData: ByteArray? = null,
  @Expose val email: String? = null,
  @Expose val folder: String? = null,
  @Expose val uid: Long = 0,
  @Expose val fwdFolder: String? = null,
  @Expose val fwdUid: Long = 0,
  @Expose val name: String? = null,
  @Expose val encodedSize: Long = 0,
  @Expose val type: String = Constants.MIME_TYPE_BINARY_DATA,
  @Expose val id: String? = null,
  @Expose val path: String = "0",
  @Expose val uri: Uri? = null,
  @Expose val isProtected: Boolean = false,
  @Expose @SerializedName(
    value = "isLazyForwarded",
    alternate = ["isForwarded"]
  ) val isLazyForwarded: Boolean = false,
  @Expose val isDecrypted: Boolean = false,
  @Expose val isEncryptionAllowed: Boolean = true,
  @Expose val orderNumber: Int = 0,
  @Expose val decryptWhenForward: Boolean = false,
) : Parcelable {

  @IgnoredOnParcel
  val isEmbedded = EmbeddedAttachmentsProvider.Cache.AUTHORITY.equals(
    uri?.authority,
    ignoreCase = true
  )

  @IgnoredOnParcel
  val isPossiblyEncrypted = RawBlockParser.ENCRYPTED_FILE_REGEX.containsMatchIn(name ?: "")

  @IgnoredOnParcel
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
    if (isLazyForwarded != other.isLazyForwarded) return false
    if (isDecrypted != other.isDecrypted) return false
    if (isEncryptionAllowed != other.isEncryptionAllowed) return false
    if (orderNumber != other.orderNumber) return false
    return decryptWhenForward == other.decryptWhenForward
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
    result = 31 * result + isLazyForwarded.hashCode()
    result = 31 * result + isDecrypted.hashCode()
    result = 31 * result + isEncryptionAllowed.hashCode()
    result = 31 * result + orderNumber
    result = 31 * result + decryptWhenForward.hashCode()
    return result
  }

  fun getSafeName(): String {
    return SecurityUtils.sanitizeFileName(name)
  }

  fun getAndroidMimeType(): String? {
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(FilenameUtils.getExtension(name))
  }

  fun isHidden() = when {
    //https://github.com/FlowCrypt/flowcrypt-android/issues/1475
    name.isNullOrEmpty() && type.lowercase() == "application/pgp-encrypted; name=\"\"" -> true

    //https://github.com/FlowCrypt/flowcrypt-android/issues/2540
    "application/pgp-signature" == type.asContentTypeOrNull()?.baseType -> true

    else -> false
  }

  fun isEmbeddedAndPossiblyEncrypted(): Boolean {
    return isEmbedded && isPossiblyEncrypted
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
    var isLazyForwarded: Boolean = false,
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
        isLazyForwarded = isLazyForwarded,
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

    val DANGEROUS_FILE_EXTENSIONS = arrayOf(
      "apk",
      "appx",
      "appxbundle"
    )
  }
}
