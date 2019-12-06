/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flowcrypt.email.database.FlowCryptRoomDatabase.Companion.DB_VERSION
import com.flowcrypt.email.database.entity.AccountAliasesEntity
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.ActionQueueEntity
import com.flowcrypt.email.database.entity.AttachmentEntity
import com.flowcrypt.email.database.entity.ContactEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.database.entity.LabelsEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.database.entity.UserIdEmailsKeysEntity

/**
 * This class describes an implementation of [RoomDatabase]
 *
 * @author Denis Bondarenko
 *         Date: 12/4/19
 *         Time: 2:51 PM
 *         E-mail: DenBond7@gmail.com
 */
@Database(entities = [
  AccountAliasesEntity::class,
  AccountEntity::class,
  ActionQueueEntity::class,
  AttachmentEntity::class,
  ContactEntity::class,
  KeyEntity::class,
  LabelsEntity::class,
  MessageEntity::class,
  UserIdEmailsKeysEntity::class],
    version = DB_VERSION)
abstract class FlowCryptRoomDatabase : RoomDatabase() {
  companion object {
    private const val DB_NAME = "flowcrypt.db"
    const val DB_VERSION = 20

    /**
     * We’ll still need to implement an empty migration to tell Room to keep the existing data.
     */
    private val MIGRATION_19_20 = object : Migration(19, DB_VERSION) {
      override fun migrate(database: SupportSQLiteDatabase) {

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
            .addMigrations(MIGRATION_19_20)
            .build()
        INSTANCE = instance
        return instance
      }
    }
  }
}