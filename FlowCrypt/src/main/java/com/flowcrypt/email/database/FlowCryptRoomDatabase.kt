/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.database.dao.AccountAliasesDao
import com.flowcrypt.email.database.dao.AccountDao
import com.flowcrypt.email.database.dao.AttachmentDao
import com.flowcrypt.email.database.dao.LabelDao
import com.flowcrypt.email.database.dao.MessageDao
import com.flowcrypt.email.database.dao.UserIdEmailsKeysDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.ActionQueueDao
import com.flowcrypt.email.database.entity.AccountAliasesEntity
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.ActionQueueEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.ContactEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.database.entity.LabelEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.database.entity.UserIdEmailsKeysEntity


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
  abstract fun msgDao(): MessageDao

  abstract fun accountDao(): AccountDao

  abstract fun attachmentDao(): AttachmentDao

  abstract fun labelDao(): LabelDao

  abstract fun accountAliasesDao(): AccountAliasesDao

  abstract fun userIdEmailsKeysDao(): UserIdEmailsKeysDao

  abstract fun actionQueueDao(): ActionQueueDao

  companion object {
    const val DB_NAME = "flowcrypt.db"
    const val DB_VERSION = 21

    private val MIGRATION_1_3 = object : Migration(1, 3) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          database.execSQL("ALTER TABLE messages ADD COLUMN is_message_has_attachments INTEGER DEFAULT 0;")

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
          database.execSQL("CREATE TABLE `accounts_aliases` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `email` TEXT NOT NULL, `account_type` TEXT NOT NULL, `send_as_email` TEXT NOT NULL, `display_name` TEXT DEFAULT NULL, `is_default` INTEGER DEFAULT 0, `verification_status` TEXT NOT NULL)")
          database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS`email_account_type_send_as_email_in_accounts_aliases` ON `accounts_aliases` (`email`, `account_type`, `send_as_email`)")
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
          database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS`email_account_type_send_as_email_in_accounts_aliases` ON `accounts_aliases` (`email`, `account_type`, `send_as_email`)")
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
          database.execSQL("CREATE TABLE `action_queue` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `email` TEXT NOT NULL, `action_type` TEXT NOT NULL, `action_json` TEXT NOT NULL)")
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
          database.execSQL("ALTER TABLE messages ADD COLUMN is_encrypted INTEGER DEFAULT -1;")
          database.execSQL("ALTER TABLE messages ADD COLUMN cc_address TEXT DEFAULT NULL;")
          database.execSQL("ALTER TABLE messages ADD COLUMN is_new INTEGER DEFAULT 0;")

          database.execSQL("ALTER TABLE " + AccountDaoSource.TABLE_NAME_ACCOUNTS +
              " ADD COLUMN " + AccountDaoSource.COL_IS_SHOW_ONLY_ENCRYPTED + " INTEGER DEFAULT 0;")

          database.execSQL("CREATE TABLE user_id_emails_and_keys (_id INTEGER PRIMARY KEY AUTOINCREMENT, long_id TEXT NOT NULL, user_id_email TEXT NOT NULL )")
          database.execSQL("CREATE UNIQUE INDEX long_id_user_id_email_in_user_id_emails_and_keys ON user_id_emails_and_keys (long_id, user_id_email)")
          database.execSQL("INSERT INTO `action_queue` (email,action_type,action_json) VALUES ('system','fill_user_id_emails_keys_table','{\"email\":\"system\",\"id\":0,\"actionType\":\"FILL_USER_ID_EMAILS_KEYS_TABLE\",\"version\":0}');")

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
          val tempTableName = "messages_temp"
          database.execSQL("CREATE TEMP TABLE IF NOT EXISTS $tempTableName AS SELECT * FROM messages;")
          database.execSQL("DROP TABLE IF EXISTS messages;")
          database.execSQL("CREATE TABLE messages (_id INTEGER PRIMARY KEY AUTOINCREMENT, email VARCHAR(100) NOT NULL, folder TEXT NOT NULL, uid INTEGER NOT NULL, received_date INTEGER DEFAULT NULL, sent_date INTEGER DEFAULT NULL, from_address TEXT DEFAULT NULL, to_address TEXT DEFAULT NULL, cc_address TEXT DEFAULT NULL, subject TEXT DEFAULT NULL, flags TEXT DEFAULT NULL, raw_message_without_attachments TEXT DEFAULT NULL, is_message_has_attachments INTEGER DEFAULT 0, is_encrypted INTEGER DEFAULT -1, is_new INTEGER DEFAULT -1 )")
          database.execSQL("CREATE INDEX email_in_messages ON messages (email)")
          database.execSQL("CREATE UNIQUE INDEX email_uid_folder_in_messages ON messages (email, uid, folder)")
          database.execSQL("INSERT INTO messages SELECT * FROM $tempTableName;")
          database.execSQL("DROP TABLE IF EXISTS $tempTableName;")
          database.execSQL("ALTER TABLE messages ADD COLUMN state INTEGER DEFAULT -1;")
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
          database.execSQL("ALTER TABLE attachment ADD COLUMN forwarded_folder TEXT;")
          database.execSQL("ALTER TABLE attachment ADD COLUMN forwarded_uid INTEGER DEFAULT -1;")
          database.execSQL("ALTER TABLE messages ADD COLUMN attachments_directory TEXT;")
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
          database.execSQL("ALTER TABLE messages ADD COLUMN error_msg TEXT DEFAULT NULL;")
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
          database.delete("attachment", "folder NOT IN (?)", arrayOf(JavaEmailConstants.FOLDER_OUTBOX))

          val tempTableName = "attachment_temp"

          database.execSQL("CREATE TEMP TABLE IF NOT EXISTS $tempTableName AS SELECT * FROM attachment")
          database.execSQL("DROP TABLE IF EXISTS attachment")
          database.execSQL("CREATE TABLE `attachment` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `email` TEXT NOT NULL, `folder` TEXT NOT NULL, `uid` INTEGER NOT NULL, `name` TEXT NOT NULL, `encodedSize` INTEGER DEFAULT 0, `type` TEXT NOT NULL, `attachment_id` TEXT, `file_uri` TEXT, `forwarded_folder` TEXT, `forwarded_uid` INTEGER DEFAULT -1, `path` TEXT NOT NULL)")
          database.execSQL("CREATE UNIQUE INDEX `email_uid_folder_path_in_attachment` ON `attachment` (`email`, `uid`, `folder`, `path`)")

          database.execSQL("INSERT INTO attachment SELECT *, 0 FROM $tempTableName")
          database.execSQL("DROP TABLE IF EXISTS $tempTableName")
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
          database.delete("messages", "folder NOT IN(?,?)", arrayOf("INBOX", "Outbox"))
          database.delete("attachment", "folder NOT IN(?,?)", arrayOf("INBOX", "Outbox"))

          val contentValues = ContentValues()
          contentValues.putNull("raw_message_without_attachments")
          database.update("messages", SQLiteDatabase.CONFLICT_NONE, contentValues, "folder = ? ", arrayOf("INBOX"))
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
          database.execSQL("ALTER TABLE messages ADD COLUMN reply_to TEXT DEFAULT NULL;")
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

    @VisibleForTesting
    val MIGRATION_19_20 = object : Migration(19, 20) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          //to prevent problems in users which have an app since database version = 9 or lower
          //(default value for 'is_new' should = -1) we have to recreate 'messages' table
          val tempTableMsgs = "messages_temp"
          database.execSQL("CREATE TEMP TABLE IF NOT EXISTS $tempTableMsgs AS SELECT * FROM messages;")
          database.execSQL("DROP TABLE IF EXISTS messages;")
          database.execSQL("CREATE TABLE messages (_id INTEGER PRIMARY KEY AUTOINCREMENT, email VARCHAR(100) NOT NULL, folder TEXT NOT NULL, uid INTEGER NOT NULL, received_date INTEGER DEFAULT NULL, sent_date INTEGER DEFAULT NULL, from_address TEXT DEFAULT NULL, to_address TEXT DEFAULT NULL, cc_address TEXT DEFAULT NULL, subject TEXT DEFAULT NULL, flags TEXT DEFAULT NULL, raw_message_without_attachments TEXT DEFAULT NULL, is_message_has_attachments INTEGER DEFAULT 0, is_encrypted INTEGER DEFAULT -1, is_new INTEGER DEFAULT -1 , state INTEGER DEFAULT -1, attachments_directory TEXT, error_msg TEXT DEFAULT NULL, reply_to TEXT DEFAULT NULL)")
          database.execSQL("CREATE INDEX email_in_messages ON messages (email)")
          database.execSQL("CREATE UNIQUE INDEX email_uid_folder_in_messages ON messages (email, uid, folder)")
          database.execSQL("INSERT INTO messages SELECT * FROM $tempTableMsgs;")
          database.execSQL("DROP TABLE IF EXISTS $tempTableMsgs;")

          //recreate 'contacts' table because of wrong column type BOOLEAN
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
          val tempTableAtts = "attachment_temp"

          database.execSQL("CREATE TEMP TABLE IF NOT EXISTS $tempTableAtts AS SELECT * FROM attachment;")
          database.execSQL("DROP TABLE IF EXISTS attachment;")
          database.execSQL("CREATE TABLE `attachment` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `email` TEXT NOT NULL, `folder` TEXT NOT NULL, `uid` INTEGER NOT NULL, `name` TEXT NOT NULL, `encodedSize` INTEGER DEFAULT 0, `type` TEXT NOT NULL, `attachment_id` TEXT, `file_uri` TEXT, `forwarded_folder` TEXT, `forwarded_uid` INTEGER DEFAULT -1, `path` TEXT NOT NULL, FOREIGN KEY(`email`, `folder`, `uid`) REFERENCES `messages`(`email`, `folder`, `uid`) ON UPDATE NO ACTION ON DELETE CASCADE );")
          database.execSQL("CREATE UNIQUE INDEX `email_uid_folder_path_in_attachment` ON `attachment` (`email`, `uid`, `folder`, `path`);")
          database.execSQL("CREATE INDEX `email_folder_uid_in_attachment` ON `attachment` (`email`, `folder`, `uid`);")
          database.execSQL("INSERT INTO attachment SELECT * FROM $tempTableAtts;")
          database.execSQL("DROP TABLE IF EXISTS $tempTableAtts;")

          database.setTransactionSuccessful()
        } finally {
          database.endTransaction()
        }
      }
    }

    @VisibleForTesting
    val MIGRATION_20_21 = object : Migration(20, 21) {
      override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
          //Recreate 'accounts_aliases' table to use an ability of foreign keys
          database.execSQL("DROP TABLE IF EXISTS `accounts_aliases`;")
          database.execSQL("CREATE TABLE `accounts_aliases` (`_id` INTEGER PRIMARY KEY AUTOINCREMENT, `email` TEXT NOT NULL, `account_type` TEXT NOT NULL, `send_as_email` TEXT NOT NULL, `display_name` TEXT DEFAULT NULL, `is_default` INTEGER DEFAULT 0, `verification_status` TEXT NOT NULL, FOREIGN KEY(`email`, `account_type`) REFERENCES `accounts`(`email`, `account_type`) ON UPDATE NO ACTION ON DELETE CASCADE )")
          database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS`email_account_type_send_as_email_in_accounts_aliases` ON `accounts_aliases` (`email`, `account_type`, `send_as_email`)")
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
                MIGRATION_19_20,
                MIGRATION_20_21)
            .build()
        INSTANCE = instance
        return instance
      }
    }
  }
}