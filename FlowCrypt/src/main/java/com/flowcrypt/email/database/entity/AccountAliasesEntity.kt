/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowcrypt.email.util.UIUtil
import kotlinx.parcelize.IgnoredOnParcel

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
  @ColumnInfo(name = "send_as_email", defaultValue = "NULL") val sendAsEmail: String? = null,
  @ColumnInfo(name = "display_name", defaultValue = "NULL") val displayName: String? = null,
  @ColumnInfo(name = "reply_to_address", defaultValue = "NULL") val replyToAddress: String? = null,
  @ColumnInfo(name = "signature", defaultValue = "NULL") val signature: String? = null,
  @ColumnInfo(name = "is_primary", defaultValue = "NULL") val isPrimary: Boolean? = null,
  @ColumnInfo(name = "is_default", defaultValue = "NULL") val isDefault: Boolean? = null,
  @ColumnInfo(name = "treat_as_alias", defaultValue = "NULL") val treatAsAlias: Boolean? = null,
  @ColumnInfo(
    name = "verification_status",
    defaultValue = "NULL"
  ) val verificationStatus: String? = null,
){

  @Ignore
  val plainTextSignature = UIUtil.getHtmlSpannedFromText(signature)?.toString()?.trimEnd()
}
