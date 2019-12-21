/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.database.dao.AccountDao
import com.flowcrypt.email.database.dao.AttachmentDao
import com.flowcrypt.email.database.dao.LabelDao
import com.flowcrypt.email.database.dao.MessagesDao
import com.flowcrypt.email.database.dao.source.AccountAliasesDaoSource
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.ActionQueueDaoSource
import com.flowcrypt.email.database.dao.source.UserIdEmailsKeysDaoSource
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource
import com.flowcrypt.email.database.entity.AccountAliasesEntity
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.ActionQueueEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.ContactEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.database.entity.LabelEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.database.entity.UserIdEmailsKeysEntity
import com.flowcrypt.email.service.actionqueue.actions.FillUserIdEmailsKeysTableAction


/**
 * A helper class to manage database creation and version management which describes an
 * implementation of [RoomDatabase]
 *
 * @author DenBond7
 * Date: 13.05.2017
 * Time: 12:20
 * E-mail: DenBond7@gmail.com
 */
@Database(entities =
[
  AccountAliasesEntity::class,
  AccountEntity::class,
  ActionQueueEntity::class,
  AttachmentEntity::class,
  ContactEntity::class,
  KeyEntity::class,
  LabelEntity::class,
  MessageEntity::class,
  UserIdEmailsKeysEntity::class
],
    version = FlowCryptRoomDatabase.DB_VERSION)
abstract class FlowCryptRoomDatabase : RoomDatabase() {
  abstract fun msgDao(): MessagesDao

  abstract fun accountDao(): AccountDao

  abstract fun attachmentDao(): AttachmentDao

  abstract fun labelDao(): LabelDao

