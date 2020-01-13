/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source.imap

import android.content.Context
import android.database.Cursor
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.dao.source.BaseDaoSource
import java.util.*

/**
 * This class describes the structure of IMAP labels for different accounts and methods which
 * will be used to manipulate this data.
 *
 * @author DenBond7
 * Date: 14.06.2017
 * Time: 15:59
 * E-mail: DenBond7@gmail.com
 */

class ImapLabelsDaoSource : BaseDaoSource() {

  override val tableName: String = TABLE_NAME_IMAP_LABELS

  /**
   * Generate a [LocalFolder] object from the current cursor position.
   *
   * @param cursor The [Cursor] which contains information about [LocalFolder].
   * @return A generated [LocalFolder].
   */
  fun getFolder(cursor: Cursor): LocalFolder {
    return LocalFolder(
        cursor.getString(cursor.getColumnIndex(COL_EMAIL)),
        cursor.getString(cursor.getColumnIndex(COL_FOLDER_NAME)),
        cursor.getString(cursor.getColumnIndex(COL_FOLDER_ALIAS)),
        parseAttributes(cursor.getString(cursor.getColumnIndex(COL_FOLDER_ATTRIBUTES))),
        cursor.getInt(cursor.getColumnIndex(COL_IS_CUSTOM_LABEL)) == 1,
        cursor.getInt(cursor.getColumnIndex(COL_MESSAGE_COUNT)), null
    )
  }

  /**
   * Get a [LocalFolder] from the database by an email and a name.
   *
   * @param email      The email of the [LocalFolder].
   * @param folderName The folder name.
   * @return [LocalFolder] or null if such folder not found.
   */
  fun getFolder(context: Context, email: String, folderName: String): LocalFolder? {
    val contentResolver = context.contentResolver
    val cursor = contentResolver.query(baseContentUri, null, COL_EMAIL + " = ?" + " AND " +
        COL_FOLDER_NAME + " = ?", arrayOf(email, folderName), null)

    var localFolder: LocalFolder? = null

    if (cursor != null) {
      while (cursor.moveToNext()) {
        localFolder = getFolder(cursor)
      }
      cursor.close()
    }

    return localFolder
  }

  private fun parseAttributes(attributesAsString: String?): List<String>? {
    return if (attributesAsString != null && attributesAsString.isNotEmpty()) {
      Arrays.asList(*attributesAsString.split("\t".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
    } else {
      null
    }
  }

  companion object {
    const val TABLE_NAME_IMAP_LABELS = "imap_labels"

    const val COL_EMAIL = "email"
    const val COL_FOLDER_NAME = "folder_name"
    const val COL_FOLDER_ALIAS = "folder_alias"
    const val COL_MESSAGE_COUNT = "message_count"
    const val COL_IS_CUSTOM_LABEL = "is_custom_label"
    const val COL_FOLDER_ATTRIBUTES = "folder_attributes"
    const val COL_FOLDER_MESSAGE_COUNT = "folder_message_count"
  }
}
