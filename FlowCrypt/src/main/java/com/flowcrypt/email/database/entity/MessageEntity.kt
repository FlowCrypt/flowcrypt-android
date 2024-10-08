/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.content.ContentValues
import android.content.Context
import android.os.Parcelable
import android.provider.BaseColumns
import androidx.preference.PreferenceManager
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowcrypt.email.Constants
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.gmail.api.GmaiAPIMimeMessage
import com.flowcrypt.email.api.email.model.MessageFlag
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.extensions.com.google.api.services.gmail.model.hasPgp
import com.flowcrypt.email.extensions.jakarta.mail.hasPgp
import com.flowcrypt.email.extensions.kotlin.asInternetAddresses
import com.flowcrypt.email.extensions.kotlin.capitalize
import com.flowcrypt.email.extensions.kotlin.toHex
import com.flowcrypt.email.extensions.uid
import com.flowcrypt.email.ui.activity.fragment.preferences.NotificationsSettingsFragment
import com.flowcrypt.email.ui.adapter.GmailApiLabelsListAdapter
import com.flowcrypt.email.util.SharedPreferencesHelper
import com.google.android.gms.common.util.CollectionUtils
import jakarta.mail.Flags
import jakarta.mail.Message
import jakarta.mail.MessageRemovedException
import jakarta.mail.MessagingException
import jakarta.mail.Session
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.InternetAddress
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.eclipse.angus.mail.imap.IMAPFolder
import java.util.Properties

/**
 * @author Denys Bondarenko
 */
