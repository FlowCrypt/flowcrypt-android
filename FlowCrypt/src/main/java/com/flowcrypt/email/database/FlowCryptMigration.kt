/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flowcrypt.email.util.LogsUtil

/**
 * Base class for a database migration.
 *
 * @author Denys Bondarenko
 */
abstract class FlowCryptMigration(startVersion: Int, endVersion: Int) :
  Migration(startVersion, endVersion) {

  /**
   * Should run the necessary migrations.
   * <p>
   * This class cannot access any generated Dao in this method.
   * <p>
   * This method is already called inside a transaction and that transaction might actually be a
   * composite transaction of all necessary {@code Migration}s.
   *
   * @param database The database instance
   */
  abstract fun doMigration(database: SupportSQLiteDatabase)

  override fun migrate(database: SupportSQLiteDatabase) {
    LogsUtil.d(
      FlowCryptMigration::class.java.simpleName,
      "Begin migration from $startVersion to $endVersion"
    )
    doMigration(database)
    LogsUtil.d(
      FlowCryptMigration::class.java.simpleName,
      "End migration from $startVersion to $endVersion"
    )
  }
}
