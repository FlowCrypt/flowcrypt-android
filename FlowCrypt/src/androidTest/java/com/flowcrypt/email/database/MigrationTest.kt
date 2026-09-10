/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException


/**
 * @author Denys Bondarenko
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
class MigrationTest {
  // Array of all migrations which we are going to test
  private val arrayOfMigrations = arrayOf(
    FlowCryptRoomDatabase.MIGRATION_19_20,
    FlowCryptRoomDatabase.MIGRATION_20_21,
    FlowCryptRoomDatabase.MIGRATION_21_22,
    FlowCryptRoomDatabase.MIGRATION_22_23,
    FlowCryptRoomDatabase.MIGRATION_23_24,
    FlowCryptRoomDatabase.MIGRATION_24_25,
    FlowCryptRoomDatabase.MIGRATION_25_26,
    FlowCryptRoomDatabase.MIGRATION_26_27,
    FlowCryptRoomDatabase.MIGRATION_27_28,
    FlowCryptRoomDatabase.MIGRATION_28_29,
    FlowCryptRoomDatabase.MIGRATION_29_30,
    FlowCryptRoomDatabase.MIGRATION_30_31,
    FlowCryptRoomDatabase.MIGRATION_31_32,
    FlowCryptRoomDatabase.MIGRATION_32_33,
    FlowCryptRoomDatabase.MIGRATION_33_34,
    FlowCryptRoomDatabase.MIGRATION_34_35,
    FlowCryptRoomDatabase.MIGRATION_35_36,
    FlowCryptRoomDatabase.MIGRATION_36_37,
    FlowCryptRoomDatabase.MIGRATION_37_38,
    FlowCryptRoomDatabase.MIGRATION_38_39,
    FlowCryptRoomDatabase.MIGRATION_39_40,
    FlowCryptRoomDatabase.MIGRATION_40_41,
    FlowCryptRoomDatabase.MIGRATION_41_42,
    FlowCryptRoomDatabase.Migration42to43(InstrumentationRegistry.getInstrumentation().targetContext),
    FlowCryptRoomDatabase.MIGRATION_43_44,
    FlowCryptRoomDatabase.MIGRATION_44_45,
    FlowCryptRoomDatabase.MIGRATION_45_46,
  )

  @get:Rule
  val migrationTestHelper: MigrationTestHelper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    FlowCryptRoomDatabase::class.java
  )

  @Test
  @Throws(IOException::class)
  fun testAllMigrations() {
    // Create earliest version of the database.
    migrationTestHelper.createDatabase(FlowCryptRoomDatabase.DB_NAME, INIT_DATABASE_VERSION).apply {
      close()
    }

    // Open latest version of the DB. Room will validate the schema once all migrations execute.
    Room.databaseBuilder(
      InstrumentationRegistry.getInstrumentation().targetContext,
      FlowCryptRoomDatabase::class.java, FlowCryptRoomDatabase.DB_NAME
    ).addMigrations(*arrayOfMigrations).build().apply {
      openHelper.writableDatabase
      close()
    }
  }

  companion object {
    const val INIT_DATABASE_VERSION = 19
  }
}
