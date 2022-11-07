/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.content.Context
import android.os.Parcelable
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.retrofit.response.model.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.VerificationResult
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.model.MessageType
import jakarta.mail.internet.InternetAddress
import kotlinx.parcelize.Parcelize
import java.util.Date
import java.util.regex.Pattern

/**
 * The class which describe an incoming message model.
 *
 * @author DenBond7
 * Date: 09.05.2017
 * Time: 11:20
 * E-mail: DenBond7@gmail.com
 */
@Parcelize
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

  fun toInitializationData(
    context: Context,
    @MessageType messageType: Int,
    accountEmail: String,
    aliases: List<String>
  ): InitializationData {
    val toAddresses = arrayListOf<String>()
    val ccAddresses = arrayListOf<String>()
    val bccAddresses = arrayListOf<String>()
    var body: String? = null

    val folderType = FoldersManager.getFolderType(localFolder)
    when (messageType) {
      MessageType.REPLY -> {
        when (folderType) {
          FoldersManager.FolderType.SENT,
          FoldersManager.FolderType.OUTBOX -> {
            toAddresses.addAll(getTo().map { it.address.lowercase() })
          }

          else -> {
            toAddresses.addAll(getReplyToWithoutOwnerAddress().ifEmpty { getTo() }
              .map { it.address.lowercase() })
          }
        }
      }

      MessageType.REPLY_ALL -> {
        when (folderType) {
          FoldersManager.FolderType.SENT, FoldersManager.FolderType.OUTBOX -> {
            toAddresses.addAll(getTo().map { it.address.lowercase() })
            ccAddresses.addAll(getCc().map { it.address.lowercase() })
          }

          else -> {
            val toRecipients = getReplyToWithoutOwnerAddress().ifEmpty { getTo() }
            toAddresses.addAll(toRecipients.map { it.address.lowercase() })

            val ccSet = LinkedHashSet<InternetAddress>()

            if (getTo().isNotEmpty()) {
              ccSet.addAll(getTo().filter { !accountEmail.equals(it.address, ignoreCase = true) })

              for (alias in aliases) {
                val iterator = ccSet.iterator()
                while (iterator.hasNext()) {
                  if (iterator.next().address.equals(alias, ignoreCase = true)) {
                    iterator.remove()
                  }
                }
              }
            }

            ccSet.removeAll(toRecipients.toSet())
            ccSet.addAll(getCc().filter { !accountEmail.equals(it.address, ignoreCase = true) })

            //here we remove the owner address
            val fromAddress = msgEntity.email
            val finalCcList = ccSet.filter { !fromAddress.equals(it.address, true) }
            ccAddresses.addAll(finalCcList.map { it.address.lowercase() })
          }
        }
      }

      MessageType.FORWARD -> {
        val stringBuilder = StringBuilder()
        stringBuilder.append(
          context.getString(
            R.string.forward_template,
            getFrom().first().address ?: "",
            EmailUtil.genForwardedMsgDate(getReceiveDate()),
            getSubject(),
            prepareRecipientsLineForForwarding(getTo())
          )
        )

        if (getCc().isNotEmpty()) {
          stringBuilder.append("Cc: ")
          stringBuilder.append(prepareRecipientsLineForForwarding(getCc()))
          stringBuilder.append("\n\n")
        }

        stringBuilder.append("\n\n" + text)

        body = stringBuilder.toString()
      }

      MessageType.DRAFT -> {
        toAddresses.addAll(getTo().map { it.address.lowercase() })
        ccAddresses.addAll(getCc().map { it.address.lowercase() })
        body = text
      }

      else -> {}
    }

    return InitializationData(
      subject = prepareReplySubject(messageType),
      body = body,
      toAddresses = toAddresses,
      ccAddresses = ccAddresses,
      bccAddresses = bccAddresses
    )
  }

  private fun prepareRecipientsLineForForwarding(recipients: List<InternetAddress>?): String {
    return recipients?.joinToString { it.toString() } ?: ""
  }

  private fun prepareReplySubject(@MessageType messageType: Int): String {
    val subject = getSubject() ?: ""
    val prefix = when (messageType) {
      MessageType.REPLY, MessageType.REPLY_ALL -> "Re"
      MessageType.FORWARD -> "Fwd"
      else -> return subject
    }
    val prefixMatcher = Pattern.compile("^($prefix: )", Pattern.CASE_INSENSITIVE).matcher(subject)
    return if (prefixMatcher.find()) subject else "$prefix: $subject"
  }

  private fun hasSomePart(partType: MsgBlock.Type): Boolean {
    for (part in msgBlocks!!) {
      if (part.type == partType) {
        return true
      }
    }

    return false
  }
}
