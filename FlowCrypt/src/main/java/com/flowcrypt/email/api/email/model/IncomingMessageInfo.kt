/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.response.model.GenericMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.VerificationResult
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.model.MessageEncryptionType
import jakarta.mail.internet.InternetAddress
import java.util.Date

/**
 * The class which describe an incoming message model.
 *
 * @author DenBond7
 * Date: 09.05.2017
 * Time: 11:20
 * E-mail: DenBond7@gmail.com
 */

data class IncomingMessageInfo constructor(
  val msgEntity: MessageEntity,
  var atts: List<AttachmentInfo>? = null,
  var localFolder: LocalFolder? = null,
  var text: String? = null,
  var inlineSubject: String? = null,
  val msgBlocks: List<@JvmSuppressWildcards MsgBlock>? = null,
  val encryptionType: MessageEncryptionType,
  val verificationResult: VerificationResult
) : Parcelable {
  fun getSubject(): String? = msgEntity.subject

  fun getFrom(): List<InternetAddress> = msgEntity.from

  fun getReplyTo(): List<InternetAddress> = msgEntity.replyToAddress

  fun getReplyToWithoutOwnerAddress(): List<InternetAddress> = getReplyTo().filter {
    !it.address.equals(msgEntity.email, true)
  }

  fun getReceiveDate(): Date = Date(msgEntity.receivedDate ?: 0)

  fun getTo(): List<InternetAddress> = msgEntity.to

  fun getCc(): List<InternetAddress> = msgEntity.cc

  fun getHtmlMsgBlock(): MsgBlock? {
    for (part in msgBlocks ?: emptyList()) {
      if (part.type == MsgBlock.Type.PLAIN_HTML || part.type == MsgBlock.Type.DECRYPTED_HTML) {
        return part
      }
    }

    return null
  }

  fun getUid(): Int = msgEntity.uid.toInt()

  constructor(
    msgEntity: MessageEntity, text: String?, subject: String?, msgBlocks: List<MsgBlock>,
    encryptionType: MessageEncryptionType, verificationResult: VerificationResult
  ) : this(
    msgEntity,
    null,
    null,
    text,
    subject,
    msgBlocks,
    encryptionType,
    verificationResult
  )

  fun hasHtmlText(): Boolean {
    return hasSomePart(MsgBlock.Type.PLAIN_HTML) || hasSomePart(MsgBlock.Type.DECRYPTED_HTML)
  }

  fun hasPlainText(): Boolean {
    return hasSomePart(MsgBlock.Type.PLAIN_TEXT)
  }

  private fun hasSomePart(partType: MsgBlock.Type): Boolean {
    for (part in msgBlocks!!) {
      if (part.type == partType) {
        return true
      }
    }

    return false
  }

  constructor(source: Parcel) : this(
    source.readParcelable<MessageEntity>(MessageEntity::class.java.classLoader)!!,
    source.createTypedArrayList(AttachmentInfo.CREATOR),
    source.readParcelable<LocalFolder>(LocalFolder::class.java.classLoader),
    source.readString(),
    source.readString(),
    mutableListOf<MsgBlock>().apply { source.readTypedList(this, GenericMsgBlock.CREATOR) },
    source.readParcelable(MessageEncryptionType::class.java.classLoader)!!,
    source.readParcelable(VerificationResult::class.java.classLoader)!!
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeParcelable(msgEntity, flags)
    writeTypedList(atts)
    writeParcelable(localFolder, flags)
    writeString(text)
    writeString(inlineSubject)
    writeTypedList(msgBlocks)
    writeParcelable(encryptionType, flags)
    writeParcelable(verificationResult, flags)
  }

  companion object {
    @JvmField
    @Suppress("unused")
    val CREATOR: Parcelable.Creator<IncomingMessageInfo> =
      object : Parcelable.Creator<IncomingMessageInfo> {
        override fun createFromParcel(source: Parcel): IncomingMessageInfo =
          IncomingMessageInfo(source)

        override fun newArray(size: Int): Array<IncomingMessageInfo?> = arrayOfNulls(size)
      }
  }
}
