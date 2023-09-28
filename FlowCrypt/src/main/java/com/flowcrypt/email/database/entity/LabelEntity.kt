/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.os.Parcelable
import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowcrypt.email.api.email.model.LocalFolder
import com.google.android.gms.common.util.CollectionUtils
import kotlinx.parcelize.Parcelize

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
  @ColumnInfo(name = "history_id", defaultValue = "NULL") val historyId: String? = null,
  @ColumnInfo(name = "label_color", defaultValue = "NULL") val labelColor: String? = null,
  @ColumnInfo(name = "text_color", defaultValue = "NULL") val textColor: String? = null,
  @ColumnInfo(
    name = "label_list_visibility",
    defaultValue = "labelShow"
  ) val labelListVisibility: LabelListVisibility,
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
          attributes = convertAttributesToString(localFolder.attributes),
          labelColor = labelColor,
          textColor = textColor,
          labelListVisibility = labelListVisibility,
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

  @Parcelize
  enum class LabelListVisibility constructor(val value: String) : Parcelable {
    SHOW("labelShow"),
    SHOW_IF_UNREAD("labelShowIfUnread"),
    HIDE("labelHide");

    companion object {
      fun findByValue(value: String): LabelListVisibility {
        return LabelListVisibility.values().firstOrNull { it.value == value }
          ?: throw IllegalArgumentException("Unsupported type")
      }
    }
  }
}
