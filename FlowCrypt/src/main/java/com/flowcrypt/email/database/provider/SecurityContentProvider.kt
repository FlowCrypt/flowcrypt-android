/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import com.flowcrypt.email.database.FlowCryptSQLiteOpenHelper
import com.flowcrypt.email.database.dao.source.AccountAliasesDaoSource
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.ActionQueueDaoSource
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.database.dao.source.KeysDaoSource
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource
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

  private lateinit var dbHelper: FlowCryptSQLiteOpenHelper
  private lateinit var appContext: Context

  override fun onCreate(): Boolean {
    appContext = context!!.applicationContext
    dbHelper = FlowCryptSQLiteOpenHelper.getInstance(appContext)
    return true
  }

  override fun insert(uri: Uri, values: ContentValues?): Uri? {
    val result: Uri?
    val sqLiteDatabase = dbHelper.writableDatabase
    val match = URI_MATCHER.match(uri)
    val id: Long
    when (match) {
      MATCHED_CODE_KEYS_TABLE -> {
        id = sqLiteDatabase.insert(KeysDaoSource().tableName, null, values)
        result = Uri.parse(KeysDaoSource().baseContentUri.toString() + "/" + id)
      }

      MATCHED_CODE_CONTACTS_TABLE -> {
        id = sqLiteDatabase.insert(ContactsDaoSource().tableName, null, values)
        result = Uri.parse(ContactsDaoSource().baseContentUri.toString() + "/" + id)
      }

      MATCHED_CODE_IMAP_LABELS_TABLE -> {
        id = sqLiteDatabase.insert(ImapLabelsDaoSource().tableName, null, values)
        result = Uri.parse(ImapLabelsDaoSource().baseContentUri.toString() + "/" + id)
      }

      MATCHED_CODE_IMAP_MESSAGES_TABLE -> {
        id = sqLiteDatabase.insert(MessageDaoSource().tableName, null, values)
        result = Uri.parse(MessageDaoSource().baseContentUri.toString() + "/" + id)
      }

      MATCHED_CODE_ACCOUNTS_TABLE -> {
        id = sqLiteDatabase.insert(AccountDaoSource().tableName, null, values)
        result = Uri.parse(AccountDaoSource().baseContentUri.toString() + "/" + id)
      }

      MATCHED_CODE_ATTACHMENT_TABLE -> {
        id = sqLiteDatabase.insert(AttachmentDaoSource().tableName, null, values)
        result = Uri.parse(AttachmentDaoSource().baseContentUri.toString() + "/" + id)
      }

      MATCHED_CODE_ACCOUNT_ALIASES_TABLE -> {
        id = sqLiteDatabase.insert(AccountAliasesDaoSource().tableName, null, values)
        result = Uri.parse(AccountAliasesDaoSource().baseContentUri.toString() + "/" + id)
      }

      MATCHED_CODE_ACTION_QUEUE_TABLE -> {
        id = sqLiteDatabase.insert(ActionQueueDaoSource().tableName, null, values)
        result = Uri.parse(ActionQueueDaoSource().baseContentUri.toString() + "/" + id)
      }

      MATCHED_CODE_ACTION_USER_ID_EMAILS_AND_KEYS_TABLE -> {
        id = sqLiteDatabase.insert(UserIdEmailsKeysDaoSource().tableName, null, values)
        result = Uri.parse(UserIdEmailsKeysDaoSource().baseContentUri.toString() + "/" + id)
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
      when (URI_MATCHER.match(uri)) {
        MATCHED_CODE_IMAP_MESSAGES_TABLE -> for (contentValues in values) {
          var id = sqLiteDatabase.insert(MessageDaoSource().tableName, null, contentValues)

          //if message not inserted, try to update message with some UID
          if (id <= 0) {
            id = updateMsgInfo(sqLiteDatabase, contentValues)
          } else {
            insertedRowsCount++
          }

          if (id <= 0) {
            LogsUtil.d(TAG, "Failed to insert row into $uri")
          }
        }

        else -> for (contentValues in values) {
          val id = sqLiteDatabase.insert(getMatchedTableName(uri), null, contentValues)
          if (id <= 0) {
            LogsUtil.d(TAG, "Failed to insert row into $uri")
          } else {
            insertedRowsCount++
          }
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
    val rowsCount = sqLiteDatabase.update(getMatchedTableName(uri), values, selection, selectionArgs)

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
        rowsCount += sqLiteDatabase.delete(MessageDaoSource().tableName,
            MessageDaoSource.COL_EMAIL + " = ?", selectionArgs)
        rowsCount += sqLiteDatabase.delete(AttachmentDaoSource().tableName,
            AttachmentDaoSource.COL_EMAIL + " = ?", selectionArgs)
      }

      MATCHED_CODE_KEY_ERASE_DATABASE -> {
        rowsCount = sqLiteDatabase.delete(AccountDaoSource().tableName, null, null)
        rowsCount += sqLiteDatabase.delete(AccountAliasesDaoSource().tableName, null, null)
        rowsCount += sqLiteDatabase.delete(ImapLabelsDaoSource().tableName, null, null)
        rowsCount += sqLiteDatabase.delete(MessageDaoSource().tableName, null, null)
        rowsCount += sqLiteDatabase.delete(AttachmentDaoSource().tableName, null, null)
        rowsCount += sqLiteDatabase.delete(KeysDaoSource().tableName, null, null)
        rowsCount += sqLiteDatabase.delete(UserIdEmailsKeysDaoSource().tableName, null, null)
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

  override fun query(uri: Uri, proj: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? {
    val sqLiteDatabase = dbHelper.readableDatabase

    val table = getMatchedTableName(uri)
    val cursor = sqLiteDatabase.query(table, proj, selection, selectionArgs, null, null, sortOrder, null)

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

      MATCHED_CODE_IMAP_MESSAGES_TABLE -> return MessageDaoSource().rowsContentType

      MATCHED_CODE_IMAP_MESSAGES_SINGLE_ROW -> return MessageDaoSource().singleRowContentType

      MATCHED_CODE_ACCOUNTS_TABLE -> return AccountDaoSource().rowsContentType

      MATCHED_CODE_ACCOUNTS_SINGLE_ROW -> return AccountDaoSource().singleRowContentType

      MATCHED_CODE_ATTACHMENT_TABLE -> return AttachmentDaoSource().rowsContentType

      MATCHED_CODE_ATTACHMENT_SINGLE_ROW -> return AttachmentDaoSource().singleRowContentType

      MATCHED_CODE_ACCOUNT_ALIASES_TABLE -> return AttachmentDaoSource().rowsContentType

      MATCHED_CODE_ACCOUNT_ALIASES_ROW -> return AttachmentDaoSource().singleRowContentType

      MATCHED_CODE_ACTION_QUEUE_TABLE -> return ActionQueueDaoSource().rowsContentType

      MATCHED_CODE_ACTION_QUEUE_ROW -> return ActionQueueDaoSource().singleRowContentType

      MATCHED_CODE_ACTION_USER_ID_EMAILS_AND_KEYS_TABLE -> return UserIdEmailsKeysDaoSource().rowsContentType

      MATCHED_CODE_ACTION_USER_ID_EMAILS_AND_KEYS_ROW -> return UserIdEmailsKeysDaoSource().singleRowContentType

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
    when (URI_MATCHER.match(uri)) {
      MATCHED_CODE_KEYS_TABLE -> return KeysDaoSource.TABLE_NAME_KEYS

      MATCHED_CODE_CONTACTS_TABLE -> return ContactsDaoSource.TABLE_NAME_CONTACTS

      MATCHED_CODE_IMAP_LABELS_TABLE -> return ImapLabelsDaoSource.TABLE_NAME_IMAP_LABELS

      MATCHED_CODE_IMAP_MESSAGES_TABLE -> return MessageDaoSource.TABLE_NAME_MESSAGES

      MATCHED_CODE_ACCOUNTS_TABLE -> return AccountDaoSource.TABLE_NAME_ACCOUNTS

      MATCHED_CODE_ATTACHMENT_TABLE -> return AttachmentDaoSource.TABLE_NAME_ATTACHMENT

      MATCHED_CODE_ACCOUNT_ALIASES_TABLE -> return AccountAliasesDaoSource.TABLE_NAME_ACCOUNTS_ALIASES

      MATCHED_CODE_ACTION_QUEUE_TABLE -> return ActionQueueDaoSource.TABLE_NAME_ACTION_QUEUE

      MATCHED_CODE_ACTION_USER_ID_EMAILS_AND_KEYS_TABLE -> return UserIdEmailsKeysDaoSource.TABLE_NAME_USER_ID_EMAILS_AND_KEYS

      else -> throw UnsupportedOperationException("Unknown uri: $uri")
    }
  }

  /**
   * Try to update some message.
   *
   * @param sqLiteDatabase The [SQLiteDatabase] which will be used to update a message.
   * @param contentValues  The new information about some message.
   * @return the number of rows affected
   */
  private fun updateMsgInfo(sqLiteDatabase: SQLiteDatabase, contentValues: ContentValues): Long {
    val id: Long
    val email = contentValues.getAsString(MessageDaoSource.COL_EMAIL)
    val folder = contentValues.getAsString(MessageDaoSource.COL_FOLDER)
    val uid = contentValues.getAsString(MessageDaoSource.COL_UID)

    val selection = (MessageDaoSource.COL_EMAIL + "= ? AND " + MessageDaoSource.COL_FOLDER + " = ? AND "
        + MessageDaoSource.COL_UID + " = ? ")
    val selectionArgs = arrayOf(email, folder, uid)
    id = sqLiteDatabase.update(MessageDaoSource().tableName, contentValues, selection, selectionArgs).toLong()
    return id
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
    private const val MATCHED_CODE_IMAP_MESSAGES_TABLE = 8
    private const val MATCHED_CODE_IMAP_MESSAGES_SINGLE_ROW = 9
    private const val MATCHED_CODE_ACCOUNTS_TABLE = 10
    private const val MATCHED_CODE_ACCOUNTS_SINGLE_ROW = 11
    private const val MATCHED_CODE_ATTACHMENT_TABLE = 12
    private const val MATCHED_CODE_ATTACHMENT_SINGLE_ROW = 13
    private const val MATCHED_CODE_ACCOUNT_ALIASES_TABLE = 14
    private const val MATCHED_CODE_ACCOUNT_ALIASES_ROW = 15
    private const val MATCHED_CODE_KEY_ERASE_DATABASE = 16
    private const val MATCHED_CODE_ACTION_QUEUE_TABLE = 17
    private const val MATCHED_CODE_ACTION_QUEUE_ROW = 18
    private const val MATCHED_CODE_ACTION_USER_ID_EMAILS_AND_KEYS_TABLE = 19
    private const val MATCHED_CODE_ACTION_USER_ID_EMAILS_AND_KEYS_ROW = 20

    private const val SINGLE_APPENDED_SUFFIX = "/#"
    private val URI_MATCHER = UriMatcher(UriMatcher.NO_MATCH)

    init {
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, KeysDaoSource.TABLE_NAME_KEYS, MATCHED_CODE_KEYS_TABLE)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, KeysDaoSource.TABLE_NAME_KEYS + SINGLE_APPENDED_SUFFIX, MATCHED_CODE_KEYS_TABLE_SINGLE_ROW)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, FlowcryptContract.CLEAN_DATABASE,
          MATCHED_CODE_KEY_CLEAN_DATABASE)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, FlowcryptContract.ERASE_DATABASE,
          MATCHED_CODE_KEY_ERASE_DATABASE)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, ContactsDaoSource.TABLE_NAME_CONTACTS,
          MATCHED_CODE_CONTACTS_TABLE)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, ContactsDaoSource.TABLE_NAME_CONTACTS + SINGLE_APPENDED_SUFFIX, MATCHED_CODE_CONTACTS_TABLE_SINGLE_ROW)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, ImapLabelsDaoSource.TABLE_NAME_IMAP_LABELS,
          MATCHED_CODE_IMAP_LABELS_TABLE)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, ImapLabelsDaoSource.TABLE_NAME_IMAP_LABELS + SINGLE_APPENDED_SUFFIX, MATCHED_CODE_IMAP_LABELS_SINGLE_ROW)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, MessageDaoSource.TABLE_NAME_MESSAGES,
          MATCHED_CODE_IMAP_MESSAGES_TABLE)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, MessageDaoSource.TABLE_NAME_MESSAGES + SINGLE_APPENDED_SUFFIX, MATCHED_CODE_IMAP_MESSAGES_SINGLE_ROW)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, AccountDaoSource.TABLE_NAME_ACCOUNTS,
          MATCHED_CODE_ACCOUNTS_TABLE)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, AccountDaoSource.TABLE_NAME_ACCOUNTS + SINGLE_APPENDED_SUFFIX, MATCHED_CODE_ACCOUNTS_SINGLE_ROW)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, AttachmentDaoSource.TABLE_NAME_ATTACHMENT,
          MATCHED_CODE_ATTACHMENT_TABLE)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, AttachmentDaoSource.TABLE_NAME_ATTACHMENT + SINGLE_APPENDED_SUFFIX, MATCHED_CODE_ATTACHMENT_SINGLE_ROW)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, AccountAliasesDaoSource.TABLE_NAME_ACCOUNTS_ALIASES,
          MATCHED_CODE_ACCOUNT_ALIASES_TABLE)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, AccountAliasesDaoSource.TABLE_NAME_ACCOUNTS_ALIASES + SINGLE_APPENDED_SUFFIX, MATCHED_CODE_ACCOUNT_ALIASES_ROW)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, ActionQueueDaoSource.TABLE_NAME_ACTION_QUEUE + SINGLE_APPENDED_SUFFIX, MATCHED_CODE_ACTION_QUEUE_ROW)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, ActionQueueDaoSource.TABLE_NAME_ACTION_QUEUE,
          MATCHED_CODE_ACTION_QUEUE_TABLE)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, UserIdEmailsKeysDaoSource.TABLE_NAME_USER_ID_EMAILS_AND_KEYS + SINGLE_APPENDED_SUFFIX, MATCHED_CODE_ACTION_USER_ID_EMAILS_AND_KEYS_ROW)
      URI_MATCHER.addURI(FlowcryptContract.AUTHORITY, UserIdEmailsKeysDaoSource.TABLE_NAME_USER_ID_EMAILS_AND_KEYS,
          MATCHED_CODE_ACTION_USER_ID_EMAILS_AND_KEYS_TABLE)
    }
  }
}
