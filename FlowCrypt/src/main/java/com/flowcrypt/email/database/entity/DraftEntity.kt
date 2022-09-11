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

@Entity(
  tableName = "drafts",
  indices = [
    Index(
      name = "account_account_type_in_drafts",
      value = ["account", "account_type"]
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
data class DraftEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
  val account: String,
  @ColumnInfo(name = "account_type") val accountType: String,
  @ColumnInfo(name = "draft_id", defaultValue = "NULL") val draftId: String?,
)