//todo-denbond7 need to add ForeignKey on account table
@Entity(
  tableName = MessageEntity.TABLE_NAME,
  indices = [
    Index(name = "email_in_messages", value = ["email"]),
    Index(name = "email_uid_folder_in_messages", value = ["email", "uid", "folder"], unique = true)
  ]
)
@Parcelize
data class MessageEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
  val email: String,
  val folder: String,
  val uid: Long,
  @ColumnInfo(name = "received_date", defaultValue = "NULL") val receivedDate: Long? = null,
  @ColumnInfo(name = "sent_date", defaultValue = "NULL") val sentDate: Long? = null,
  @ColumnInfo(name = "from_address", defaultValue = "NULL") val fromAddress: String? = null,
  @ColumnInfo(name = "to_address", defaultValue = "NULL") val toAddress: String? = null,
  @ColumnInfo(name = "cc_address", defaultValue = "NULL") val ccAddress: String? = null,
  @ColumnInfo(defaultValue = "NULL") val subject: String? = null,
  @ColumnInfo(defaultValue = "NULL") val flags: String? = null,
  @ColumnInfo(
    name = "is_message_has_attachments",
    defaultValue = "0"
  ) val hasAttachments: Boolean? = null,
  @ColumnInfo(name = "is_encrypted", defaultValue = "-1") val isEncrypted: Boolean? = null,
  @ColumnInfo(name = "is_new", defaultValue = "-1") val isNew: Boolean? = null,
  @ColumnInfo(defaultValue = "-1") val state: Int? = null,
  @ColumnInfo(name = "attachments_directory") val attachmentsDirectory: String? = null,
  @ColumnInfo(name = "error_msg", defaultValue = "NULL") val errorMsg: String? = null,
  @ColumnInfo(name = "reply_to", defaultValue = "NULL") val replyTo: String? = null,
  @ColumnInfo(name = "thread_id", defaultValue = "NULL") val threadId: String? = null,
  @ColumnInfo(name = "history_id", defaultValue = "NULL") val historyId: String? = null,
  @ColumnInfo(name = "password", defaultValue = "NULL") val password: ByteArray? = null,
  @ColumnInfo(name = "draft_id", defaultValue = "NULL") val draftId: String? = null,
  @ColumnInfo(name = "label_ids", defaultValue = "NULL") val labelIds: String? = null,
  @ColumnInfo(name = "has_pgp", defaultValue = "0") val hasPgp: Boolean? = null,
) : Parcelable {

  @IgnoredOnParcel
  @Ignore
  val from: List<InternetAddress> = fromAddress.asInternetAddresses().asList()

  @IgnoredOnParcel
  @Ignore
  val replyToAddress: List<InternetAddress> = replyTo.asInternetAddresses().asList()

  @IgnoredOnParcel
  @Ignore
  val to: List<InternetAddress> = toAddress.asInternetAddresses().asList()

  @IgnoredOnParcel
  @Ignore
  val cc: List<InternetAddress> = ccAddress.asInternetAddresses().asList()

  @IgnoredOnParcel
  @Ignore
  val msgState: MessageState = MessageState.generate(state ?: MessageState.NONE.value)

  @IgnoredOnParcel
  @Ignore
  val isSeen: Boolean = flags?.contains(MessageFlag.SEEN.value) ?: false

  @IgnoredOnParcel
  @Ignore
  val isDraft: Boolean = flags?.contains(MessageFlag.DRAFT.value) ?: false

  @IgnoredOnParcel
  @Ignore
  val isOutboxMsg: Boolean = JavaEmailConstants.FOLDER_OUTBOX.equals(folder, ignoreCase = true)

  @IgnoredOnParcel
  @Ignore
  val uidAsHEX: String = uid.toHex()

  @IgnoredOnParcel
  @Ignore
  val isPasswordProtected = password?.isNotEmpty() ?: false

  /**
   * Generate a list of the all recipients.
   *
   * @return A list of the all recipients
   */
  val allRecipients: List<String>
    get() {
      val emails = ArrayList<String>()

      for (internetAddress in to) {
        emails.add(internetAddress.address)
      }

      for (internetAddress in cc) {
        emails.add(internetAddress.address)
      }

      return emails
    }

  override fun describeContents(): Int {
    return 0
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MessageEntity

    if (id != other.id) return false
    if (email != other.email) return false
    if (folder != other.folder) return false
    if (uid != other.uid) return false
    if (receivedDate != other.receivedDate) return false
    if (sentDate != other.sentDate) return false
    if (fromAddress != other.fromAddress) return false
    if (toAddress != other.toAddress) return false
    if (ccAddress != other.ccAddress) return false
    if (subject != other.subject) return false
    if (flags != other.flags) return false
    if (hasAttachments != other.hasAttachments) return false
    if (isEncrypted != other.isEncrypted) return false
    if (isNew != other.isNew) return false
    if (state != other.state) return false
    if (attachmentsDirectory != other.attachmentsDirectory) return false
    if (errorMsg != other.errorMsg) return false
    if (replyTo != other.replyTo) return false
    if (threadId != other.threadId) return false
    if (historyId != other.historyId) return false
    if (password != null) {
      if (other.password == null) return false
      if (!password.contentEquals(other.password)) return false
    } else if (other.password != null) return false
    if (draftId != other.draftId) return false
    if (labelIds != other.labelIds) return false
    if (hasPgp != other.hasPgp) return false
    return true
  }

  override fun hashCode(): Int {
    var result = id?.hashCode() ?: 0
    result = 31 * result + email.hashCode()
    result = 31 * result + folder.hashCode()
    result = 31 * result + uid.hashCode()
    result = 31 * result + (receivedDate?.hashCode() ?: 0)
    result = 31 * result + (sentDate?.hashCode() ?: 0)
    result = 31 * result + (fromAddress?.hashCode() ?: 0)
    result = 31 * result + (toAddress?.hashCode() ?: 0)
    result = 31 * result + (ccAddress?.hashCode() ?: 0)
    result = 31 * result + (subject?.hashCode() ?: 0)
    result = 31 * result + (flags?.hashCode() ?: 0)
    result = 31 * result + (hasAttachments?.hashCode() ?: 0)
    result = 31 * result + (isEncrypted?.hashCode() ?: 0)
    result = 31 * result + (isNew?.hashCode() ?: 0)
    result = 31 * result + (state ?: 0)
    result = 31 * result + (attachmentsDirectory?.hashCode() ?: 0)
    result = 31 * result + (errorMsg?.hashCode() ?: 0)
    result = 31 * result + (replyTo?.hashCode() ?: 0)
    result = 31 * result + (threadId?.hashCode() ?: 0)
    result = 31 * result + (historyId?.hashCode() ?: 0)
    result = 31 * result + (password?.contentHashCode() ?: 0)
    result = 31 * result + (draftId?.hashCode() ?: 0)
    result = 31 * result + (labelIds?.hashCode() ?: 0)
    result = 31 * result + (hasPgp?.hashCode() ?: 0)
    return result
  }

  companion object {
    const val TABLE_NAME = "messages"
    const val LABEL_IDS_SEPARATOR = " "

    fun genMessageEntities(
      context: Context,
      email: String,
      label: String,
      folder: IMAPFolder,
      msgs: Array<Message>?,
      hasPgpAfterAdditionalSearchSet: Set<Long>,
      isNew: Boolean,
      isOnlyPgpModeEnabled: Boolean
    ): List<MessageEntity> {
      val messageEntities = mutableListOf<MessageEntity>()
      msgs?.let { msgsList ->
        val isNotificationDisabled = NotificationsSettingsFragment.NOTIFICATION_LEVEL_NEVER ==
            SharedPreferencesHelper.getString(
              PreferenceManager.getDefaultSharedPreferences(context),
              Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER, ""
            )

        val notificationForOnlyPgpMessages =
          NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY ==
              SharedPreferencesHelper.getString(
                PreferenceManager.getDefaultSharedPreferences(context),
                Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER, ""
              )

        for (msg in msgsList) {
          try {
            var isNewTemp = isNew

            if (isNotificationDisabled) {
              isNewTemp = false
            }

            val hasPgp = if (isOnlyPgpModeEnabled) {
              true
            } else {
              msg.hasPgp() || hasPgpAfterAdditionalSearchSet.contains(folder.getUID(msg))
            }

            if (notificationForOnlyPgpMessages && !hasPgp) {
              isNewTemp = false
            }

            messageEntities.add(
              genMsgEntity(
                email = email,
                label = label,
                msg = msg,
                uid = folder.getUID(msg),
                isNew = isNewTemp,
                hasPgp = hasPgp
              )
            )
          } catch (e: MessageRemovedException) {
            e.printStackTrace()
          } catch (e: AddressException) {
            e.printStackTrace()
          }
        }
      }

      return messageEntities
    }

    fun genMessageEntities(
      context: Context,
      email: String,
      label: String,
      msgsList: List<com.google.api.services.gmail.model.Message>,
      isNew: Boolean,
      onlyPgpModeEnabled: Boolean,
      draftIdsMap: Map<String, String> = emptyMap()
    ): List<MessageEntity> {
      val messageEntities = mutableListOf<MessageEntity>()
      val isNotificationDisabled = NotificationsSettingsFragment.NOTIFICATION_LEVEL_NEVER ==
          SharedPreferencesHelper.getString(
            PreferenceManager.getDefaultSharedPreferences(context),
            Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER, ""
          )

      val onlyEncryptedMsgs =
        NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY ==
            SharedPreferencesHelper.getString(
              PreferenceManager.getDefaultSharedPreferences(context),
              Constants.PREF_KEY_MESSAGES_NOTIFICATION_FILTER, ""
            )

      for (msg in msgsList) {
        try {
          var isNewTemp = isNew

          if (isNotificationDisabled) {
            isNewTemp = false
          }

          val hasPgp: Boolean = if (onlyPgpModeEnabled) {
            true
          } else {
            msg.hasPgp()
          }

          if (onlyEncryptedMsgs && !hasPgp) {
            isNewTemp = false
          }

          val mimeMessage = GmaiAPIMimeMessage(Session.getInstance(Properties()), msg)
          messageEntities.add(
            genMsgEntity(
              email = email,
              label = label,
              msg = mimeMessage,
              uid = msg.uid,
              isNew = isNewTemp,
              hasPgp = hasPgp,
              hasAttachments = GmailApiHelper.getAttsInfoFromMessagePart(msg.payload).isNotEmpty()
            ).copy(
              threadId = msg.threadId,
              historyId = msg.historyId.toString(),
              draftId = draftIdsMap[msg.id],
              labelIds = msg.labelIds?.joinToString(separator = LABEL_IDS_SEPARATOR)
            )
          )
        } catch (e: MessageRemovedException) {
          e.printStackTrace()
        } catch (e: AddressException) {
          e.printStackTrace()
        }
      }

      return messageEntities
    }

    /**
     * Prepare the content values for insert to the database. This method must be called in the
     * non-UI thread.
     *
     * @param email The email that the message linked.
     * @param label The folder label.
     * @param msg   The message which will be added to the database.
     * @param uid   The message UID.
     * @param isNew true if need to mark a given message as new
     * @return generated [ContentValues]
     * @throws MessagingException This exception may be occured when we call methods of thr
     * [Message] object
     */
    fun genMsgEntity(
      email: String,
      label: String,
      msg: Message,
      uid: Long,
      isNew: Boolean,
      isEncrypted: Boolean? = null,
      hasPgp: Boolean? = null,
      hasAttachments: Boolean? = null
    ): MessageEntity {
      return MessageEntity(email = email,
        folder = label,
        uid = uid,
        receivedDate = msg.receivedDate?.time,
        sentDate = msg.sentDate?.time,
        fromAddress = InternetAddress.toString(msg.from),
        replyTo = InternetAddress.toString(msg.replyTo),
        toAddress = InternetAddress.toString(msg.getRecipients(Message.RecipientType.TO)),
        ccAddress = InternetAddress.toString(msg.getRecipients(Message.RecipientType.CC)),
        subject = msg.subject,
        flags = msg.flags.toString().uppercase(),
        hasAttachments = hasAttachments?.let { hasAttachments } ?: EmailUtil.hasAtt(msg),
        isNew = if (!msg.flags.contains(Flags.Flag.SEEN)) {
          isNew
        } else {
          null
        },
        isEncrypted = isEncrypted,
        hasPgp = hasPgp
      )
    }

    fun genMsgEntity(
      email: String,
      label: String,
      uid: Long,
      info: OutgoingMessageInfo,
      flags: List<MessageFlag> = listOf(MessageFlag.SEEN)
    ): MessageEntity {
      return MessageEntity(
        email = email,
        folder = label,
        uid = uid,
        sentDate = System.currentTimeMillis(),
        subject = info.subject,
        flags = MessageFlag.flagsToString(flags),
        hasAttachments = !CollectionUtils.isEmpty(info.atts) || !CollectionUtils.isEmpty(info.forwardedAtts)
      )
    }

    fun generateColoredLabels(
      labelIds: List<String>?,
      labelEntities: List<LabelEntity>?,
      skippedLabels: List<String> = emptyList()
    ): List<GmailApiLabelsListAdapter.Label> {
      return labelIds.orEmpty().mapNotNull { id ->
        labelEntities?.find {
          it.name == id && (id !in skippedLabels) && (it.isCustom || JavaEmailConstants.FOLDER_INBOX == id)
        }?.let { entity ->
          val name = entity.alias.takeIf { it == JavaEmailConstants.FOLDER_INBOX }?.capitalize()
            ?: entity.alias.orEmpty()
          GmailApiLabelsListAdapter.Label(name, entity.labelColor, entity.textColor)
        }
      }.sortedBy { it.name }
    }
  }
}
