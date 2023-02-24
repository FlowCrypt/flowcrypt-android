/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
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
  tableName = "accounts_aliases",
  indices = [Index(
    name = "email_account_type_send_as_email_in_accounts_aliases",
    value = ["email", "account_type", "send_as_email"], unique = true
  )],
  foreignKeys = [
    ForeignKey(
      entity = AccountEntity::class, parentColumns = ["email", "account_type"],
      childColumns = ["email", "account_type"], onDelete = ForeignKey.CASCADE
    )
  ]
)
data class AccountAliasesEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
  val email: String,
  @ColumnInfo(name = "account_type") val accountType: String,
  @ColumnInfo(name = "send_as_email") val sendAsEmail: String,
  @ColumnInfo(name = "display_name", defaultValue = "NULL") val displayName: String? = null,
  @ColumnInfo(name = "is_default", defaultValue = "0") val isDefault: Boolean? = null,
  @ColumnInfo(name = "verification_status") val verificationStatus: String
)
