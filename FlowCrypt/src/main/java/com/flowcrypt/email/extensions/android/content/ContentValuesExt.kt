/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.android.content

import android.content.ContentValues
import android.database.Cursor

/**
 * @author Denys Bondarenko
 */
fun ContentValues.fillWithDataFromCursor(
  cursor: Cursor,
  skippedNames: Set<String> = emptySet(),
  namesToBeRenamed: Map<String, String> = emptyMap()
) {
  for (name in cursor.columnNames) {
    val columnIndex = cursor.getColumnIndex(name)
    if (columnIndex == -1 || name in skippedNames) {
      continue
    }

    val dataType = cursor.getType(columnIndex)
    val finalName = namesToBeRenamed[name] ?: name
    when (dataType) {
      Cursor.FIELD_TYPE_BLOB -> {
        put(finalName, cursor.getBlob(columnIndex))
      }

      Cursor.FIELD_TYPE_FLOAT -> {
        put(finalName, cursor.getFloat(columnIndex))
      }

      Cursor.FIELD_TYPE_INTEGER -> {
        put(finalName, cursor.getInt(columnIndex))
      }

      Cursor.FIELD_TYPE_NULL -> {
        putNull(finalName)
      }

      Cursor.FIELD_TYPE_STRING -> {
        put(finalName, cursor.getString(columnIndex))
      }
    }
  }
}