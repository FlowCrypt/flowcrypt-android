/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt
import com.flowcrypt.email.extensions.android.os.readSerializableViaExt
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import jakarta.mail.internet.InternetAddress

/**
 * Simple POJO class which describe an outgoing message model.
 *
 * @author DenBond7
 * Date: 09.05.2017
 * Time: 11:20
 * E-mail: DenBond7@gmail.com
 */
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

  val isPasswordProtected = password?.isNotEmpty()

  /**
   * Generate a list of the all recipients.
   *
   * @return A list of the all recipients
   */
  fun getAllRecipients(): List<String> {
    val allRecipients = mutableListOf<String>()
    toRecipients?.let { allRecipients.addAll(it.map { address -> address.address }) }
    ccRecipients?.let { allRecipients.addAll(it.map { address -> address.address }) }
    bccRecipients?.let { allRecipients.addAll(it.map { address -> address.address }) }
    return allRecipients
  }

  @Suppress("UNCHECKED_CAST")
  constructor(parcel: Parcel) : this(
    parcel.readString(),
    parcel.readString(),
    parcel.readString(),
    parcel.readValue(InternetAddress::class.java.classLoader) as List<InternetAddress>?,
    parcel.readValue(InternetAddress::class.java.classLoader) as List<InternetAddress>?,
    parcel.readValue(InternetAddress::class.java.classLoader) as List<InternetAddress>?,
    parcel.readSerializableViaExt(InternetAddress::class.java),
    parcel.readValue(AttachmentInfo::class.java.classLoader) as List<AttachmentInfo>?,
    parcel.readValue(AttachmentInfo::class.java.classLoader) as List<AttachmentInfo>?,
    parcel.readParcelableViaExt(MessageEncryptionType::class.java)!!,
    parcel.readInt(),
    parcel.readParcelableViaExt(MessageEntity::class.java),
    parcel.readLong(),
    parcel.createCharArray()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    with(dest) {
      writeString(account)
      writeString(subject)
      writeString(msg)
      writeValue(toRecipients)
      writeValue(ccRecipients)
      writeValue(bccRecipients)
      writeSerializable(from)
      writeValue(atts)
      writeValue(forwardedAtts)
      writeParcelable(encryptionType, flags)
      writeInt(messageType)
      writeParcelable(replyToMsgEntity, flags)
      writeLong(uid)
      writeCharArray(password)
    }
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

  companion object CREATOR : Parcelable.Creator<OutgoingMessageInfo> {
    override fun createFromParcel(parcel: Parcel) = OutgoingMessageInfo(parcel)
    override fun newArray(size: Int): Array<OutgoingMessageInfo?> = arrayOfNulls(size)
  }
}
