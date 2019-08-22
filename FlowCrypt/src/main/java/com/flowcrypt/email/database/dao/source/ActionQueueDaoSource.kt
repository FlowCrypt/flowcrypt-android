/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.BaseColumns
import com.flowcrypt.email.service.actionqueue.actions.Action
import com.flowcrypt.email.service.actionqueue.actions.Action.Type
import com.flowcrypt.email.util.google.gson.ActionJsonDeserializer
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.util.*

/**
 * This object describes a logic of working with [Action] in the local database.
 *
 * @author Denis Bondarenko
 * Date: 30.01.2018
 * Time: 10:00
 * E-mail: DenBond7@gmail.com
 */

class ActionQueueDaoSource : BaseDaoSource() {

  private val gson: Gson = GsonBuilder().registerTypeAdapter(Action::class.java, ActionJsonDeserializer()).create()

  override val tableName: String = TABLE_NAME_ACTION_QUEUE

  /**
   * Save information about an [Action] to the database;
   *
   * @param context Interface to global information about an application environment;
   * @param action  An input [Action].
   * @return The created [Uri] or null;
   */
  fun addAction(context: Context, action: Action?): Uri? {
    val contentResolver = context.contentResolver
    if (action != null && contentResolver != null) {
      val contentValues = generateContentValues(action) ?: return null

      return contentResolver.insert(baseContentUri, contentValues)
    } else
      return null
  }

  /**
   * Save information about an [Action] to the database;
   *
   * @param sqLiteDatabase An instance of the local database;
   * @param action         An input [Action].
   * @return the row ID of the newly inserted row, or -1 if an error occurred;
   */
  fun addAction(sqLiteDatabase: SQLiteDatabase?, action: Action?): Long {
    if (action != null && sqLiteDatabase != null) {
      val contentValues = generateContentValues(action) ?: return -1

      return sqLiteDatabase.insert(TABLE_NAME_ACTION_QUEUE, null, contentValues)
    } else
      return -1
  }

  /**
   * This method add rows per single transaction.
   *
   * @param context Interface to global information about an application environment.
   * @param actions The list of [Action] objects.
   */
  fun addActions(context: Context, actions: List<Action>?): Int {
    return if (actions != null && actions.isNotEmpty()) {
      val contentResolver = context.contentResolver
      val contentValuesArray = arrayOfNulls<ContentValues>(actions.size)

      for (i in actions.indices) {
        val action = actions[i]
        contentValuesArray[i] = generateContentValues(action)
      }

      contentResolver.bulkInsert(baseContentUri, contentValuesArray)
    } else
      0
  }

  /**
   * Get the list of [Action] object from the local database for some email.
   *
   * @param context Interface to global information about an application environment.
   * @param account An account information.
   * @return The list of [Action];
   */
  fun getActions(context: Context, account: AccountDao?): List<Action> {
    val actions = ArrayList<Action>()
    if (account != null) {
      val selection = "$COL_EMAIL = ? OR $COL_EMAIL = ?"
      val selectionArgs = arrayOf(account.email, Action.USER_SYSTEM)
      val cursor = context.contentResolver.query(baseContentUri, null, selection, selectionArgs, null)

      if (cursor != null) {
        while (cursor.moveToNext()) {
          getCurrentAction(cursor)?.let { actions.add(it) }
        }
      }

      cursor?.close()
    }

    return actions
  }

  /**
   * Get the list of [Action] object from the local database for some email using some [Type].
   *
   * @param context    Interface to global information about an application environment.
   * @param account    An account information.
   * @param type An action type.
   * @return The list of [Action];
   */
  fun getActionsByType(context: Context, account: AccountDao?, type: Type?): List<Action> {
    val actions = ArrayList<Action>()
    if (account != null && type != null) {
      val selection = "$COL_EMAIL = ? AND $COL_ACTION_TYPE = ?"
      val selectionArgs = arrayOf(account.email, type.value)
      val cursor = context.contentResolver.query(baseContentUri, null, selection, selectionArgs, null)

      if (cursor != null) {
        while (cursor.moveToNext()) {
          getCurrentAction(cursor)?.let { actions.add(it) }
        }
      }

      cursor?.close()
    }

    return actions
  }

  /**
   * Delete an [Action] from the database.
   *
   * @param context Interface to global information about an application environment.
   * @param action  An input [Action] which will be deleted.
   * @return The count of deleted rows. Will be 1 if information about [Action] was
   * deleted or -1 otherwise.
   */
  fun deleteAction(context: Context, action: Action?): Int {
    return if (action != null) {
      val contentResolver = context.contentResolver
      if (contentResolver != null) {
        val actionId = action.id.toString()
        contentResolver.delete(baseContentUri.buildUpon().appendPath(actionId).build(), null, null)
      } else
        -1
    } else
      -1
  }

  /**
   * Generate the [Action] from the current cursor position;
   *
   * @param cursor  The cursor from which to get the data.
   * @return [Action].
   */
  private fun getCurrentAction(cursor: Cursor): Action? {
    val action = gson.fromJson(cursor.getString(cursor.getColumnIndex(COL_ACTION_JSON)), Action::class.java)
    return if (action != null) {
      action.id = cursor.getLong(cursor.getColumnIndex(BaseColumns._ID))
      action
    } else {
      null
    }
  }

  /**
   * Generate a [ContentValues] using [Action].
   *
   * @param action The [Action] object;
   * @return The generated [ContentValues].
   */
  private fun generateContentValues(action: Action): ContentValues? {
    val contentValues = ContentValues()
    if (action.email != null) {
      contentValues.put(COL_EMAIL, action.email!!.toLowerCase(Locale.getDefault()))
    } else {
      return null
    }

    contentValues.put(COL_ACTION_TYPE, action.type.value)
    contentValues.put(COL_ACTION_JSON, gson.toJson(action))
    return contentValues
  }

  companion object {
    const val TABLE_NAME_ACTION_QUEUE = "action_queue"

    const val COL_EMAIL = "email"
    const val COL_ACTION_TYPE = "action_type"
    const val COL_ACTION_JSON = "action_json"

    const val ACTION_QUEUE_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
        TABLE_NAME_ACTION_QUEUE + " (" +
        BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COL_EMAIL + " VARCHAR(100) NOT NULL, " +
        COL_ACTION_TYPE + " TEXT NOT NULL, " +
        COL_ACTION_JSON + " TEXT NOT NULL " + ");"
  }
}

