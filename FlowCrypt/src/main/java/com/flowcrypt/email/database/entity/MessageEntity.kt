/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * @author Denis Bondarenko
 *         Date: 12/5/19
 *         Time: 6:30 PM
 *         E-mail: DenBond7@gmail.com
 */
@Entity(tableName = "messages",
    indices = [
      Index(name = "email_in_messages", value = ["email"]),
      Index(name = "email_uid_folder_in_messages", value = ["email", "uid", "folder"], unique = true)
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long?,
    val email: String,
    val folder: String,
    val uid: Long,
    @ColumnInfo(name = "received_date", defaultValue = "NULL") val receivedDate: Long?,
    @ColumnInfo(name = "sent_date", defaultValue = "NULL") val sentDate: Long?,
    @ColumnInfo(name = "from_address", defaultValue = "NULL") val fromAddress: String?,
    @ColumnInfo(name = "to_address", defaultValue = "NULL") val to_address: String?,
    @ColumnInfo(name = "cc_address", defaultValue = "NULL") val ccAddress: String?,
    @ColumnInfo(defaultValue = "NULL") val subject: String?,
    @ColumnInfo(defaultValue = "NULL") val flags: String?,
    @ColumnInfo(name = "raw_message_without_attachments", defaultValue = "NULL") val rawMessageWithoutAttachments: String?,
    @ColumnInfo(name = "is_message_has_attachments", defaultValue = "0") val isMessageHasAttachments: Boolean?,
    @ColumnInfo(name = "is_encrypted", defaultValue = "-1") val isEncrypted: Boolean?,
    @ColumnInfo(name = "is_new", defaultValue = "-1") val isNew: Boolean?,
    @ColumnInfo(defaultValue = "-1") val state: Int?,
    @ColumnInfo(name = "attachments_directory") val attachmentsDirectory: String?,
    @ColumnInfo(name = "error_msg", defaultValue = "NULL") val errorMsg: String?,
    @ColumnInfo(name = "reply_to", defaultValue = "NULL") val replyTo: String?
)