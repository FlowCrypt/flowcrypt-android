/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.database.converters.ClientConfigurationConverter
import com.flowcrypt.email.database.converters.PassphraseTypeConverter
import com.flowcrypt.email.database.dao.AccountAliasesDao
import com.flowcrypt.email.database.dao.AccountDao
import com.flowcrypt.email.database.dao.ActionQueueDao
import com.flowcrypt.email.database.dao.AttachmentDao
import com.flowcrypt.email.database.dao.KeysDao
import com.flowcrypt.email.database.dao.LabelDao
import com.flowcrypt.email.database.dao.MessageDao
import com.flowcrypt.email.database.dao.PubKeyDao
import com.flowcrypt.email.database.dao.RecipientDao
import com.flowcrypt.email.database.entity.AccountAliasesEntity
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.ActionQueueEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.database.entity.LabelEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.security.pgp.PgpKey
import org.pgpainless.key.OpenPgpV4Fingerprint


/**
 * A helper class to manage database creation and version management which describes an
 * implementation of [RoomDatabase]
 *
 * @author Denys Bondarenko
 */
@Database(
  entities = [
    AccountAliasesEntity::class,
    AccountEntity::class,
    ActionQueueEntity::class,
    AttachmentEntity::class,
    RecipientEntity::class,
    KeyEntity::class,
    LabelEntity::class,
    MessageEntity::class,
    PublicKeyEntity::class
  ],
  version = FlowCryptRoomDatabase.DB_VERSION
)
@TypeConverters(PassphraseTypeConverter::class, ClientConfigurationConverter::class)
abstract class FlowCryptRoomDatabase : RoomDatabase() {
  abstract fun msgDao(): MessageDao

  abstract fun accountDao(): AccountDao

  abstract fun attachmentDao(): AttachmentDao

  abstract fun labelDao(): LabelDao

  abstract fun accountAliasesDao(): AccountAliasesDao

  abstract fun actionQueueDao(): ActionQueueDao

  abstract fun keysDao(): KeysDao

  abstract fun recipientDao(): RecipientDao

  abstract fun pubKeyDao(): PubKeyDao

  @WorkerThread
  fun forceDatabaseCreationIfNeeded() {
    super.openHelper.readableDatabase
  }

