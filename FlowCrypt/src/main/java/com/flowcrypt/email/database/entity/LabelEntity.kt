/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity

import android.provider.BaseColumns
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.flowcrypt.email.api.email.model.LocalFolder
import com.google.android.gms.common.util.CollectionUtils

/**
 * @author Denis Bondarenko
 *         Date: 12/5/19
 *         Time: 5:57 PM
 *         E-mail: DenBond7@gmail.com
 */
@Entity(tableName = "imap_labels")
data class LabelEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = BaseColumns._ID) val id: Long? = null,
    val email: String,
    @ColumnInfo(name = "folder_name") val folderName: String,
    @ColumnInfo(name = "is_custom_label", defaultValue = "0") val isCustomLabel: Boolean?,
    @ColumnInfo(name = "folder_alias", defaultValue = "NULL") val folderAlias: String?,
    @ColumnInfo(name = "message_count", defaultValue = "0") val msgsCount: Int?,
    @ColumnInfo(name = "folder_attributes") val folderAttributes: String,
    @ColumnInfo(name = "folder_message_count", defaultValue = "0") val folderMessageCount: Int? = 0) {

  @Ignore
  val attributesList: List<String> = parseAttributes(folderAttributes)

  companion object {

    fun genLabel(accountName: String, localFolder: LocalFolder): LabelEntity {
      return with(localFolder) {
        LabelEntity(email = accountName,
            folderName = fullName,
            isCustomLabel = isCustom,
            folderAlias = folderAlias,
            msgsCount = msgCount,
            folderAttributes = convertAttributesToString(localFolder.attributes))
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
      return listOf(*nonNullString.split("\t".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
    }
  }
}