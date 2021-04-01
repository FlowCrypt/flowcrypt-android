/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import javax.mail.internet.InternetAddress

/**
 * Simple POJO class which describe an outgoing message model.
 *
 * @author DenBond7
 * Date: 09.05.2017
 * Time: 11:20
 * E-mail: DenBond7@gmail.com
 */
data class OutgoingMessageInfo constructor(
    val account: String,
    val subject: String,
    val msg: String? = null,
    val toRecipients: List<InternetAddress>,
    val ccRecipients: List<InternetAddress>? = null,
    val bccRecipients: List<InternetAddress>? = null,
    val from: String,
    val atts: List<AttachmentInfo>? = null,
    val forwardedAtts: List<AttachmentInfo>? = null,
    val encryptionType: MessageEncryptionType,
    val messageType: MessageType,
    val replyToMsgEntity: MessageEntity? = null,
    val uid: Long = 0) : Parcelable {

  /**
   * Generate a list of the all recipients.
   *
   * @return A list of the all recipients
   */
  fun getAllRecipients(): List<String> {
    val allRecipients = mutableListOf<String>()
    allRecipients.addAll(toRecipients.map { address -> address.address })
    ccRecipients?.let { allRecipients.addAll(it.map { address -> address.address }) }
    bccRecipients?.let { allRecipients.addAll(it.map { address -> address.address }) }
    return allRecipients
  }

  @Suppress("UNCHECKED_CAST")
  constructor(parcel: Parcel) : this(
      parcel.readString()!!,
      parcel.readString()!!,
      parcel.readString(),
      parcel.readValue(InternetAddress::class.java.classLoader) as List<InternetAddress>,
      parcel.readValue(InternetAddress::class.java.classLoader) as List<InternetAddress>?,
      parcel.readValue(InternetAddress::class.java.classLoader) as List<InternetAddress>?,
      parcel.readString()!!,
      parcel.readValue(AttachmentInfo::class.java.classLoader) as List<AttachmentInfo>?,
      parcel.readValue(AttachmentInfo::class.java.classLoader) as List<AttachmentInfo>?,
      parcel.readParcelable<MessageEncryptionType>(MessageEncryptionType::class.java.classLoader)!!,
      parcel.readParcelable<MessageType>(MessageType::class.java.classLoader)!!,
      parcel.readParcelable<MessageEntity>(MessageEntity::class.java.classLoader),
      parcel.readLong())

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
      writeString(from)
      writeValue(atts)
      writeValue(forwardedAtts)
      writeParcelable(encryptionType, flags)
      writeParcelable(messageType, flags)
      writeParcelable(replyToMsgEntity, flags)
      writeLong(uid)
    }
  }

  companion object {
    @JvmField
    @Suppress("unused")
    val CREATOR: Parcelable.Creator<OutgoingMessageInfo> = object : Parcelable.Creator<OutgoingMessageInfo> {
      override fun createFromParcel(source: Parcel): OutgoingMessageInfo = OutgoingMessageInfo(source)
      override fun newArray(size: Int): Array<OutgoingMessageInfo?> = arrayOfNulls(size)
    }
  }
}