  companion object {
    const val COLUMN_NAME_COUNT = "COUNT(*)"
    private const val DB_NAME = "flowcrypt.db"
    private const val DROP_TABLE = "DROP TABLE IF EXISTS "
    private const val CREATE_TEMP_TABLE_IF_NOT_EXISTS = "CREATE TEMP TABLE IF NOT EXISTS "

    const val DB_VERSION = 20

    private val MIGRATION_1_3 = object : Migration(1, 3) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          database.execSQL("ALTER TABLE " + MessageDaoSource.TABLE_NAME_MESSAGES +
              " ADD COLUMN " + MessageDaoSource.COL_IS_MESSAGE_HAS_ATTACHMENTS + " INTEGER DEFAULT 0;")

          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_IS_ENABLE + " INTEGER DEFAULT 1;")

          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_IS_ACTIVE + " INTEGER DEFAULT 0;")
          database.setTransactionSuccessful()
        } finally {
          database.endTransaction()
        }
      }
    }
    private val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_USERNAME + " TEXT NOT NULL DEFAULT '';")
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_PASSWORD + " TEXT NOT NULL DEFAULT '';")
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_IMAP_SERVER + " TEXT NOT NULL DEFAULT '';")
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_IMAP_PORT + " INTEGER DEFAULT 143;")
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_IMAP_IS_USE_SSL_TLS + " INTEGER DEFAULT 0;")
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_IMAP_IS_USE_STARTTLS + " INTEGER DEFAULT 0;")
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_IMAP_AUTH_MECHANISMS + " TEXT;")
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_SMTP_SERVER + " TEXT NOT NULL DEFAULT '';")
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_SMTP_PORT + " INTEGER DEFAULT 25;")
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_SMTP_IS_USE_SSL_TLS + " INTEGER DEFAULT 0;")
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_SMTP_IS_USE_STARTTLS + " INTEGER DEFAULT 0;")
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_SMTP_AUTH_MECHANISMS + " TEXT;")
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_SMTP_IS_USE_CUSTOM_SIGN + " INTEGER DEFAULT 0;")
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_SMTP_USERNAME + " TEXT DEFAULT NULL;")
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_SMTP_PASSWORD + " TEXT DEFAULT NULL;")
          database.setTransactionSuccessful()
        } finally {
          database.endTransaction()
        }
      }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          database.execSQL(AccountAliasesDaoSource.ACCOUNTS_ALIASES_TABLE_SQL_CREATE)
          database.execSQL(AccountAliasesDaoSource.CREATE_INDEX_EMAIL_TYPE_IN_ACCOUNTS_ALIASES)
          database.setTransactionSuccessful()
        } finally {
          database.endTransaction()
        }
      }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          database.execSQL("DROP INDEX IF EXISTS email_account_type_in_accounts_aliases")
          database.execSQL(AccountAliasesDaoSource.CREATE_INDEX_EMAIL_TYPE_IN_ACCOUNTS_ALIASES)
          database.setTransactionSuccessful()
        } finally {
          database.endTransaction()
        }
      }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          database.execSQL(ActionQueueDaoSource.ACTION_QUEUE_TABLE_SQL_CREATE)
          database.setTransactionSuccessful()
        } finally {
          database.endTransaction()
        }
      }
    }

    private val MIGRATION_7_8 = object : Migration(7, 8) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_IS_CONTACTS_LOADED + " INTEGER DEFAULT 0;")
          database.setTransactionSuccessful()
        } finally {
          database.endTransaction()
        }
      }
    }

    private val MIGRATION_8_9 = object : Migration(8, 9) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          database.execSQL("ALTER TABLE " + MessageDaoSource.TABLE_NAME_MESSAGES +
              " ADD COLUMN " + MessageDaoSource.COL_IS_ENCRYPTED + " INTEGER DEFAULT -1;")
          database.execSQL("ALTER TABLE " + MessageDaoSource.TABLE_NAME_MESSAGES +
              " ADD COLUMN " + MessageDaoSource.COL_CC_ADDRESSES + " TEXT DEFAULT NULL;")
          database.execSQL("ALTER TABLE " + MessageDaoSource.TABLE_NAME_MESSAGES +
              " ADD COLUMN " + MessageDaoSource.COL_IS_NEW + " INTEGER DEFAULT 0;")

          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_IS_SHOW_ONLY_ENCRYPTED + " INTEGER DEFAULT 0;")

          database.execSQL(UserIdEmailsKeysDaoSource.SQL_CREATE_TABLE)
          database.execSQL(UserIdEmailsKeysDaoSource.INDEX_LONG_ID_USER_ID_EMAIL)
          ActionQueueDaoSource().addAction(database, FillUserIdEmailsKeysTableAction())

          database.setTransactionSuccessful()
        } finally {
          database.endTransaction()
        }
      }
    }

    private val MIGRATION_9_10 = object : Migration(9, 10) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          database.execSQL("ALTER TABLE " + MessageDaoSource.TABLE_NAME_MESSAGES +
              " ADD COLUMN " + MessageDaoSource.COL_STATE + " INTEGER DEFAULT -1;")
          database.setTransactionSuccessful()
        } finally {
          database.endTransaction()
        }
      }
    }

    private val MIGRATION_10_11 = object : Migration(10, 11) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          database.execSQL("ALTER TABLE " + AttachmentDaoSource.TABLE_NAME_ATTACHMENT +
              " ADD COLUMN " + AttachmentDaoSource.COL_FORWARDED_FOLDER + " TEXT;")
          database.execSQL("ALTER TABLE " + AttachmentDaoSource.TABLE_NAME_ATTACHMENT +
              " ADD COLUMN " + AttachmentDaoSource.COL_FORWARDED_UID + " INTEGER DEFAULT -1;")
          database.execSQL("ALTER TABLE " + MessageDaoSource.TABLE_NAME_MESSAGES +
              " ADD COLUMN " + MessageDaoSource.COL_ATTACHMENTS_DIRECTORY + " TEXT;")
          database.setTransactionSuccessful()
        } finally {
          database.endTransaction()
        }
      }
    }

    private val MIGRATION_11_12 = object : Migration(11, 12) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          database.execSQL("ALTER TABLE " + MessageDaoSource.TABLE_NAME_MESSAGES +
              " ADD COLUMN " + MessageDaoSource.COL_ERROR_MSG + " TEXT DEFAULT NULL;")
          database.setTransactionSuccessful()
        } finally {
          database.endTransaction()
        }
      }
    }

    private val MIGRATION_12_14 = object : Migration(12, 14) {
      override fun migrate(database: SupportSQLiteDatabase) {
        //just empty migration. There is no code because it is redundant.
      }
    }

    private val MIGRATION_14_15 = object : Migration(14, 15) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          //delete non-OUTBOX attachments
          database.delete(AttachmentDaoSource.TABLE_NAME_ATTACHMENT, AttachmentDaoSource.COL_FOLDER
              + " NOT IN (?)", arrayOf(JavaEmailConstants.FOLDER_OUTBOX))

          val tempTableName = "att"

          database.execSQL(CREATE_TEMP_TABLE_IF_NOT_EXISTS + tempTableName + " AS SELECT * FROM "
              + AttachmentDaoSource.TABLE_NAME_ATTACHMENT)

          database.execSQL(DROP_TABLE + AttachmentDaoSource.TABLE_NAME_ATTACHMENT)
          database.execSQL(AttachmentDaoSource.ATTACHMENT_TABLE_SQL_CREATE)
          database.execSQL(AttachmentDaoSource.CREATE_UNIQUE_INDEX_EMAIL_UID_FOLDER_PATH_IN_ATTACHMENT)

          database.execSQL("INSERT INTO " + AttachmentDaoSource.TABLE_NAME_ATTACHMENT
              + " SELECT *, 0 FROM " + tempTableName)
          database.execSQL(DROP_TABLE + tempTableName)
          database.setTransactionSuccessful()
        } finally {
          database.endTransaction()
        }
      }
    }

    private val MIGRATION_15_16 = object : Migration(15, 16) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          database.delete(MessageDaoSource.TABLE_NAME_MESSAGES,
              MessageDaoSource.COL_FOLDER + " NOT IN(?,?) ", arrayOf("INBOX", "Outbox"))
          database.delete(AttachmentDaoSource.TABLE_NAME_ATTACHMENT,
              AttachmentDaoSource.COL_FOLDER + " NOT IN(?,?) ", arrayOf("INBOX", "Outbox"))

          val contentValues = ContentValues()
          contentValues.putNull(MessageDaoSource.COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS)
          database.update(MessageDaoSource.TABLE_NAME_MESSAGES, SQLiteDatabase.CONFLICT_NONE,
              contentValues, MessageDaoSource.COL_FOLDER + " = ? ", arrayOf("INBOX"))
          database.setTransactionSuccessful()
        } finally {
          database.endTransaction()
        }
      }
    }

    private val MIGRATION_16_17 = object : Migration(16, 17) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          database.execSQL("ALTER TABLE " + MessageDaoSource.TABLE_NAME_MESSAGES +
              " ADD COLUMN " + MessageDaoSource.COL_REPLY_TO + " TEXT DEFAULT NULL;")
          database.setTransactionSuccessful()
        } finally {
          database.endTransaction()
        }
      }
    }

    private val MIGRATION_17_18 = object : Migration(17, 18) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_UUID + " TEXT DEFAULT NULL;")
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_DOMAIN_RULES + " TEXT DEFAULT NULL;")
          database.setTransactionSuccessful()
        } finally {
          database.endTransaction()
        }
      }
    }

    private val MIGRATION_18_19 = object : Migration(18, 19) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_IS_RESTORE_ACCESS_REQUIRED + " INTEGER DEFAULT 0;")
          database.setTransactionSuccessful()
        } finally {
          database.endTransaction()
        }
      }
    }

    private val MIGRATION_19_20 = object : Migration(19, DB_VERSION) {
      override fun migrate(database: SupportSQLiteDatabase) {
        //recreate 'contacts' table because of wrong column type BOOLEAN
        database.beginTransaction()
        try {
          database.execSQL("CREATE TEMP TABLE IF NOT EXISTS contacts_temp AS SELECT * FROM contacts;")
          database.execSQL("DROP TABLE IF EXISTS contacts;")
          database.execSQL("CREATE TABLE IF NOT EXISTS contacts (_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, email TEXT NOT NULL, name TEXT DEFAULT NULL, public_key BLOB DEFAULT NULL, has_pgp INTEGER NOT NULL, client TEXT DEFAULT NULL, attested INTEGER DEFAULT NULL, fingerprint TEXT DEFAULT NULL, long_id TEXT DEFAULT NULL, keywords TEXT DEFAULT NULL, last_use INTEGER DEFAULT 0 NOT NULL);")
          database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS email_in_contacts ON contacts (email);")
          database.execSQL("CREATE INDEX IF NOT EXISTS name_in_contacts ON contacts (name);")
          database.execSQL("CREATE INDEX IF NOT EXISTS has_pgp_in_contacts ON contacts (has_pgp);")
          database.execSQL("CREATE INDEX IF NOT EXISTS long_id_in_contacts ON contacts (long_id);")
          database.execSQL("CREATE INDEX IF NOT EXISTS last_use_in_contacts ON contacts (last_use);")
          database.execSQL("INSERT INTO contacts SELECT * FROM contacts_temp;")
          database.execSQL("DROP TABLE IF EXISTS contacts_temp;")

          //Recreate 'attachment' table to use an ability of foreign keys
          //delete non-OUTBOX attachments
          database.delete("attachment", "folder NOT IN (?)", arrayOf(JavaEmailConstants.FOLDER_OUTBOX))
          val tempTableName = "attachment_temp"

          database.execSQL("CREATE TEMP TABLE IF NOT EXISTS $tempTableName AS SELECT * FROM attachment;")
          database.execSQL("DROP TABLE IF EXISTS attachment;")
          database.execSQL("CREATE TABLE `attachment` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `email` TEXT NOT NULL, `folder` TEXT NOT NULL, `uid` INTEGER NOT NULL, `name` TEXT NOT NULL, `encodedSize` INTEGER DEFAULT 0, `type` TEXT NOT NULL, `attachment_id` TEXT, `file_uri` TEXT, `forwarded_folder` TEXT, `forwarded_uid` INTEGER DEFAULT -1, `path` TEXT NOT NULL, FOREIGN KEY(`email`, `folder`, `uid`) REFERENCES `messages`(`email`, `folder`, `uid`) ON UPDATE NO ACTION ON DELETE CASCADE );")
          database.execSQL("CREATE UNIQUE INDEX `email_uid_folder_path_in_attachment` ON `attachment` (`email`, `uid`, `folder`, `path`);")
          database.execSQL("CREATE INDEX `email_folder_uid_in_attachment` ON `attachment` (`email`, `folder`, `uid`);")
          database.execSQL("INSERT INTO attachment SELECT * FROM $tempTableName;")
          database.execSQL("DROP TABLE IF EXISTS $tempTableName;")

          database.setTransactionSuccessful()
        } finally {
          database.endTransaction()
        }
      }
    }

    // Singleton prevents multiple instances of database opening at the same time.
    @Volatile
    private var INSTANCE: FlowCryptRoomDatabase? = null

    fun getDatabase(context: Context): FlowCryptRoomDatabase {
      val tempInstance = INSTANCE
      if (tempInstance != null) {
        return tempInstance
      }

      synchronized(this) {
        val instance = Room.databaseBuilder(
            context.applicationContext,
            FlowCryptRoomDatabase::class.java,
            DB_NAME)
            .addMigrations(
                MIGRATION_1_3,
                MIGRATION_3_4,
                MIGRATION_4_5,
                MIGRATION_5_6,
                MIGRATION_6_7,
                MIGRATION_7_8,
                MIGRATION_8_9,
                MIGRATION_9_10,
                MIGRATION_10_11,
                MIGRATION_11_12,
                MIGRATION_12_14,
                MIGRATION_14_15,
                MIGRATION_15_16,
                MIGRATION_16_17,
                MIGRATION_17_18,
                MIGRATION_18_19,
                MIGRATION_19_20)
            .build()
        INSTANCE = instance
        return instance
      }
    }
  }
}