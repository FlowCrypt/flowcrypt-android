/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.provider

import android.content.ContentProvider
import android.content.ContentProviderOperation
import android.content.ContentProviderResult
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.BaseColumns
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.ActionQueueDaoSource
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.database.dao.source.KeysDaoSource
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import java.util.*

/**
 * This class encapsulate data and provide it to the application through the single
 * [ContentResolver] interface.
 *
 * @author Denis Bondarenko
 * Date: 13.05.2017
 * Time: 10:32
 * E-mail: DenBond7@gmail.com
 */
class SecurityContentProvider : ContentProvider() {

  private lateinit var dbHelper: SupportSQLiteOpenHelper
  private lateinit var appContext: Context

  override fun onCreate(): Boolean {
    appContext = context!!.applicationContext
    dbHelper = FlowCryptRoomDatabase.getDatabase(appContext).openHelper
    return true
  }

  override fun insert(uri: Uri, values: ContentValues?): Uri? {
    val result: Uri?
    val sqLiteDatabase = dbHelper.writableDatabase
    val match = URI_MATCHER.match(uri)
    val id: Long
    when (match) {
      MATCHED_CODE_KEYS_TABLE -> {
        id = sqLiteDatabase.insert(KeysDaoSource().tableName, SQLiteDatabase.CONFLICT_NONE, values)
        result = Uri.parse(KeysDaoSource().baseContentUri.toString() + "/" + id)
      }

      MATCHED_CODE_CONTACTS_TABLE -> {
        id = sqLiteDatabase.insert(ContactsDaoSource().tableName, SQLiteDatabase.CONFLICT_NONE, values)
        result = Uri.parse(ContactsDaoSource().baseContentUri.toString() + "/" + id)
      }

      MATCHED_CODE_IMAP_LABELS_TABLE -> {
        id = sqLiteDatabase.insert(ImapLabelsDaoSource().tableName, SQLiteDatabase.CONFLICT_NONE, values)
        result = Uri.parse(ImapLabelsDaoSource().baseContentUri.toString() + "/" + id)
      }

      MATCHED_CODE_ACCOUNTS_TABLE -> {
        id = sqLiteDatabase.insert(AccountDaoSource().tableName, SQLiteDatabase.CONFLICT_NONE, values)
        result = Uri.parse(AccountDaoSource().baseContentUri.toString() + "/" + id)
      }

      MATCHED_CODE_ACTION_QUEUE_TABLE -> {
        id = sqLiteDatabase.insert(ActionQueueDaoSource().tableName, SQLiteDatabase.CONFLICT_NONE, values)
        result = Uri.parse(ActionQueueDaoSource().baseContentUri.toString() + "/" + id)
      }

      else -> throw UnsupportedOperationException("Unknown uri: $uri")
    }

    if (id == -1L) {
      return null
    }

    if (result != null) {
      appContext.contentResolver.notifyChange(uri, null, false)
    }

    return result
  }

  override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int {
    var insertedRowsCount = 0

    val sqLiteDatabase = dbHelper.writableDatabase
    sqLiteDatabase.beginTransaction()
    try {
      for (contentValues in values) {
        val id = sqLiteDatabase.insert(getMatchedTableName(uri), SQLiteDatabase.CONFLICT_NONE, contentValues)
        if (id <= 0) {
          LogsUtil.d(TAG, "Failed to insert row into $uri")
        } else {
          insertedRowsCount++
        }
      }

      sqLiteDatabase.setTransactionSuccessful()

      if (insertedRowsCount != 0) {
        appContext.contentResolver.notifyChange(uri, null, false)
      }
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    } finally {
      sqLiteDatabase.endTransaction()
    }

    return insertedRowsCount
  }

  override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
    val sqLiteDatabase = dbHelper.writableDatabase
    val rowsCount = sqLiteDatabase.update(getMatchedTableName(uri), SQLiteDatabase.CONFLICT_NONE, values, selection, selectionArgs)

    if (rowsCount != 0) {
      appContext.contentResolver.notifyChange(uri, null, false)
    }

