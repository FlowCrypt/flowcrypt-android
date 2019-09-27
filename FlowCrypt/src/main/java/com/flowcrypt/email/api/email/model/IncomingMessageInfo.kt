/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.api.retrofit.response.model.node.BaseMsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.model.MessageEncryptionType
import java.util.*
import javax.mail.internet.InternetAddress

/**
 * The class which describe an incoming message model.
 *
 * @author DenBond7
 * Date: 09.05.2017
 * Time: 11:20
 * E-mail: DenBond7@gmail.com
 */

data class IncomingMessageInfo constructor(val generalMsgDetails: GeneralMessageDetails,
                                           var atts: List<AttachmentInfo>? = null,
                                           var localFolder: LocalFolder? = null,
                                           var text: String? = null,
                                           val msgBlocks: List<MsgBlock>? = null,
                                           val origMsgHeaders: String? = null,
                                           val encryptionType: MessageEncryptionType) : Parcelable {
  fun getSubject(): String? = generalMsgDetails.subject

  fun getFrom(): List<InternetAddress>? = generalMsgDetails.from

  fun getReplyTo(): List<InternetAddress>? = generalMsgDetails.replyTo

  fun getReceiveDate(): Date = Date(generalMsgDetails.receivedDate)

  fun getTo(): List<InternetAddress>? = generalMsgDetails.to

  fun getCc(): List<InternetAddress>? = generalMsgDetails.cc

  fun getHtmlMsgBlock(): MsgBlock? {
    for (part in msgBlocks!!) {
      if (part.type == MsgBlock.Type.PLAIN_HTML || part.type == MsgBlock.Type.DECRYPTED_HTML) {
        return part
      }
    }

    return null
  }

  fun getUid(): Int = generalMsgDetails.uid

  constructor(generalMsgDetails: GeneralMessageDetails, text: String?, msgBlocks: List<MsgBlock>,
              origMsgHeaders: String?, encryptionType: MessageEncryptionType) : this(
      generalMsgDetails,
      null,
      null,
      text,
      msgBlocks,
      origMsgHeaders,
      encryptionType)

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
      source.readParcelable<GeneralMessageDetails>(GeneralMessageDetails::class.java.classLoader)!!,
      source.createTypedArrayList(AttachmentInfo.CREATOR),
      source.readParcelable<LocalFolder>(LocalFolder::class.java.classLoader),
      source.readString(),
      mutableListOf<MsgBlock>().apply { source.readTypedList(this, BaseMsgBlock.CREATOR) },
      source.readString(),
      source.readParcelable(MessageEncryptionType::class.java.classLoader)!!
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeParcelable(generalMsgDetails, flags)
    writeTypedList(atts)
    writeParcelable(localFolder, flags)
    writeString(text)
    writeTypedList(msgBlocks)
    writeString(origMsgHeaders)
    writeParcelable(encryptionType, flags)
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<IncomingMessageInfo> = object : Parcelable.Creator<IncomingMessageInfo> {
      override fun createFromParcel(source: Parcel): IncomingMessageInfo = IncomingMessageInfo(source)
      override fun newArray(size: Int): Array<IncomingMessageInfo?> = arrayOfNulls(size)
    }
  }
}