  companion object {
    const val DB_NAME = "flowcrypt.db"
    const val DB_VERSION = 38

    private val MIGRATION_1_3 = object : FlowCryptMigration(1, 3) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL(
          "ALTER TABLE messages " +
              "ADD COLUMN is_message_has_attachments INTEGER DEFAULT 0;"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN is_enable INTEGER DEFAULT 1;"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN is_active INTEGER DEFAULT 0;"
        )
      }
    }
    private val MIGRATION_3_4 = object : FlowCryptMigration(3, 4) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN username TEXT NOT NULL DEFAULT '';"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN password TEXT NOT NULL DEFAULT '';"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN imap_server TEXT NOT NULL DEFAULT '';"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN imap_port INTEGER DEFAULT 143;"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN imap_is_use_ssl_tls INTEGER DEFAULT 0;"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN imap_is_use_starttls INTEGER DEFAULT 0;"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN imap_auth_mechanisms TEXT;"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN smtp_server TEXT NOT NULL DEFAULT '';"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN smtp_port INTEGER DEFAULT 25;"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN smtp_is_use_ssl_tls INTEGER DEFAULT 0;"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN smtp_is_use_starttls INTEGER DEFAULT 0;"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN smtp_auth_mechanisms TEXT;"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN smtp_is_use_custom_sign INTEGER DEFAULT 0;"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN smtp_username TEXT DEFAULT NULL;"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN smtp_password TEXT DEFAULT NULL;"
        )
      }
    }

    private val MIGRATION_4_5 = object : FlowCryptMigration(4, 5) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL(
          "CREATE TABLE `accounts_aliases` (" +
              "`_id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "`email` TEXT NOT NULL, " +
              "`account_type` TEXT NOT NULL, " +
              "`send_as_email` TEXT NOT NULL, " +
              "`display_name` TEXT DEFAULT NULL, " +
              "`is_default` INTEGER DEFAULT 0, " +
              "`verification_status` TEXT NOT NULL)"
        )
        database.execSQL(
          "CREATE UNIQUE INDEX IF NOT EXISTS " +
              "`email_account_type_send_as_email_in_accounts_aliases` " +
              "ON `accounts_aliases` (`email`, `account_type`, `send_as_email`)"
        )
      }
    }

    private val MIGRATION_5_6 = object : FlowCryptMigration(5, 6) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL("DROP INDEX IF EXISTS email_account_type_in_accounts_aliases")
        database.execSQL(
          "CREATE UNIQUE INDEX IF NOT EXISTS " +
              "`email_account_type_send_as_email_in_accounts_aliases` " +
              "ON `accounts_aliases` (`email`, `account_type`, `send_as_email`)"
        )
      }
    }

    private val MIGRATION_6_7 = object : FlowCryptMigration(6, 7) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL(
          "CREATE TABLE `action_queue` (" +
              "`_id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "`email` TEXT NOT NULL, " +
              "`action_type` TEXT NOT NULL, " +
              "`action_json` TEXT NOT NULL)"
        )
      }
    }

    private val MIGRATION_7_8 = object : FlowCryptMigration(7, 8) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN ic_contacts_loaded INTEGER DEFAULT 0;"
        )
      }
    }

    private val MIGRATION_8_9 = object : FlowCryptMigration(8, 9) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL(
          "ALTER TABLE messages " +
              "ADD COLUMN is_encrypted INTEGER DEFAULT -1;"
        )
        database.execSQL(
          "ALTER TABLE messages " +
              "ADD COLUMN cc_address TEXT DEFAULT NULL;"
        )
        database.execSQL(
          "ALTER TABLE messages " +
              "ADD COLUMN is_new INTEGER DEFAULT 0;"
        )

        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN is_show_only_encrypted INTEGER DEFAULT 0;"
        )

        database.execSQL(
          "CREATE TABLE user_id_emails_and_keys (" +
              "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "long_id TEXT NOT NULL, " +
              "user_id_email TEXT NOT NULL )"
        )
        database.execSQL(
          "CREATE UNIQUE INDEX " +
              "long_id_user_id_email_in_user_id_emails_and_keys " +
              "ON user_id_emails_and_keys (long_id, user_id_email)"
        )
        database.execSQL(
          "INSERT INTO `action_queue` (" +
              "email," +
              "action_type," +
              "action_json)" +
              " VALUES (" +
              "'system'," +
              "'fill_user_id_emails_keys_table'," +
              "'{\"email\":\"system\",\"id\":0,\"actionType\":\"FILL_USER_ID_EMAILS_KEYS_TABLE\",\"version\":0}');"
        )
      }
    }

    private val MIGRATION_9_10 = object : FlowCryptMigration(9, 10) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        val tempTableName = "messages_temp"
        database.execSQL(
          "CREATE TEMP TABLE IF NOT EXISTS $tempTableName" +
              " AS SELECT * FROM messages;"
        )
        database.execSQL("DROP TABLE IF EXISTS messages;")
        database.execSQL(
          "CREATE TABLE messages (" +
              "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "email VARCHAR(100) NOT NULL, " +
              "folder TEXT NOT NULL, " +
              "uid INTEGER NOT NULL, " +
              "received_date INTEGER DEFAULT NULL, " +
              "sent_date INTEGER DEFAULT NULL, " +
              "from_address TEXT DEFAULT NULL, " +
              "to_address TEXT DEFAULT NULL, " +
              "cc_address TEXT DEFAULT NULL, " +
              "subject TEXT DEFAULT NULL, " +
              "flags TEXT DEFAULT NULL, " +
              "raw_message_without_attachments TEXT DEFAULT NULL, " +
              "is_message_has_attachments INTEGER DEFAULT 0, " +
              "is_encrypted INTEGER DEFAULT -1, " +
              "is_new INTEGER DEFAULT -1 )"
        )
        database.execSQL("CREATE INDEX email_in_messages ON messages (email)")
        database.execSQL(
          "CREATE UNIQUE INDEX email_uid_folder_in_messages " +
              "ON messages (email, uid, folder)"
        )
        database.execSQL("INSERT INTO messages SELECT * FROM $tempTableName;")
        database.execSQL("DROP TABLE IF EXISTS $tempTableName;")
        database.execSQL(
          "ALTER TABLE messages " +
              "ADD COLUMN state INTEGER DEFAULT -1;"
        )
      }
    }

    private val MIGRATION_10_11 = object : FlowCryptMigration(10, 11) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL(
          "ALTER TABLE attachment " +
              "ADD COLUMN forwarded_folder TEXT;"
        )
        database.execSQL(
          "ALTER TABLE attachment " +
              "ADD COLUMN forwarded_uid INTEGER DEFAULT -1;"
        )
        database.execSQL(
          "ALTER TABLE messages " +
              "ADD COLUMN attachments_directory TEXT;"
        )
      }
    }

    private val MIGRATION_11_12 = object : FlowCryptMigration(11, 12) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL(
          "ALTER TABLE messages " +
              "ADD COLUMN error_msg TEXT DEFAULT NULL;"
        )
      }
    }

    private val MIGRATION_12_13 = object : FlowCryptMigration(12, 13) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        //just empty migration. There is no code because it is redundant.
      }
    }

    private val MIGRATION_13_14 = object : FlowCryptMigration(13, 14) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        //just empty migration. There is no code because it is redundant.
      }
    }

    private val MIGRATION_14_15 = object : FlowCryptMigration(14, 15) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.delete(
          "attachment",
          "folder NOT IN (?)",
          arrayOf(JavaEmailConstants.FOLDER_OUTBOX)
        )

        val tempTableName = "attachment_temp"

        database.execSQL(
          "CREATE TEMP TABLE IF NOT EXISTS $tempTableName " +
              "AS SELECT * FROM attachment"
        )
        database.execSQL("DROP TABLE IF EXISTS attachment")
        database.execSQL(
          "CREATE TABLE `attachment` (" +
              "`_id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "`email` TEXT NOT NULL, " +
              "`folder` TEXT NOT NULL, " +
              "`uid` INTEGER NOT NULL, " +
              "`name` TEXT NOT NULL, " +
              "`encodedSize` INTEGER DEFAULT 0, " +
              "`type` TEXT NOT NULL, " +
              "`attachment_id` TEXT, " +
              "`file_uri` TEXT, " +
              "`forwarded_folder` TEXT, " +
              "`forwarded_uid` INTEGER DEFAULT -1, " +
              "`path` TEXT NOT NULL)"
        )
        database.execSQL(
          "CREATE UNIQUE INDEX `email_uid_folder_path_in_attachment` " +
              "ON `attachment` (`email`, `uid`, `folder`, `path`)"
        )

        database.execSQL(
          "INSERT INTO attachment " +
              "SELECT *, 0 FROM $tempTableName"
        )
        database.execSQL("DROP TABLE IF EXISTS $tempTableName")
      }
    }

    private val MIGRATION_15_16 = object : FlowCryptMigration(15, 16) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.delete("messages", "folder NOT IN(?,?)", arrayOf("INBOX", "Outbox"))
        database.delete("attachment", "folder NOT IN(?,?)", arrayOf("INBOX", "Outbox"))

        val contentValues = ContentValues()
        contentValues.putNull("raw_message_without_attachments")
        database.update(
          "messages",
          SQLiteDatabase.CONFLICT_NONE,
          contentValues,
          "folder = ? ",
          arrayOf("INBOX")
        )
      }
    }

    private val MIGRATION_16_17 = object : FlowCryptMigration(16, 17) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL(
          "ALTER TABLE messages " +
              "ADD COLUMN reply_to TEXT DEFAULT NULL;"
        )
      }
    }

    private val MIGRATION_17_18 = object : FlowCryptMigration(17, 18) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN uuid TEXT DEFAULT NULL;"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN domain_rules TEXT DEFAULT NULL;"
        )
      }
    }

    private val MIGRATION_18_19 = object : FlowCryptMigration(18, 19) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN is_restore_access_required INTEGER DEFAULT 0;"
        )
      }
    }

    @VisibleForTesting
    val MIGRATION_19_20 = object : FlowCryptMigration(19, 20) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        //to prevent problems in users which have an app since database version = 9 or lower
        //(default value for 'is_new' should = -1) we have to recreate 'messages' table
        val tempTableMsgs = "messages_temp"
        database.execSQL(
          "CREATE TEMP TABLE IF NOT EXISTS $tempTableMsgs " +
              "AS SELECT * FROM messages;"
        )
        database.execSQL("DROP TABLE IF EXISTS messages;")
        database.execSQL(
          "CREATE TABLE messages (" +
              "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "email VARCHAR(100) NOT NULL, " +
              "folder TEXT NOT NULL, " +
              "uid INTEGER NOT NULL, " +
              "received_date INTEGER DEFAULT NULL, " +
              "sent_date INTEGER DEFAULT NULL, " +
              "from_address TEXT DEFAULT NULL, " +
              "to_address TEXT DEFAULT NULL, " +
              "cc_address TEXT DEFAULT NULL, " +
              "subject TEXT DEFAULT NULL, " +
              "flags TEXT DEFAULT NULL, " +
              "raw_message_without_attachments TEXT DEFAULT NULL, " +
              "is_message_has_attachments INTEGER DEFAULT 0, " +
              "is_encrypted INTEGER DEFAULT -1, " +
              "is_new INTEGER DEFAULT -1 , " +
              "state INTEGER DEFAULT -1, " +
              "attachments_directory TEXT, " +
              "error_msg TEXT DEFAULT NULL, " +
              "reply_to TEXT DEFAULT NULL)"
        )
        database.execSQL(
          "CREATE INDEX email_in_messages " +
              "ON messages (email)"
        )
        database.execSQL(
          "CREATE UNIQUE INDEX email_uid_folder_in_messages " +
              "ON messages (email, uid, folder)"
        )
        database.execSQL(
          "INSERT INTO messages " +
              "SELECT * FROM $tempTableMsgs;"
        )
        database.execSQL("DROP TABLE IF EXISTS $tempTableMsgs;")

        //recreate 'contacts' table because of wrong column type BOOLEAN
        database.execSQL(
          "CREATE TEMP TABLE IF NOT EXISTS contacts_temp " +
              "AS SELECT * FROM contacts;"
        )
        database.execSQL("DROP TABLE IF EXISTS contacts;")
        database.execSQL(
          "CREATE TABLE IF NOT EXISTS contacts (" +
              "_id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, " +
              "email TEXT NOT NULL, " +
              "name TEXT DEFAULT NULL, " +
              "public_key BLOB DEFAULT NULL, " +
              "has_pgp INTEGER NOT NULL, " +
              "client TEXT DEFAULT NULL, " +
              "attested INTEGER DEFAULT NULL, " +
              "fingerprint TEXT DEFAULT NULL, " +
              "long_id TEXT DEFAULT NULL, " +
              "keywords TEXT DEFAULT NULL, " +
              "last_use INTEGER DEFAULT 0 NOT NULL);"
        )
        database.execSQL(
          "CREATE UNIQUE INDEX IF NOT EXISTS email_in_contacts " +
              "ON contacts (email);"
        )
        database.execSQL(
          "CREATE INDEX IF NOT EXISTS name_in_contacts " +
              "ON contacts (name);"
        )
        database.execSQL(
          "CREATE INDEX IF NOT EXISTS has_pgp_in_contacts " +
              "ON contacts (has_pgp);"
        )
        database.execSQL(
          "CREATE INDEX IF NOT EXISTS long_id_in_contacts " +
              "ON contacts (long_id);"
        )
        database.execSQL(
          "CREATE INDEX IF NOT EXISTS last_use_in_contacts " +
              "ON contacts (last_use);"
        )
        database.execSQL(
          "INSERT INTO contacts " +
              "SELECT * FROM contacts_temp;"
        )
        database.execSQL("DROP TABLE IF EXISTS contacts_temp;")

        //Recreate 'attachment' table to use an ability of foreign keys
        //delete non-OUTBOX attachments
        database.delete(
          "attachment",
          "folder NOT IN (?)",
          arrayOf(JavaEmailConstants.FOLDER_OUTBOX)
        )
        val tempTableAtts = "attachment_temp"

        database.execSQL(
          "CREATE TEMP TABLE IF NOT EXISTS $tempTableAtts " +
              "AS SELECT * FROM attachment;"
        )
        database.execSQL("DROP TABLE IF EXISTS attachment;")
        database.execSQL(
          "CREATE TABLE `attachment` (" +
              "`_id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "`email` TEXT NOT NULL, " +
              "`folder` TEXT NOT NULL, " +
              "`uid` INTEGER NOT NULL, " +
              "`name` TEXT NOT NULL, " +
              "`encodedSize` INTEGER DEFAULT 0, " +
              "`type` TEXT NOT NULL, " +
              "`attachment_id` TEXT, " +
              "`file_uri` TEXT, " +
              "`forwarded_folder` TEXT, " +
              "`forwarded_uid` INTEGER DEFAULT -1, " +
              "`path` TEXT NOT NULL, " +
              "FOREIGN KEY(`email`, `folder`, `uid`) " +
              "REFERENCES `messages`(`email`, `folder`, `uid`) " +
              "ON UPDATE NO ACTION ON DELETE CASCADE );"
        )
        database.execSQL(
          "CREATE UNIQUE INDEX `email_uid_folder_path_in_attachment` " +
              "ON `attachment` (`email`, `uid`, `folder`, `path`);"
        )
        database.execSQL(
          "CREATE INDEX `email_folder_uid_in_attachment` " +
              "ON `attachment` (`email`, `folder`, `uid`);"
        )
        database.execSQL(
          "INSERT INTO attachment " +
              "SELECT * FROM $tempTableAtts;"
        )
        database.execSQL("DROP TABLE IF EXISTS $tempTableAtts;")
      }
    }

    @VisibleForTesting
    val MIGRATION_20_21 = object : FlowCryptMigration(20, 21) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        //Recreate 'accounts_aliases' table to use an ability of foreign keys
        database.execSQL("DROP TABLE IF EXISTS `accounts_aliases`;")
        database.execSQL(
          "CREATE TABLE `accounts_aliases` (" +
              "`_id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "`email` TEXT NOT NULL, " +
              "`account_type` TEXT NOT NULL, " +
              "`send_as_email` TEXT NOT NULL, " +
              "`display_name` TEXT DEFAULT NULL, " +
              "`is_default` INTEGER DEFAULT 0, " +
              "`verification_status` TEXT NOT NULL, " +
              "FOREIGN KEY(`email`, `account_type`) " +
              "REFERENCES `accounts`(`email`, `account_type`) " +
              "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        database.execSQL(
          "CREATE UNIQUE INDEX IF NOT EXISTS " +
              "`email_account_type_send_as_email_in_accounts_aliases` " +
              "ON `accounts_aliases` (`email`, `account_type`, `send_as_email`)"
        )
      }
    }

    @VisibleForTesting
    val MIGRATION_21_22 = object : FlowCryptMigration(21, 22) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        //recreate 'contacts' table because of wrong '_id' column
        database.execSQL(
          "CREATE TEMP TABLE IF NOT EXISTS contacts_temp " +
              "AS SELECT * FROM contacts;"
        )
        database.execSQL("DROP TABLE IF EXISTS contacts;")
        database.execSQL(
          "CREATE TABLE `contacts` (" +
              "`_id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "`email` TEXT NOT NULL, " +
              "`name` TEXT DEFAULT NULL, " +
              "`public_key` BLOB DEFAULT NULL, " +
              "`has_pgp` INTEGER NOT NULL, " +
              "`client` TEXT DEFAULT NULL, " +
              "`attested` INTEGER DEFAULT NULL, " +
              "`fingerprint` TEXT DEFAULT NULL, " +
              "`long_id` TEXT DEFAULT NULL, " +
              "`keywords` TEXT DEFAULT NULL, " +
              "`last_use` INTEGER NOT NULL DEFAULT 0)"
        )
        database.execSQL(
          "CREATE UNIQUE INDEX IF NOT EXISTS `email_in_contacts` " +
              "ON `contacts` (`email`)"
        )
        database.execSQL(
          "CREATE INDEX IF NOT EXISTS name_in_contacts " +
              "ON contacts (name);"
        )
        database.execSQL(
          "CREATE INDEX IF NOT EXISTS has_pgp_in_contacts " +
              "ON contacts (has_pgp);"
        )
        database.execSQL(
          "CREATE INDEX IF NOT EXISTS long_id_in_contacts " +
              "ON contacts (long_id);"
        )
        database.execSQL(
          "CREATE INDEX IF NOT EXISTS last_use_in_contacts " +
              "ON contacts (last_use);"
        )
        database.execSQL(
          "INSERT INTO contacts " +
              "SELECT * FROM contacts_temp;"
        )
        database.execSQL("DROP TABLE IF EXISTS contacts_temp;")
      }
    }

    /**
     * This migration resolve https://github.com/FlowCrypt/flowcrypt-android/issues/923
     */
    @VisibleForTesting
    val MIGRATION_22_23 = object : FlowCryptMigration(22, 23) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        //create temp table with existing content
        database.execSQL(
          "CREATE TEMP TABLE IF NOT EXISTS keys_temp " +
              "AS SELECT * FROM keys;"
        )
        //drop old table
        database.execSQL("DROP TABLE IF EXISTS keys;")
        //create a new table 'keys' with additional fields: 'account', 'account_type'
        database.execSQL(
          "CREATE TABLE IF NOT EXISTS `keys` (" +
              "`_id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "`long_id` TEXT NOT NULL, " +
              "`account` TEXT NOT NULL, " +
              "`account_type` TEXT DEFAULT NULL, " +
              "`source` TEXT NOT NULL, " +
              "`public_key` BLOB NOT NULL, " +
              "`private_key` BLOB NOT NULL, " +
              "`passphrase` TEXT DEFAULT NULL, " +
              "FOREIGN KEY(`account`, `account_type`) " +
              "REFERENCES `accounts`(`email`, `account_type`) " +
              "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        //create indices for new table
        database.execSQL(
          "CREATE UNIQUE INDEX IF NOT EXISTS " +
              "`long_id_account_account_type_in_keys` " +
              "ON `keys` (`long_id`, `account`, `account_type`)"
        )
        //fill new keys table with combination of existing keys
        //and existing accounts using JOIN instruction
        database.execSQL(
          "INSERT INTO keys(" +
              "long_id, " +
              "account, " +
              "account_type, " +
              "source, " +
              "public_key, " +
              "private_key, " +
              "passphrase) " +
              "SELECT " +
              "K.long_id, " +
              "A.email, " +
              "A.account_type, " +
              "K.source, " +
              "K.public_key, " +
              "K.private_key, " +
              "K.passphrase  " +
              "FROM keys_temp as K JOIN accounts as A;"
        )
        //drop temp table
        database.execSQL("DROP TABLE IF EXISTS keys_temp;")

        //drop 'user_id_emails_and_keys' table as unused
        database.execSQL("DROP TABLE IF EXISTS user_id_emails_and_keys;")

        //remove unused actions
        database.execSQL(
          "DELETE FROM action_queue " +
              "WHERE action_type = 'fill_user_id_emails_keys_table'"
        )
      }
    }

    /**
     * Here we do preparation for https://github.com/FlowCrypt/flowcrypt-android/issues/932
     */
    @VisibleForTesting
    val MIGRATION_23_24 = object : FlowCryptMigration(23, 24) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL("DROP TABLE IF EXISTS imap_labels;")
        database.execSQL(
          "CREATE TABLE IF NOT EXISTS `labels` (" +
              "`_id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "`email` TEXT NOT NULL, " +
              "`account_type` TEXT DEFAULT NULL, " +
              "`name` TEXT NOT NULL, " +
              "`alias` TEXT DEFAULT NULL, " +
              "`is_custom` INTEGER NOT NULL DEFAULT 0, " +
              "`messages_total` INTEGER NOT NULL DEFAULT 0, " +
              "`message_unread` INTEGER NOT NULL DEFAULT 0, " +
              "`attributes` TEXT DEFAULT NULL, " +
              "`next_page_token` TEXT DEFAULT NULL, " +
              "`history_id` TEXT DEFAULT NULL, " +
              "FOREIGN KEY(`email`, `account_type`) " +
              "REFERENCES `accounts`(`email`, `account_type`) " +
              "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        database.execSQL(
          "CREATE UNIQUE INDEX IF NOT EXISTS " +
              "`email_account_type_name_in_labels` ON `labels` (`email`, `account_type`, `name`)"
        )
        database.execSQL(
          "ALTER TABLE messages " +
              "ADD COLUMN thread_id TEXT DEFAULT NULL;"
        )
        database.execSQL(
          "ALTER TABLE messages " +
              "ADD COLUMN history_id TEXT DEFAULT NULL;"
        )
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN use_api INTEGER NOT NULL DEFAULT 0;"
        )
      }
    }

    @VisibleForTesting
    val MIGRATION_24_25 = object : FlowCryptMigration(24, 25) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        //create temp table with existing content
        database.execSQL(
          "CREATE TEMP TABLE IF NOT EXISTS keys_temp " +
              "AS SELECT * FROM keys;"
        )
        //drop old table
        database.execSQL("DROP TABLE IF EXISTS keys;")
        //create a new table 'keys' with 'fingerprint' instead of 'long_id'
        database.execSQL(
          "CREATE TABLE IF NOT EXISTS `keys` (" +
              "`_id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "`fingerprint` TEXT NOT NULL, " +
              "`account` TEXT NOT NULL, " +
              "`account_type` TEXT DEFAULT NULL, " +
              "`source` TEXT NOT NULL, " +
              "`public_key` BLOB NOT NULL, " +
              "`private_key` BLOB NOT NULL, " +
              "`passphrase` TEXT DEFAULT NULL, " +
              "`passphrase_type` INTEGER NOT NULL DEFAULT 0, " +
              "FOREIGN KEY(`account`, `account_type`) " +
              "REFERENCES `accounts`(`email`, `account_type`) " +
              "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        //create indices for new table
        database.execSQL(
          "CREATE UNIQUE INDEX IF NOT EXISTS " +
              "`fingerprint_account_account_type_in_keys` " +
              "ON `keys` (`fingerprint`, `account`, `account_type`)"
        )
        //fill new keys table with existing data. Later we will update fingerprints
        database.execSQL(
          "INSERT INTO keys(" +
              "_id, " +
              "fingerprint, " +
              "account, " +
              "account_type, " +
              "source, " +
              "public_key, " +
              "private_key, " +
              "passphrase) " +
              "SELECT * FROM keys_temp;"
        )
        //drop temp table
        database.execSQL("DROP TABLE IF EXISTS keys_temp;")

        val cursor = database.query("SELECT * FROM keys;")
        if (cursor.count > 0) {
          while (cursor.moveToNext()) {
            val longId = cursor.getString(cursor.getColumnIndexOrThrow("fingerprint"))
            val pubKeyAsByteArray = cursor.getBlob(cursor.getColumnIndexOrThrow("public_key"))
            val pubKey = PgpKey.parseKeys(source = pubKeyAsByteArray)
              .pgpKeyRingCollection.pgpPublicKeyRingCollection.first()
            val fingerprint = OpenPgpV4Fingerprint(pubKey).toString()
            database.execSQL(
              "UPDATE keys SET fingerprint = ?, passphrase_type = 0 WHERE fingerprint = ?;",
              arrayOf(fingerprint, longId)
            )
          }
        }
      }
    }

    @VisibleForTesting
    val MIGRATION_25_26 = object : FlowCryptMigration(25, 26) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        //create temp table with existing content
        database.execSQL(
          "CREATE TEMP TABLE IF NOT EXISTS " +
              "accounts_temp AS SELECT * FROM accounts;"
        )
        //drop old table
        database.execSQL("DROP TABLE IF EXISTS accounts;")
        //create a new table 'accounts' with modified structure
        database.execSQL(
          "CREATE TABLE IF NOT EXISTS `accounts` (" +
              "`_id` INTEGER PRIMARY KEY AUTOINCREMENT," +
              " `email` TEXT NOT NULL," +
              " `account_type` TEXT DEFAULT NULL," +
              " `display_name` TEXT DEFAULT NULL," +
              " `given_name` TEXT DEFAULT NULL," +
              " `family_name` TEXT DEFAULT NULL," +
              " `photo_url` TEXT DEFAULT NULL," +
              " `is_enabled` INTEGER DEFAULT 1," +
              " `is_active` INTEGER DEFAULT 0," +
              " `username` TEXT NOT NULL," +
              " `password` TEXT NOT NULL," +
              " `imap_server` TEXT NOT NULL," +
              " `imap_port` INTEGER DEFAULT 143," +
              " `imap_use_ssl_tls` INTEGER DEFAULT 0," +
              " `imap_use_starttls` INTEGER DEFAULT 0," +
              " `imap_auth_mechanisms` TEXT," +
              " `smtp_server` TEXT NOT NULL," +
              " `smtp_port` INTEGER DEFAULT 25," +
              " `smtp_use_ssl_tls` INTEGER DEFAULT 0," +
              " `smtp_use_starttls` INTEGER DEFAULT 0," +
              " `smtp_auth_mechanisms` TEXT," +
              " `smtp_use_custom_sign` INTEGER DEFAULT 0," +
              " `smtp_username` TEXT DEFAULT NULL," +
              " `smtp_password` TEXT DEFAULT NULL," +
              " `contacts_loaded` INTEGER DEFAULT 0," +
              " `show_only_encrypted` INTEGER DEFAULT 0," +
              " `uuid` TEXT DEFAULT NULL," +
              " `client_configuration` TEXT DEFAULT NULL," +
              " `use_api` INTEGER NOT NULL DEFAULT 0);"
        )
        //create indices for new table
        database.execSQL(
          "CREATE UNIQUE INDEX IF NOT EXISTS `email_account_type_in_accounts`" +
              " ON `accounts` (`email`, `account_type`);"
        )
        //fill new accounts table with existing data.
        database.execSQL(
          "INSERT INTO accounts(" +
              "_id," +
              " email," +
              " account_type," +
              " display_name," +
              " given_name," +
              " family_name," +
              " photo_url," +
              " is_enabled," +
              " is_active," +
              " username," +
              " password," +
              " imap_server," +
              " imap_port," +
              " imap_use_ssl_tls," +
              " imap_use_starttls," +
              " imap_auth_mechanisms," +
              " smtp_server," +
              " smtp_port," +
              " smtp_use_ssl_tls," +
              " smtp_use_starttls," +
              " smtp_auth_mechanisms," +
              " smtp_use_custom_sign," +
              " smtp_username," +
              " smtp_password," +
              " contacts_loaded," +
              " show_only_encrypted," +
              " uuid," +
              " use_api" +
              ") SELECT" +
              " _id," +
              " email," +
              " account_type," +
              " display_name," +
              " given_name," +
              " family_name," +
              " photo_url," +
              " is_enable," +
              " is_active," +
              " username," +
              " password," +
              " imap_server," +
              " imap_port," +
              " imap_is_use_ssl_tls," +
              " imap_is_use_starttls," +
              " imap_auth_mechanisms," +
              " smtp_server," +
              " smtp_port," +
              " smtp_is_use_ssl_tls," +
              " smtp_is_use_starttls," +
              " smtp_auth_mechanisms," +
              " smtp_is_use_custom_sign," +
              " smtp_username," +
              " smtp_password," +
              " ic_contacts_loaded," +
              " is_show_only_encrypted," +
              " uuid," +
              " use_api" +
              " FROM accounts_temp;"
        )
        //drop temp table
        database.execSQL("DROP TABLE IF EXISTS accounts_temp;")
      }
    }

    /**
     * Here we do preparation for https://github.com/FlowCrypt/flowcrypt-android/issues/1188
     */
    @VisibleForTesting
    val MIGRATION_26_27 = object : FlowCryptMigration(26, 27) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL(
          "CREATE TEMP TABLE IF NOT EXISTS " +
              "contacts_temp AS SELECT * FROM contacts;"
        )

        //create `recipients` table
        database.execSQL(
          "CREATE TABLE IF NOT EXISTS `recipients` (" +
              "`_id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "`email` TEXT NOT NULL, " +
              "`name` TEXT DEFAULT NULL, " +
              "`last_use` INTEGER NOT NULL DEFAULT 0)"
        )
        database.execSQL(
          "CREATE INDEX IF NOT EXISTS `name_in_recipients` " +
              "ON `recipients` (`name`)"
        )
        database.execSQL(
          "CREATE INDEX IF NOT EXISTS `last_use_in_recipients` " +
              "ON `recipients` (`last_use`)"
        )
        database.execSQL(
          "CREATE UNIQUE INDEX IF NOT EXISTS `email_in_recipients` " +
              "ON `recipients` (`email`)"
        )
        database.execSQL(
          "INSERT INTO recipients(email, name, last_use) " +
              "SELECT email, name, last_use FROM contacts_temp"
        )

        //create `public_keys` table
        database.execSQL(
          "CREATE TABLE IF NOT EXISTS `public_keys` (" +
              "`_id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "`recipient` TEXT NOT NULL, " +
              "`fingerprint` TEXT NOT NULL, " +
              "`public_key` BLOB NOT NULL, " +
              "FOREIGN KEY(`recipient`) " +
              "REFERENCES `recipients`(`email`) " +
              "ON UPDATE NO ACTION ON DELETE CASCADE )"
        )
        database.execSQL(
          "CREATE UNIQUE INDEX IF NOT EXISTS " +
              "`recipient_fingerprint_in_public_keys` ON `public_keys` (`recipient`, `fingerprint`)"
        )
        database.execSQL(
          "CREATE INDEX IF NOT EXISTS `recipient_in_public_keys` " +
              "ON `public_keys` (`recipient`)"
        )
        database.execSQL(
          "CREATE INDEX IF NOT EXISTS `fingerprint_in_public_keys` " +
              "ON `public_keys` (`fingerprint`)"
        )
        database.execSQL(
          "INSERT INTO public_keys(recipient, fingerprint, public_key) " +
              "SELECT email, fingerprint, public_key " +
              "FROM contacts_temp " +
              "WHERE contacts_temp.public_key NOT NULL AND contacts_temp.fingerprint NOT NULL"
        )

        //delete unused tables
        database.execSQL("DROP TABLE IF EXISTS contacts_temp;")
        database.execSQL("DROP TABLE IF EXISTS contacts;")
      }
    }

    @VisibleForTesting
    val MIGRATION_27_28 = object : FlowCryptMigration(27, 28) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL(
          "ALTER TABLE attachment " +
              "ADD COLUMN decrypt_when_forward INTEGER NOT NULL DEFAULT 0;"
        )
      }
    }

    @VisibleForTesting
    val MIGRATION_28_29 = object : FlowCryptMigration(28, 29) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL(
          "ALTER TABLE messages " +
              "ADD COLUMN password BLOB DEFAULT NULL;"
        )
      }
    }

    @VisibleForTesting
    val MIGRATION_29_30 = object : FlowCryptMigration(29, 30) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN use_fes INTEGER NOT NULL DEFAULT 0;"
        )
      }
    }

    @VisibleForTesting
    val MIGRATION_30_31 = object : FlowCryptMigration(30, 31) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        //we temporary disable show_only_encrypted for all users
        database.execSQL("UPDATE accounts SET show_only_encrypted = 0;")
      }
    }

    @VisibleForTesting
    val MIGRATION_31_32 = object : FlowCryptMigration(31, 32) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL(
          "CREATE INDEX IF NOT EXISTS " +
              "`account_account_type_in_keys` " +
              "ON `keys` (`account`, `account_type`)"
        )
      }
    }

    @VisibleForTesting
    val MIGRATION_32_33 = object : FlowCryptMigration(32, 33) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        database.execSQL(
          "ALTER TABLE messages " +
              "ADD COLUMN draft_id TEXT DEFAULT NULL;"
        )
      }
    }

    @VisibleForTesting
    val MIGRATION_33_34 = object : FlowCryptMigration(33, 34) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        //create temp table with existing content
        database.execSQL(
          "CREATE TEMP TABLE IF NOT EXISTS " +
              "accounts_temp AS SELECT * FROM accounts;"
        )
        //drop old table
        database.execSQL("DROP TABLE IF EXISTS accounts;")
        //create a new table 'accounts' with modified structure
        database.execSQL(
          "CREATE TABLE IF NOT EXISTS `accounts` (" +
              "`_id` INTEGER PRIMARY KEY AUTOINCREMENT," +
              " `email` TEXT NOT NULL," +
              " `account_type` TEXT DEFAULT NULL," +
              " `display_name` TEXT DEFAULT NULL," +
              " `given_name` TEXT DEFAULT NULL," +
              " `family_name` TEXT DEFAULT NULL," +
              " `photo_url` TEXT DEFAULT NULL," +
              " `is_enabled` INTEGER DEFAULT 1," +
              " `is_active` INTEGER DEFAULT 0," +
              " `username` TEXT NOT NULL," +
              " `password` TEXT NOT NULL," +
              " `imap_server` TEXT NOT NULL," +
              " `imap_port` INTEGER DEFAULT 143," +
              " `imap_use_ssl_tls` INTEGER DEFAULT 0," +
              " `imap_use_starttls` INTEGER DEFAULT 0," +
              " `imap_auth_mechanisms` TEXT," +
              " `smtp_server` TEXT NOT NULL," +
              " `smtp_port` INTEGER DEFAULT 25," +
              " `smtp_use_ssl_tls` INTEGER DEFAULT 0," +
              " `smtp_use_starttls` INTEGER DEFAULT 0," +
              " `smtp_auth_mechanisms` TEXT," +
              " `smtp_use_custom_sign` INTEGER DEFAULT 0," +
              " `smtp_username` TEXT DEFAULT NULL," +
              " `smtp_password` TEXT DEFAULT NULL," +
              " `contacts_loaded` INTEGER DEFAULT 0," +
              " `show_only_encrypted` INTEGER DEFAULT 0," +
              " `client_configuration` TEXT DEFAULT NULL," +
              " `use_api` INTEGER NOT NULL DEFAULT 0," +
              " `use_fes` INTEGER NOT NULL DEFAULT 0)"
        )
        //create indices for new table
        database.execSQL(
          "CREATE UNIQUE INDEX IF NOT EXISTS `email_account_type_in_accounts`" +
              " ON `accounts` (`email`, `account_type`);"
        )
        //fill new accounts table with existing data.
        database.execSQL(
          "INSERT INTO accounts SELECT" +
              " _id," +
              " email," +
              " account_type," +
              " display_name," +
              " given_name," +
              " family_name," +
              " photo_url," +
              " is_enabled," +
              " is_active," +
              " username," +
              " password," +
              " imap_server," +
              " imap_port," +
              " imap_use_ssl_tls," +
              " imap_use_starttls," +
              " imap_auth_mechanisms," +
              " smtp_server," +
              " smtp_port," +
              " smtp_use_ssl_tls," +
              " smtp_use_starttls," +
              " smtp_auth_mechanisms," +
              " smtp_use_custom_sign," +
              " smtp_username," +
              " smtp_password," +
              " contacts_loaded," +
              " show_only_encrypted," +
              " client_configuration," +
              " use_api," +
              " use_fes" +
              " FROM accounts_temp;"
        )
        //drop temp table
        database.execSQL("DROP TABLE IF EXISTS accounts_temp;")
      }
    }

    @VisibleForTesting
    val MIGRATION_34_35 = object : FlowCryptMigration(34, 35) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        //create temp table with existing content
        database.execSQL("CREATE TEMP TABLE IF NOT EXISTS accounts_temp AS SELECT * FROM accounts;")
        //drop old table
        database.execSQL("DROP TABLE IF EXISTS accounts;")
        //create a new table 'accounts' with the renamed field 'useFES' to 'useCustomerFesUrl'
        database.execSQL(
          "CREATE TABLE IF NOT EXISTS `accounts` (" +
              "`_id` INTEGER PRIMARY KEY AUTOINCREMENT," +
              " `email` TEXT NOT NULL," +
              " `account_type` TEXT DEFAULT NULL," +
              " `display_name` TEXT DEFAULT NULL," +
              " `given_name` TEXT DEFAULT NULL," +
              " `family_name` TEXT DEFAULT NULL," +
              " `photo_url` TEXT DEFAULT NULL," +
              " `is_enabled` INTEGER DEFAULT 1," +
              " `is_active` INTEGER DEFAULT 0," +
              " `username` TEXT NOT NULL," +
              " `password` TEXT NOT NULL," +
              " `imap_server` TEXT NOT NULL," +
              " `imap_port` INTEGER DEFAULT 143," +
              " `imap_use_ssl_tls` INTEGER DEFAULT 0," +
              " `imap_use_starttls` INTEGER DEFAULT 0," +
              " `imap_auth_mechanisms` TEXT," +
              " `smtp_server` TEXT NOT NULL," +
              " `smtp_port` INTEGER DEFAULT 25," +
              " `smtp_use_ssl_tls` INTEGER DEFAULT 0," +
              " `smtp_use_starttls` INTEGER DEFAULT 0," +
              " `smtp_auth_mechanisms` TEXT," +
              " `smtp_use_custom_sign` INTEGER DEFAULT 0," +
              " `smtp_username` TEXT DEFAULT NULL," +
              " `smtp_password` TEXT DEFAULT NULL," +
              " `contacts_loaded` INTEGER DEFAULT 0," +
              " `show_only_encrypted` INTEGER DEFAULT 0," +
              " `client_configuration` TEXT DEFAULT NULL," +
              " `use_api` INTEGER NOT NULL DEFAULT 0," +
              " `use_customer_fes_url` INTEGER NOT NULL DEFAULT 0)"

        )
        //create indices for new table
        database.execSQL(
          "CREATE UNIQUE INDEX IF NOT EXISTS `email_account_type_in_accounts`" +
              " ON `accounts` (`email`, `account_type`);"
        )
        //fill new accounts table with existing data.
        database.execSQL("INSERT INTO accounts SELECT * FROM accounts_temp;")
        //drop temp table
        database.execSQL("DROP TABLE IF EXISTS accounts_temp;")
      }
    }

    @VisibleForTesting
    val MIGRATION_35_36 = object : FlowCryptMigration(35, 36) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        //we need to clean old cache for some 'com.google' users: messages, attachments, labels

        val commonWhereClause = "email IN (SELECT email FROM " + AccountEntity.TABLE_NAME +
            " WHERE account_type = ? AND use_api = ?)"
        val commonWhereArgs = arrayOf(
          AccountEntity.ACCOUNT_TYPE_GOOGLE,
          "0",
          JavaEmailConstants.FOLDER_OUTBOX
        )
        //delete messages for predefined 'com.google' users, except outgoing
        database.delete(
          MessageEntity.TABLE_NAME,
          "$commonWhereClause AND folder != ?",
          commonWhereArgs
        )

        //delete attachments for predefined 'com.google' users, except outgoing
        database.delete(
          AttachmentEntity.TABLE_NAME,
          "$commonWhereClause AND folder != ?",
          commonWhereArgs
        )

        //delete all labels for 'com.google' users
        database.delete(
          LabelEntity.TABLE_NAME,
          commonWhereClause,
          commonWhereArgs.copyOf(2)
        )

        //in the and we need to force using Gmail API by 'com.google' users via 'use_api = 1'
        val contentValues = ContentValues()
        contentValues.put("use_api", "1")
        database.update(
          AccountEntity.TABLE_NAME,
          SQLiteDatabase.CONFLICT_IGNORE,
          contentValues,
          "account_type = ?",
          arrayOf(AccountEntity.ACCOUNT_TYPE_GOOGLE)
        )
      }
    }

    @VisibleForTesting
    val MIGRATION_36_37 = object : FlowCryptMigration(36, 37) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        //ref https://github.com/FlowCrypt/flowcrypt-android/issues/1766

        //create temp table with existing content
        database.execSQL("CREATE TEMP TABLE IF NOT EXISTS keys_temp AS SELECT * FROM keys;")
        //drop old table
        database.execSQL("DROP TABLE IF EXISTS keys;")
        //create a new table 'keys' with dropped field 'public_key'
        database.execSQL(
          "CREATE TABLE IF NOT EXISTS `keys` (" +
              "`_id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
              "`fingerprint` TEXT NOT NULL, " +
              "`account` TEXT NOT NULL, " +
              "`account_type` TEXT DEFAULT NULL, " +
              "`source` TEXT NOT NULL, " +
              "`private_key` BLOB NOT NULL, " +
              "`passphrase` TEXT DEFAULT NULL, " +
              "`passphrase_type` INTEGER NOT NULL DEFAULT 0, " +
              "FOREIGN KEY(`account`, `account_type`) " +
              "REFERENCES `accounts`(`email`, `account_type`) " +
              "ON UPDATE NO ACTION ON DELETE CASCADE )"

        )
        //create indices for new table
        database.execSQL(
          "CREATE INDEX IF NOT EXISTS `account_account_type_in_keys` " +
              "ON `keys` (`account`, `account_type`)"
        )
        database.execSQL(
          "CREATE UNIQUE INDEX IF NOT EXISTS `fingerprint_account_account_type_in_keys` " +
              "ON `keys` (`fingerprint`, `account`, `account_type`)"
        )
        //fill new table with existing data.
        database.execSQL(
          "INSERT INTO `keys` SELECT " +
              "_id, " +
              "fingerprint, " +
              "account, " +
              "account_type, " +
              "source, " +
              "private_key, " +
              "passphrase, " +
              "passphrase_type " +
              "FROM keys_temp;"
        )
        //drop temp table
        database.execSQL("DROP TABLE IF EXISTS keys_temp;")
      }
    }

    @VisibleForTesting
    val MIGRATION_37_38 = object : FlowCryptMigration(37, 38) {
      override fun doMigration(database: SupportSQLiteDatabase) {
        //ref https://github.com/FlowCrypt/flowcrypt-android/issues/2356

        database.execSQL(
          "ALTER TABLE accounts " +
              "ADD COLUMN check_pass_phrase_last_time INTEGER NOT NULL DEFAULT 0;"
        )
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
          DB_NAME
        ).addMigrations(
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
          MIGRATION_12_13,
          MIGRATION_13_14,
          MIGRATION_14_15,
          MIGRATION_15_16,
          MIGRATION_16_17,
          MIGRATION_17_18,
          MIGRATION_18_19,
          MIGRATION_19_20,
          MIGRATION_20_21,
          MIGRATION_21_22,
          MIGRATION_22_23,
          MIGRATION_23_24,
          MIGRATION_24_25,
          MIGRATION_25_26,
          MIGRATION_26_27,
          MIGRATION_27_28,
          MIGRATION_28_29,
          MIGRATION_29_30,
          MIGRATION_30_31,
          MIGRATION_31_32,
          MIGRATION_32_33,
          MIGRATION_33_34,
          MIGRATION_34_35,
          MIGRATION_35_36,
          MIGRATION_36_37,
          MIGRATION_37_38,
        ).build()
        INSTANCE = instance
        return instance
      }
    }
  }
}
