/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException


/**
 * @author Denis Bondarenko
 *         Date: 1/27/20
 *         Time: 9:25 AM
 *         E-mail: DenBond7@gmail.com
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@Ignore("it doesn't allow to run all tests")
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
    FlowCryptRoomDatabase.MIGRATION_29_30
  )

  @get:Rule
  val migrationTestHelper: MigrationTestHelper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    FlowCryptRoomDatabase::class.java.canonicalName,
    FrameworkSQLiteOpenHelperFactory()
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
