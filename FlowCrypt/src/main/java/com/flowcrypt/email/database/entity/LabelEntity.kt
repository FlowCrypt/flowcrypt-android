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
import com.flowcrypt.email.api.email.model.LocalFolder
import com.google.android.gms.common.util.CollectionUtils

/**
 * @author Denys Bondarenko
 */
@Entity(
  tableName = LabelEntity.TABLE_NAME,
  indices = [
    Index(
      name = "email_account_type_name_in_labels",
      value = ["email", "account_type", "name"],
      unique = true
    )
  ],
  foreignKeys = [
    ForeignKey(
      entity = AccountEntity::class, parentColumns = ["email", "account_type"],
      childColumns = ["email", "account_type"], onDelete = ForeignKey.CASCADE
    )
  ]
)
data class LabelEntity(
  @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
  val email: String,
  @ColumnInfo(name = "account_type", defaultValue = "NULL") val accountType: String? = null,
  @ColumnInfo(name = "name") val name: String,
  @ColumnInfo(name = "alias", defaultValue = "NULL") val alias: String?,
  @ColumnInfo(name = "is_custom", defaultValue = "0") val isCustom: Boolean = false,
  @ColumnInfo(name = "messages_total", defaultValue = "0") val messagesTotal: Int = 0,
  @ColumnInfo(name = "message_unread", defaultValue = "0") val messagesUnread: Int = 0,
  @ColumnInfo(name = "attributes", defaultValue = "NULL") val attributes: String? = null,
  @ColumnInfo(name = "next_page_token", defaultValue = "NULL") val nextPageToken: String? = null,
  @ColumnInfo(name = "history_id", defaultValue = "NULL") val historyId: String? = null
) {

  @Ignore
  val attributesList: List<String> = parseAttributes(attributes)

  companion object {
    const val TABLE_NAME = "labels"
    fun genLabel(accountEntity: AccountEntity, localFolder: LocalFolder): LabelEntity {
      return with(localFolder) {
        LabelEntity(
          email = accountEntity.email,
          accountType = accountEntity.accountType,
          name = fullName,
          isCustom = isCustom,
          alias = folderAlias,
          messagesTotal = msgCount,
          attributes = convertAttributesToString(localFolder.attributes)
        )
      }
    }

    private fun convertAttributesToString(attributes: List<String>?): String {
      return if (!CollectionUtils.isEmpty(attributes)) {
        val result = StringBuilder()
        for (attribute in attributes!!) {
          result.append(attribute).append("\t")
        }

        result.toString()
      } else {
        ""
      }
    }

    private fun parseAttributes(attributesAsString: String?): List<String> {
      val nonNullString = attributesAsString ?: return emptyList()
      return listOf(*nonNullString.split("\t".toRegex()).dropLastWhile { it.isEmpty() }
        .toTypedArray())
    }
  }
}
