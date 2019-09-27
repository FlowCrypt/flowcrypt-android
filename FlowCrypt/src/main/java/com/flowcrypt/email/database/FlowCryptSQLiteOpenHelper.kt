/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.database.dao.source.AccountAliasesDaoSource
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.ActionQueueDaoSource
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.database.dao.source.KeysDaoSource
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource
import com.flowcrypt.email.service.actionqueue.actions.FillUserIdEmailsKeysTableAction
import com.flowcrypt.email.util.LogsUtil


/**
 * A helper class to manage database creation and version management.
 *
 * @author DenBond7
 * Date: 13.05.2017
 * Time: 12:20
 * E-mail: DenBond7@gmail.com
 */
class FlowCryptSQLiteOpenHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

  override fun onCreate(sqLiteDatabase: SQLiteDatabase) {
    sqLiteDatabase.execSQL(KeysDaoSource.KEYS_TABLE_SQL_CREATE)
    sqLiteDatabase.execSQL(KeysDaoSource.CREATE_INDEX_LONG_ID_IN_KEYS)

    sqLiteDatabase.execSQL(ContactsDaoSource.CONTACTS_TABLE_SQL_CREATE)
    sqLiteDatabase.execSQL(ContactsDaoSource.CREATE_UNIQUE_INDEX_EMAIL_IN_CONTACT)
    sqLiteDatabase.execSQL(ContactsDaoSource.CREATE_INDEX_NAME_IN_CONTACT)
    sqLiteDatabase.execSQL(ContactsDaoSource.CREATE_INDEX_HAS_PGP_IN_CONTACT)
    sqLiteDatabase.execSQL(ContactsDaoSource.CREATE_INDEX_LONG_ID_IN_CONTACT)
    sqLiteDatabase.execSQL(ContactsDaoSource.CREATE_INDEX_LAST_USE_IN_CONTACT)

    sqLiteDatabase.execSQL(ImapLabelsDaoSource.IMAP_LABELS_TABLE_SQL_CREATE)

    sqLiteDatabase.execSQL(MessageDaoSource.IMAP_MESSAGES_INFO_TABLE_SQL_CREATE)
    sqLiteDatabase.execSQL(MessageDaoSource.CREATE_INDEX_EMAIL_IN_MESSAGES)
    sqLiteDatabase.execSQL(MessageDaoSource.CREATE_INDEX_EMAIL_UID_FOLDER_IN_MESSAGES)

    sqLiteDatabase.execSQL(AccountDaoSource.ACCOUNTS_TABLE_SQL_CREATE)
    sqLiteDatabase.execSQL(AccountDaoSource.CREATE_INDEX_EMAIL_TYPE_IN_ACCOUNTS)

    sqLiteDatabase.execSQL(AttachmentDaoSource.ATTACHMENT_TABLE_SQL_CREATE)
    sqLiteDatabase.execSQL(AttachmentDaoSource.CREATE_UNIQUE_INDEX_EMAIL_UID_FOLDER_PATH_IN_ATTACHMENT)

    sqLiteDatabase.execSQL(AccountAliasesDaoSource.ACCOUNTS_ALIASES_TABLE_SQL_CREATE)
    sqLiteDatabase.execSQL(AccountAliasesDaoSource.CREATE_INDEX_EMAIL_TYPE_IN_ACCOUNTS_ALIASES)

    sqLiteDatabase.execSQL(ActionQueueDaoSource.ACTION_QUEUE_TABLE_SQL_CREATE)

    sqLiteDatabase.execSQL(UserIdEmailsKeysDaoSource.SQL_CREATE_TABLE)
    sqLiteDatabase.execSQL(UserIdEmailsKeysDaoSource.INDEX_LONG_ID_USER_ID_EMAIL)
  }

  override fun onUpgrade(sqLiteDatabase: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    when (oldVersion) {
      1 -> {
        upgradeDatabaseFrom1To2Version(sqLiteDatabase)
        upgradeDatabaseFrom2To3Version(sqLiteDatabase)
        upgradeDatabaseFrom3To4Version(sqLiteDatabase)
        upgradeDatabaseFrom4To5Version(sqLiteDatabase)
        upgradeDatabaseFrom5To6Version(sqLiteDatabase)
        upgradeDatabaseFrom6To7Version(sqLiteDatabase)
        upgradeDatabaseFrom7To8Version(sqLiteDatabase)
        upgradeDatabaseFrom8To9Version(sqLiteDatabase)
        upgradeDatabaseFrom9To10Version(sqLiteDatabase)
        upgradeDatabaseFrom10To11Version(sqLiteDatabase)
        upgradeDatabaseFrom11To12Version(sqLiteDatabase)
        upgradeDatabaseFrom12To13Version(sqLiteDatabase)
        upgradeDatabaseFrom13To14Version(sqLiteDatabase)
        upgradeDatabaseFrom14To15Version(sqLiteDatabase)
        upgradeDatabaseFrom15To16Version(sqLiteDatabase)
        upgradeDatabaseFrom16To17Version(sqLiteDatabase)
      }

      2 -> {
        upgradeDatabaseFrom2To3Version(sqLiteDatabase)
        upgradeDatabaseFrom3To4Version(sqLiteDatabase)
        upgradeDatabaseFrom4To5Version(sqLiteDatabase)
        upgradeDatabaseFrom5To6Version(sqLiteDatabase)
        upgradeDatabaseFrom6To7Version(sqLiteDatabase)
        upgradeDatabaseFrom7To8Version(sqLiteDatabase)
        upgradeDatabaseFrom8To9Version(sqLiteDatabase)
        upgradeDatabaseFrom9To10Version(sqLiteDatabase)
        upgradeDatabaseFrom10To11Version(sqLiteDatabase)
        upgradeDatabaseFrom11To12Version(sqLiteDatabase)
        upgradeDatabaseFrom12To13Version(sqLiteDatabase)
        upgradeDatabaseFrom13To14Version(sqLiteDatabase)
        upgradeDatabaseFrom14To15Version(sqLiteDatabase)
        upgradeDatabaseFrom15To16Version(sqLiteDatabase)
        upgradeDatabaseFrom16To17Version(sqLiteDatabase)
      }

      3 -> {
        upgradeDatabaseFrom3To4Version(sqLiteDatabase)
        upgradeDatabaseFrom4To5Version(sqLiteDatabase)
        upgradeDatabaseFrom5To6Version(sqLiteDatabase)
        upgradeDatabaseFrom6To7Version(sqLiteDatabase)
        upgradeDatabaseFrom7To8Version(sqLiteDatabase)
        upgradeDatabaseFrom8To9Version(sqLiteDatabase)
        upgradeDatabaseFrom9To10Version(sqLiteDatabase)
        upgradeDatabaseFrom10To11Version(sqLiteDatabase)
        upgradeDatabaseFrom11To12Version(sqLiteDatabase)
        upgradeDatabaseFrom12To13Version(sqLiteDatabase)
        upgradeDatabaseFrom13To14Version(sqLiteDatabase)
        upgradeDatabaseFrom14To15Version(sqLiteDatabase)
        upgradeDatabaseFrom15To16Version(sqLiteDatabase)
        upgradeDatabaseFrom16To17Version(sqLiteDatabase)
      }

      4 -> {
        upgradeDatabaseFrom4To5Version(sqLiteDatabase)
        upgradeDatabaseFrom5To6Version(sqLiteDatabase)
        upgradeDatabaseFrom6To7Version(sqLiteDatabase)
        upgradeDatabaseFrom7To8Version(sqLiteDatabase)
        upgradeDatabaseFrom8To9Version(sqLiteDatabase)
        upgradeDatabaseFrom9To10Version(sqLiteDatabase)
        upgradeDatabaseFrom10To11Version(sqLiteDatabase)
        upgradeDatabaseFrom11To12Version(sqLiteDatabase)
        upgradeDatabaseFrom12To13Version(sqLiteDatabase)
        upgradeDatabaseFrom13To14Version(sqLiteDatabase)
        upgradeDatabaseFrom14To15Version(sqLiteDatabase)
        upgradeDatabaseFrom15To16Version(sqLiteDatabase)
        upgradeDatabaseFrom16To17Version(sqLiteDatabase)
      }

      5 -> {
        upgradeDatabaseFrom5To6Version(sqLiteDatabase)
        upgradeDatabaseFrom6To7Version(sqLiteDatabase)
        upgradeDatabaseFrom7To8Version(sqLiteDatabase)
        upgradeDatabaseFrom8To9Version(sqLiteDatabase)
        upgradeDatabaseFrom9To10Version(sqLiteDatabase)
        upgradeDatabaseFrom10To11Version(sqLiteDatabase)
        upgradeDatabaseFrom11To12Version(sqLiteDatabase)
        upgradeDatabaseFrom12To13Version(sqLiteDatabase)
        upgradeDatabaseFrom13To14Version(sqLiteDatabase)
        upgradeDatabaseFrom14To15Version(sqLiteDatabase)
        upgradeDatabaseFrom15To16Version(sqLiteDatabase)
        upgradeDatabaseFrom16To17Version(sqLiteDatabase)
      }

      6 -> {
        upgradeDatabaseFrom6To7Version(sqLiteDatabase)
        upgradeDatabaseFrom7To8Version(sqLiteDatabase)
        upgradeDatabaseFrom8To9Version(sqLiteDatabase)
        upgradeDatabaseFrom9To10Version(sqLiteDatabase)
        upgradeDatabaseFrom10To11Version(sqLiteDatabase)
        upgradeDatabaseFrom11To12Version(sqLiteDatabase)
        upgradeDatabaseFrom12To13Version(sqLiteDatabase)
        upgradeDatabaseFrom13To14Version(sqLiteDatabase)
        upgradeDatabaseFrom14To15Version(sqLiteDatabase)
        upgradeDatabaseFrom15To16Version(sqLiteDatabase)
        upgradeDatabaseFrom16To17Version(sqLiteDatabase)
      }

      7 -> {
        upgradeDatabaseFrom7To8Version(sqLiteDatabase)
        upgradeDatabaseFrom8To9Version(sqLiteDatabase)
        upgradeDatabaseFrom9To10Version(sqLiteDatabase)
        upgradeDatabaseFrom10To11Version(sqLiteDatabase)
        upgradeDatabaseFrom11To12Version(sqLiteDatabase)
        upgradeDatabaseFrom12To13Version(sqLiteDatabase)
        upgradeDatabaseFrom13To14Version(sqLiteDatabase)
        upgradeDatabaseFrom14To15Version(sqLiteDatabase)
        upgradeDatabaseFrom15To16Version(sqLiteDatabase)
        upgradeDatabaseFrom16To17Version(sqLiteDatabase)
      }

      8 -> {
        upgradeDatabaseFrom8To9Version(sqLiteDatabase)
        upgradeDatabaseFrom9To10Version(sqLiteDatabase)
        upgradeDatabaseFrom10To11Version(sqLiteDatabase)
        upgradeDatabaseFrom11To12Version(sqLiteDatabase)
        upgradeDatabaseFrom12To13Version(sqLiteDatabase)
        upgradeDatabaseFrom13To14Version(sqLiteDatabase)
        upgradeDatabaseFrom14To15Version(sqLiteDatabase)
        upgradeDatabaseFrom15To16Version(sqLiteDatabase)
        upgradeDatabaseFrom16To17Version(sqLiteDatabase)
      }

      9 -> {
        upgradeDatabaseFrom9To10Version(sqLiteDatabase)
        upgradeDatabaseFrom10To11Version(sqLiteDatabase)
        upgradeDatabaseFrom11To12Version(sqLiteDatabase)
        upgradeDatabaseFrom12To13Version(sqLiteDatabase)
        upgradeDatabaseFrom13To14Version(sqLiteDatabase)
        upgradeDatabaseFrom14To15Version(sqLiteDatabase)
        upgradeDatabaseFrom15To16Version(sqLiteDatabase)
        upgradeDatabaseFrom16To17Version(sqLiteDatabase)
      }

      10 -> {
        upgradeDatabaseFrom10To11Version(sqLiteDatabase)
        upgradeDatabaseFrom11To12Version(sqLiteDatabase)
        upgradeDatabaseFrom12To13Version(sqLiteDatabase)
        upgradeDatabaseFrom13To14Version(sqLiteDatabase)
        upgradeDatabaseFrom14To15Version(sqLiteDatabase)
        upgradeDatabaseFrom15To16Version(sqLiteDatabase)
        upgradeDatabaseFrom16To17Version(sqLiteDatabase)
      }

      11 -> {
        upgradeDatabaseFrom11To12Version(sqLiteDatabase)
        upgradeDatabaseFrom12To13Version(sqLiteDatabase)
        upgradeDatabaseFrom13To14Version(sqLiteDatabase)
        upgradeDatabaseFrom14To15Version(sqLiteDatabase)
        upgradeDatabaseFrom15To16Version(sqLiteDatabase)
        upgradeDatabaseFrom16To17Version(sqLiteDatabase)
      }

      12 -> {
        upgradeDatabaseFrom12To13Version(sqLiteDatabase)
        upgradeDatabaseFrom13To14Version(sqLiteDatabase)
        upgradeDatabaseFrom14To15Version(sqLiteDatabase)
        upgradeDatabaseFrom15To16Version(sqLiteDatabase)
        upgradeDatabaseFrom16To17Version(sqLiteDatabase)
      }

      13 -> {
        upgradeDatabaseFrom13To14Version(sqLiteDatabase)
        upgradeDatabaseFrom14To15Version(sqLiteDatabase)
        upgradeDatabaseFrom15To16Version(sqLiteDatabase)
        upgradeDatabaseFrom16To17Version(sqLiteDatabase)
      }

      14 -> {
        upgradeDatabaseFrom14To15Version(sqLiteDatabase)
        upgradeDatabaseFrom15To16Version(sqLiteDatabase)
        upgradeDatabaseFrom16To17Version(sqLiteDatabase)
      }

      15 -> {
        upgradeDatabaseFrom15To16Version(sqLiteDatabase)
        upgradeDatabaseFrom16To17Version(sqLiteDatabase)
      }

      16 -> {
        upgradeDatabaseFrom16To17Version(sqLiteDatabase)
      }
    }

    LogsUtil.d(TAG, "Database updated from OLD_VERSION = " + oldVersion
        + " to NEW_VERSION = " + newVersion)
  }

  private fun upgradeDatabaseFrom1To2Version(sqLiteDatabase: SQLiteDatabase) {
    sqLiteDatabase.beginTransaction()
    try {
      sqLiteDatabase.execSQL("ALTER TABLE " + MessageDaoSource.TABLE_NAME_MESSAGES +
          " ADD COLUMN " + MessageDaoSource.COL_IS_MESSAGE_HAS_ATTACHMENTS + " INTEGER DEFAULT 0;")

      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_IS_ENABLE + " INTEGER DEFAULT 1;")

      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_IS_ACTIVE + " INTEGER DEFAULT 0;")
      sqLiteDatabase.setTransactionSuccessful()
    } finally {
      sqLiteDatabase.endTransaction()
    }
  }

  @Suppress("UNUSED_PARAMETER")
  private fun upgradeDatabaseFrom2To3Version(sqLiteDatabase: SQLiteDatabase) {
    // Removed as redundant
  }

  private fun upgradeDatabaseFrom3To4Version(sqLiteDatabase: SQLiteDatabase) {
    sqLiteDatabase.beginTransaction()
    try {
      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_USERNAME + " TEXT NOT NULL DEFAULT '';")
      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_PASSWORD + " TEXT NOT NULL DEFAULT '';")
      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_IMAP_SERVER + " TEXT NOT NULL DEFAULT '';")
      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_IMAP_PORT + " INTEGER DEFAULT 143;")
      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_IMAP_IS_USE_SSL_TLS + " INTEGER DEFAULT 0;")
      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_IMAP_IS_USE_STARTTLS + " INTEGER DEFAULT 0;")
      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_IMAP_AUTH_MECHANISMS + " TEXT;")
      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_SMTP_SERVER + " TEXT NOT NULL DEFAULT '';")
      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_SMTP_PORT + " INTEGER DEFAULT 25;")
      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_SMTP_IS_USE_SSL_TLS + " INTEGER DEFAULT 0;")
      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_SMTP_IS_USE_STARTTLS + " INTEGER DEFAULT 0;")
      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_SMTP_AUTH_MECHANISMS + " TEXT;")
      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_SMTP_IS_USE_CUSTOM_SIGN + " INTEGER DEFAULT 0;")
      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_SMTP_USERNAME + " TEXT DEFAULT NULL;")
      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_SMTP_PASSWORD + " TEXT DEFAULT NULL;")

      sqLiteDatabase.setTransactionSuccessful()
    } finally {
      sqLiteDatabase.endTransaction()
    }
  }

  private fun upgradeDatabaseFrom4To5Version(sqLiteDatabase: SQLiteDatabase) {
    sqLiteDatabase.beginTransaction()
    try {
      sqLiteDatabase.execSQL(AccountAliasesDaoSource.ACCOUNTS_ALIASES_TABLE_SQL_CREATE)
      sqLiteDatabase.execSQL(AccountAliasesDaoSource.CREATE_INDEX_EMAIL_TYPE_IN_ACCOUNTS_ALIASES)
      sqLiteDatabase.setTransactionSuccessful()
    } finally {
      sqLiteDatabase.endTransaction()
    }
  }

  private fun upgradeDatabaseFrom5To6Version(sqLiteDatabase: SQLiteDatabase) {
    sqLiteDatabase.beginTransaction()
    try {
      sqLiteDatabase.execSQL("DROP INDEX IF EXISTS email_account_type_in_accounts_aliases")
      sqLiteDatabase.execSQL(AccountAliasesDaoSource.CREATE_INDEX_EMAIL_TYPE_IN_ACCOUNTS_ALIASES)
      sqLiteDatabase.setTransactionSuccessful()
    } finally {
      sqLiteDatabase.endTransaction()
    }
  }

  private fun upgradeDatabaseFrom6To7Version(sqLiteDatabase: SQLiteDatabase) {
    sqLiteDatabase.beginTransaction()
    try {
      sqLiteDatabase.execSQL(ActionQueueDaoSource.ACTION_QUEUE_TABLE_SQL_CREATE)
      sqLiteDatabase.setTransactionSuccessful()
    } finally {
      sqLiteDatabase.endTransaction()
    }
  }

  private fun upgradeDatabaseFrom7To8Version(sqLiteDatabase: SQLiteDatabase) {
    sqLiteDatabase.beginTransaction()
    try {
      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_IS_CONTACTS_LOADED + " INTEGER DEFAULT 0;")
      sqLiteDatabase.setTransactionSuccessful()
    } finally {
      sqLiteDatabase.endTransaction()
    }
  }

  private fun upgradeDatabaseFrom8To9Version(sqLiteDatabase: SQLiteDatabase) {
    sqLiteDatabase.beginTransaction()
    try {
      sqLiteDatabase.execSQL("ALTER TABLE " + MessageDaoSource.TABLE_NAME_MESSAGES +
          " ADD COLUMN " + MessageDaoSource.COL_IS_ENCRYPTED + " INTEGER DEFAULT -1;")
      sqLiteDatabase.execSQL("ALTER TABLE " + MessageDaoSource.TABLE_NAME_MESSAGES +
          " ADD COLUMN " + MessageDaoSource.COL_CC_ADDRESSES + " TEXT DEFAULT NULL;")
      sqLiteDatabase.execSQL("ALTER TABLE " + MessageDaoSource.TABLE_NAME_MESSAGES +
          " ADD COLUMN " + MessageDaoSource.COL_IS_NEW + " INTEGER DEFAULT 0;")

      sqLiteDatabase.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
          " ADD COLUMN " + AccountDaoSource.COL_IS_SHOW_ONLY_ENCRYPTED + " INTEGER DEFAULT 0;")

      sqLiteDatabase.execSQL(UserIdEmailsKeysDaoSource.SQL_CREATE_TABLE)
      sqLiteDatabase.execSQL(UserIdEmailsKeysDaoSource.INDEX_LONG_ID_USER_ID_EMAIL)
      ActionQueueDaoSource().addAction(sqLiteDatabase, FillUserIdEmailsKeysTableAction())

      sqLiteDatabase.setTransactionSuccessful()
    } finally {
      sqLiteDatabase.endTransaction()
    }
  }

  private fun upgradeDatabaseFrom9To10Version(sqLiteDatabase: SQLiteDatabase) {
    sqLiteDatabase.beginTransaction()
    try {
      sqLiteDatabase.execSQL("ALTER TABLE " + MessageDaoSource.TABLE_NAME_MESSAGES +
          " ADD COLUMN " + MessageDaoSource.COL_STATE + " INTEGER DEFAULT -1;")
      sqLiteDatabase.setTransactionSuccessful()
    } finally {
      sqLiteDatabase.endTransaction()
    }
  }

  private fun upgradeDatabaseFrom10To11Version(sqLiteDatabase: SQLiteDatabase) {
    sqLiteDatabase.beginTransaction()
    try {
      if (sqLiteDatabase.version >= 3) {
        sqLiteDatabase.execSQL("ALTER TABLE " + AttachmentDaoSource.TABLE_NAME_ATTACHMENT +
            " ADD COLUMN " + AttachmentDaoSource.COL_FORWARDED_FOLDER + " TEXT;")
        sqLiteDatabase.execSQL("ALTER TABLE " + AttachmentDaoSource.TABLE_NAME_ATTACHMENT +
            " ADD COLUMN " + AttachmentDaoSource.COL_FORWARDED_UID + " INTEGER DEFAULT -1;")
      }

      sqLiteDatabase.execSQL("ALTER TABLE " + MessageDaoSource.TABLE_NAME_MESSAGES +
          " ADD COLUMN " + MessageDaoSource.COL_ATTACHMENTS_DIRECTORY + " TEXT;")
      sqLiteDatabase.setTransactionSuccessful()
    } finally {
      sqLiteDatabase.endTransaction()
    }
  }

  private fun upgradeDatabaseFrom11To12Version(sqLiteDatabase: SQLiteDatabase) {
    sqLiteDatabase.beginTransaction()
    try {
      sqLiteDatabase.execSQL("ALTER TABLE " + MessageDaoSource.TABLE_NAME_MESSAGES +
          " ADD COLUMN " + MessageDaoSource.COL_ERROR_MSG + " TEXT DEFAULT NULL;")
      sqLiteDatabase.setTransactionSuccessful()
    } finally {
      sqLiteDatabase.endTransaction()
    }
  }

  @Suppress("UNUSED_PARAMETER")
  private fun upgradeDatabaseFrom12To13Version(sqLiteDatabase: SQLiteDatabase) {
    //removed as redundant
  }

  /**
   * This method makes changes in the database only if we are upgrading from version 13 to 14. Because upgrading to
   * 13 version had a wrong index.
   *
   * @param sqLiteDatabase The given [SQLiteDatabase] object.
   */
  private fun upgradeDatabaseFrom13To14Version(sqLiteDatabase: SQLiteDatabase) {
    if (sqLiteDatabase.version == 13) {
      upgradeDatabaseFrom12To13Version(sqLiteDatabase)
    }
  }

  private fun upgradeDatabaseFrom14To15Version(sqLiteDatabase: SQLiteDatabase) {
    sqLiteDatabase.beginTransaction()
    try {
      //delete non-OUTBOX attachments
      sqLiteDatabase.delete(AttachmentDaoSource.TABLE_NAME_ATTACHMENT, AttachmentDaoSource.COL_FOLDER
          + " NOT IN (?)", arrayOf(JavaEmailConstants.FOLDER_OUTBOX))

      val tempTableName = "att"

      sqLiteDatabase.execSQL(CREATE_TEMP_TABLE_IF_NOT_EXISTS + tempTableName + " AS SELECT * FROM "
          + AttachmentDaoSource.TABLE_NAME_ATTACHMENT)

      sqLiteDatabase.execSQL(DROP_TABLE + AttachmentDaoSource.TABLE_NAME_ATTACHMENT)
      sqLiteDatabase.execSQL(AttachmentDaoSource.ATTACHMENT_TABLE_SQL_CREATE)
      sqLiteDatabase.execSQL(AttachmentDaoSource.CREATE_UNIQUE_INDEX_EMAIL_UID_FOLDER_ATTACHMENT_IN_ATTACHMENT)

      sqLiteDatabase.execSQL("INSERT INTO " + AttachmentDaoSource.TABLE_NAME_ATTACHMENT
          + " SELECT *, 0 FROM " + tempTableName)
      sqLiteDatabase.execSQL(DROP_TABLE + tempTableName)
      sqLiteDatabase.setTransactionSuccessful()
    } finally {
      sqLiteDatabase.endTransaction()
    }
  }

  private fun upgradeDatabaseFrom15To16Version(sqLiteDatabase: SQLiteDatabase) {
    sqLiteDatabase.beginTransaction()
    try {
      sqLiteDatabase.delete(MessageDaoSource.TABLE_NAME_MESSAGES,
          MessageDaoSource.COL_FOLDER + " NOT IN(?,?) ", arrayOf("INBOX", "Outbox"))
      sqLiteDatabase.delete(AttachmentDaoSource.TABLE_NAME_ATTACHMENT,
          AttachmentDaoSource.COL_FOLDER + " NOT IN(?,?) ", arrayOf("INBOX", "Outbox"))

      val contentValues = ContentValues()
      contentValues.putNull(MessageDaoSource.COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS)
      sqLiteDatabase.update(MessageDaoSource.TABLE_NAME_MESSAGES, contentValues,
          MessageDaoSource.COL_FOLDER + " = ? ", arrayOf("INBOX"))
      sqLiteDatabase.setTransactionSuccessful()
    } finally {
      sqLiteDatabase.endTransaction()
    }
  }

  private fun upgradeDatabaseFrom16To17Version(sqLiteDatabase: SQLiteDatabase) {
    sqLiteDatabase.beginTransaction()
    try {
      sqLiteDatabase.execSQL("ALTER TABLE " + MessageDaoSource.TABLE_NAME_MESSAGES +
          " ADD COLUMN " + MessageDaoSource.COL_REPLY_TO + " TEXT DEFAULT NULL;")
      sqLiteDatabase.setTransactionSuccessful()
    } finally {
      sqLiteDatabase.endTransaction()
    }
  }

  companion object {
    const val COLUMN_NAME_COUNT = "COUNT(*)"
    const val DB_NAME = "flowcrypt.db"
    const val DB_VERSION = 17

    private val TAG = FlowCryptSQLiteOpenHelper::class.java.simpleName
    private const val DROP_TABLE = "DROP TABLE IF EXISTS "
    private const val CREATE_TABLE_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS "
    private const val CREATE_TEMP_TABLE_IF_NOT_EXISTS = "CREATE TEMP TABLE IF NOT EXISTS "

    @JvmStatic
    fun dropTable(sqLiteDatabase: SQLiteDatabase, tableName: String) {
      sqLiteDatabase.execSQL(DROP_TABLE + tableName)
    }

    @Volatile
    private var INSTANCE: FlowCryptSQLiteOpenHelper? = null

    @JvmStatic
    fun getInstance(context: Context): FlowCryptSQLiteOpenHelper {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: FlowCryptSQLiteOpenHelper(context).also { INSTANCE = it }
      }
    }
  }
}
