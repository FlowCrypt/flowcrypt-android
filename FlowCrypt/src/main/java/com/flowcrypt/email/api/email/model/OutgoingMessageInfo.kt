/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcelable
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import jakarta.mail.internet.InternetAddress
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * Simple POJO class which describe an outgoing message model.
 *
 * @author Denys Bondarenko
 */
@Parcelize
data class OutgoingMessageInfo constructor(
  val account: String? = null,
  val subject: String? = null,
  val msg: String? = null,
  val toRecipients: List<InternetAddress>? = null,
  val ccRecipients: List<InternetAddress>? = null,
  val bccRecipients: List<InternetAddress>? = null,
  val from: InternetAddress? = null,
  val atts: List<AttachmentInfo>? = null,
  val forwardedAtts: List<AttachmentInfo>? = null,
  val encryptionType: MessageEncryptionType? = null,
  @MessageType val messageType: Int = MessageType.NEW,
  val replyToMsgEntity: MessageEntity? = null,
  val uid: Long = 0,
  val password: CharArray? = null
) : Parcelable {

  @IgnoredOnParcel
  val isPasswordProtected = password?.isNotEmpty()

  /**
   * Generate a list of the all recipients.
   *
   * @return A list of the all recipients
   */
  fun getAllRecipients(): List<String> {
    return getPublicRecipients() + getProtectedRecipients()
  }

  fun getPublicRecipients(): List<String> {
    val publicRecipients = mutableListOf<String>()
    toRecipients?.let { publicRecipients.addAll(it.map { address -> address.address }) }
    ccRecipients?.let { publicRecipients.addAll(it.map { address -> address.address }) }
    return publicRecipients
  }

  fun getProtectedRecipients(): List<String> {
    val allRecipients = mutableListOf<String>()
    bccRecipients?.let { allRecipients.addAll(it.map { address -> address.address }) }
    return allRecipients
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as OutgoingMessageInfo

    if (account != other.account) return false
    if (subject != other.subject) return false
    if (msg != other.msg) return false
    if (toRecipients != other.toRecipients) return false
    if (ccRecipients != other.ccRecipients) return false
    if (bccRecipients != other.bccRecipients) return false
    if (from != other.from) return false
    if (atts != other.atts) return false
    if (forwardedAtts != other.forwardedAtts) return false
    if (encryptionType != other.encryptionType) return false
    if (messageType != other.messageType) return false
    if (replyToMsgEntity != other.replyToMsgEntity) return false
    if (uid != other.uid) return false
    if (password != null) {
      if (other.password == null) return false
      if (!password.contentEquals(other.password)) return false
    } else if (other.password != null) return false
    if (isPasswordProtected != other.isPasswordProtected) return false

    return true
  }

  override fun hashCode(): Int {
    var result = account?.hashCode() ?: 0
    result = 31 * result + (subject?.hashCode() ?: 0)
    result = 31 * result + (msg?.hashCode() ?: 0)
    result = 31 * result + (toRecipients?.hashCode() ?: 0)
    result = 31 * result + (ccRecipients?.hashCode() ?: 0)
    result = 31 * result + (bccRecipients?.hashCode() ?: 0)
    result = 31 * result + (from?.hashCode() ?: 0)
    result = 31 * result + (atts?.hashCode() ?: 0)
    result = 31 * result + (forwardedAtts?.hashCode() ?: 0)
    result = 31 * result + (encryptionType?.hashCode() ?: 0)
    result = 31 * result + messageType
    result = 31 * result + (replyToMsgEntity?.hashCode() ?: 0)
    result = 31 * result + uid.hashCode()
    result = 31 * result + (password?.contentHashCode() ?: 0)
    result = 31 * result + (isPasswordProtected?.hashCode() ?: 0)
    return result
  }
}