    return rowsCount
  }

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
    var rowsCount: Int
    val sqLiteDatabase = dbHelper.writableDatabase

    when (URI_MATCHER.match(uri)) {
      MATCHED_CODE_KEY_CLEAN_DATABASE -> {
        rowsCount = sqLiteDatabase.delete(AccountDaoSource().tableName,
            AccountDaoSource.COL_EMAIL + " = ?", selectionArgs)
        rowsCount += sqLiteDatabase.delete(ImapLabelsDaoSource().tableName,
            ImapLabelsDaoSource.COL_EMAIL + " = ?", selectionArgs)
        rowsCount += sqLiteDatabase.delete("messages", "email = ?", selectionArgs)
        rowsCount += sqLiteDatabase.delete("attachment", "email = ?", selectionArgs)
        rowsCount += sqLiteDatabase.delete("accounts_aliases", "email = ?", selectionArgs)
      }

      MATCHED_CODE_KEY_ERASE_DATABASE -> {
        rowsCount = sqLiteDatabase.delete(AccountDaoSource().tableName, null, null)
        rowsCount += sqLiteDatabase.delete("accounts_aliases", null, null)
        rowsCount += sqLiteDatabase.delete(ImapLabelsDaoSource().tableName, null, null)
        rowsCount += sqLiteDatabase.delete("messages", null, null)
        rowsCount += sqLiteDatabase.delete("attachment", null, null)
        rowsCount += sqLiteDatabase.delete(KeysDaoSource().tableName, null, null)
        rowsCount += sqLiteDatabase.delete("user_id_emails_and_keys", null, null)
        rowsCount += sqLiteDatabase.delete(ContactsDaoSource().tableName, null, null)
      }

      MATCHED_CODE_ACTION_QUEUE_ROW -> rowsCount = sqLiteDatabase.delete(ActionQueueDaoSource().tableName,
          BaseColumns._ID + " = ?", arrayOf(uri.lastPathSegment))

      else -> rowsCount = sqLiteDatabase.delete(getMatchedTableName(uri), selection, selectionArgs)
    }

    if (rowsCount != 0) {
      appContext.contentResolver.notifyChange(uri, null, false)
    }

    return rowsCount
  }

  override fun query(uri: Uri, proj: Array<String>?, selection: String?, selectionArgs: Array<String>?,
                     sortOrder: String?): Cursor? {
    val sqLiteDatabase = dbHelper.readableDatabase

    val table = getMatchedTableName(uri)
    val supportSQLiteQuery = SupportSQLiteQueryBuilder
        .builder(table)
        .columns(proj)
        .selection(selection, selectionArgs)
        .orderBy(sortOrder)
        .create()

    val cursor = sqLiteDatabase.query(supportSQLiteQuery)

    cursor?.setNotificationUri(appContext.contentResolver, uri)

    return cursor
  }

  override fun applyBatch(operations: ArrayList<ContentProviderOperation>): Array<ContentProviderResult> {
    var contentProviderResults = emptyArray<ContentProviderResult>()
    val sqLiteDatabase = dbHelper.writableDatabase
    sqLiteDatabase.beginTransaction()
    try {
      contentProviderResults = super.applyBatch(operations)
      sqLiteDatabase.setTransactionSuccessful()
    } catch (e: Exception) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    } finally {
      sqLiteDatabase.endTransaction()
    }
    return contentProviderResults
  }

  override fun getType(uri: Uri): String? {
    when (URI_MATCHER.match(uri)) {
      MATCHED_CODE_KEYS_TABLE -> return KeysDaoSource().rowsContentType

      MATCHED_CODE_KEYS_TABLE_SINGLE_ROW -> return KeysDaoSource().singleRowContentType

      MATCHED_CODE_CONTACTS_TABLE -> return ContactsDaoSource().rowsContentType

      MATCHED_CODE_CONTACTS_TABLE_SINGLE_ROW -> return ContactsDaoSource().singleRowContentType

      MATCHED_CODE_IMAP_LABELS_TABLE -> return ImapLabelsDaoSource().rowsContentType

      MATCHED_CODE_IMAP_LABELS_SINGLE_ROW -> return ImapLabelsDaoSource().singleRowContentType

      MATCHED_CODE_ACCOUNTS_TABLE -> return AccountDaoSource().rowsContentType

      MATCHED_CODE_ACCOUNTS_SINGLE_ROW -> return AccountDaoSource().singleRowContentType

      MATCHED_CODE_ACTION_QUEUE_TABLE -> return ActionQueueDaoSource().rowsContentType

      MATCHED_CODE_ACTION_QUEUE_ROW -> return ActionQueueDaoSource().singleRowContentType

      else -> throw IllegalArgumentException("Unknown uri: $uri")
    }
  }

  /**
   * Get the matched table name from an input [Uri]
   *
   * @param uri An input [Uri]
   * @return The matched table name;
   */
  private fun getMatchedTableName(uri: Uri): String {
    return when (URI_MATCHER.match(uri)) {
      MATCHED_CODE_KEYS_TABLE -> KeysDaoSource.TABLE_NAME_KEYS

      MATCHED_CODE_CONTACTS_TABLE -> ContactsDaoSource.TABLE_NAME_CONTACTS

      MATCHED_CODE_IMAP_LABELS_TABLE -> ImapLabelsDaoSource.TABLE_NAME_IMAP_LABELS

      MATCHED_CODE_ACCOUNTS_TABLE -> AccountDaoSource.TABLE_NAME_ACCOUNTS

      MATCHED_CODE_ACTION_QUEUE_TABLE -> ActionQueueDaoSource.TABLE_NAME_ACTION_QUEUE

      else -> throw UnsupportedOperationException("Unknown uri: $uri")
    }
  }

  companion object {
    private val TAG = SecurityContentProvider::class.java.simpleName

    private const val MATCHED_CODE_KEYS_TABLE = 1
    private const val MATCHED_CODE_KEYS_TABLE_SINGLE_ROW = 2
    private const val MATCHED_CODE_KEY_CLEAN_DATABASE = 3
    private const val MATCHED_CODE_CONTACTS_TABLE = 4
    private const val MATCHED_CODE_CONTACTS_TABLE_SINGLE_ROW = 5
    private const val MATCHED_CODE_IMAP_LABELS_TABLE = 6
    private const val MATCHED_CODE_IMAP_LABELS_SINGLE_ROW = 7
    private const val MATCHED_CODE_ACCOUNTS_TABLE = 10
    private const val MATCHED_CODE_ACCOUNTS_SINGLE_ROW = 11
    private const val MATCHED_CODE_KEY_ERASE_DATABASE = 16
    private const val MATCHED_CODE_ACTION_QUEUE_TABLE = 17
    private const val MATCHED_CODE_ACTION_QUEUE_ROW = 18

    private const val SINGLE_APPENDED_SUFFIX = "/#"
    private val URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH)

    init {
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, KeysDaoSource.TABLE_NAME_KEYS, MATCHED_CODE_KEYS_TABLE)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, KeysDaoSource.TABLE_NAME_KEYS + SINGLE_APPENDED_SUFFIX,
          MATCHED_CODE_KEYS_TABLE_SINGLE_ROW)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, FlowcryptContract.CLEAN_DATABASE,
          MATCHED_CODE_KEY_CLEAN_DATABASE)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, FlowcryptContract.ERASE_DATABASE,
          MATCHED_CODE_KEY_ERASE_DATABASE)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, ContactsDaoSource.TABLE_NAME_CONTACTS,
          MATCHED_CODE_CONTACTS_TABLE)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, ContactsDaoSource.TABLE_NAME_CONTACTS
          + SINGLE_APPENDED_SUFFIX, MATCHED_CODE_CONTACTS_TABLE_SINGLE_ROW)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, ImapLabelsDaoSource.TABLE_NAME_IMAP_LABELS,
          MATCHED_CODE_IMAP_LABELS_TABLE)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, ImapLabelsDaoSource.TABLE_NAME_IMAP_LABELS
          + SINGLE_APPENDED_SUFFIX, MATCHED_CODE_IMAP_LABELS_SINGLE_ROW)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, AccountDaoSource.TABLE_NAME_ACCOUNTS,
          MATCHED_CODE_ACCOUNTS_TABLE)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, AccountDaoSource.TABLE_NAME_ACCOUNTS
          + SINGLE_APPENDED_SUFFIX, MATCHED_CODE_ACCOUNTS_SINGLE_ROW)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, ActionQueueDaoSource.TABLE_NAME_ACTION_QUEUE
          + SINGLE_APPENDED_SUFFIX, MATCHED_CODE_ACTION_QUEUE_ROW)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, ActionQueueDaoSource.TABLE_NAME_ACTION_QUEUE,
          MATCHED_CODE_ACTION_QUEUE_TABLE)
    }
  }
}
