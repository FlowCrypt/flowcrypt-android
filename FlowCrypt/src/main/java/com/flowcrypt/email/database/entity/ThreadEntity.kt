/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.database.entity

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * @author Denys Bondarenko
 */
@Entity(
  tableName = ThreadEntity.TABLE_NAME,
  indices = [
    Index(
      name = "account_account_type_in_threads",
      value = ["account", "account_type"]
    ),
    Index(
      name = "threadId_account_account_type_in_threads",
      value = ["thread_id", "account", "account_type"],
      unique = true
    )
  ],
  foreignKeys = [
    ForeignKey(
      entity = AccountEntity::class,
      parentColumns = ["email", "account_type"],
      childColumns = ["account", "account_type"],
      onDelete = ForeignKey.CASCADE
    )
  ]
)
data class ThreadEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
  val account: String,
  @ColumnInfo(name = "account_type", defaultValue = "NULL") val accountType: String? = null,
  val folder: String,
  @ColumnInfo(name = "thread_id", defaultValue = "NULL") val threadId: String? = null,
  @ColumnInfo(name = "history_id", defaultValue = "NULL") val historyId: String? = null,
  @ColumnInfo(defaultValue = "NULL") val subject: String? = null,
  @ColumnInfo(defaultValue = "NULL") val addresses: String? = null,
  @ColumnInfo(name = "date", defaultValue = "NULL") val date: Long? = null,
  @ColumnInfo(name = "label_ids", defaultValue = "NULL") val labelIds: String? = null,
  @ColumnInfo(name = "messages_count", defaultValue = "0") val messagesCount: Int = 0,
  @ColumnInfo(name = "has_attachments", defaultValue = "0") val hasAttachments: Boolean = false,
  @ColumnInfo(name = "has_pgp", defaultValue = "0") val hasPgp: Boolean = false,
) {
  companion object {
    const val TABLE_NAME = "threads"
  }
}