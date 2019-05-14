/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.model.MessageEncryptionType

/**
 * Simple POJO class which describe an outgoing message model.
 *
 * @author DenBond7
 * Date: 09.05.2017
 * Time: 11:20
 * E-mail: DenBond7@gmail.com
 */

data class OutgoingMessageInfo constructor(var subject: String? = null,
                                           var msg: String? = null,
                                           var toRecipients: List<String>? = null,
                                           var ccRecipients: List<String>? = null,
                                           var bccRecipients: List<String>? = null,
                                           var from: String? = null,
                                           var rawReplyMsg: String? = null,
                                           var atts: List<AttachmentInfo>? = null,
                                           var forwardedAtts: List<AttachmentInfo>? = null,
                                           var encryptionType: MessageEncryptionType? = null,
                                           var isForwarded: Boolean = false,
                                           var uid: Long = 0) : Parcelable {

  /**
   * Generate a list of the all recipients.
   *
   * @return A list of the all recipients
   */
  fun getAllRecipients(): List<String> {
    val recipients = mutableListOf<String>()
    toRecipients?.let { recipients.addAll(it) }
    ccRecipients?.let { recipients.addAll(it) }
    bccRecipients?.let { recipients.addAll(it) }
    return recipients
  }

  constructor(parcel: Parcel) : this(
      parcel.readString(),
      parcel.readString(),
      parcel.createStringArrayList(),
      parcel.createStringArrayList(),
      parcel.createStringArrayList(),
      parcel.readString(),
      parcel.readString(),
      mutableListOf<AttachmentInfo>().apply { parcel.readTypedList(this, AttachmentInfo.CREATOR) },
      mutableListOf<AttachmentInfo>().apply { parcel.readTypedList(this, AttachmentInfo.CREATOR) },
      parcel.readParcelable(MessageEncryptionType::class.java.classLoader),
      parcel.readByte() != 0.toByte(),
      parcel.readLong())

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    with(dest) {
      writeString(subject)
      writeString(msg)
      writeStringList(toRecipients)
      writeStringList(ccRecipients)
      writeStringList(bccRecipients)
      writeString(from)
      writeString(rawReplyMsg)
      writeTypedList(atts)
      writeTypedList(forwardedAtts)
      writeParcelable(encryptionType, flags)
      writeByte(if (isForwarded) 1.toByte() else 0.toByte())
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
