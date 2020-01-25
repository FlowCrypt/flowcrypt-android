/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import java.util.*

/**
 * This object describes a logic of work with [AccountAliases].
 *
 * @author Denis Bondarenko
 * Date: 26.10.2017
 * Time: 15:51
 * E-mail: DenBond7@gmail.com
 */

class AccountAliasesDao : BaseDaoSource() {

  override val tableName: String = TABLE_NAME_ACCOUNTS_ALIASES

  /**
   * Save information about an account alias using the [AccountAliases];
   *
   * @param context Interface to global information about an application environment;
   * @param dao     The user's alias information.
   * @return The created [Uri] or null;
   */
  fun addRow(context: Context, dao: AccountAliases?): Uri? {
    val contentResolver = context.contentResolver
    if (dao != null && contentResolver != null) {
      val contentValues = generateContentValues(dao) ?: return null

      return contentResolver.insert(baseContentUri, contentValues)
    } else
      return null
  }

  /**
   * This method add rows per single transaction.
   *
   * @param context Interface to global information about an application environment.
   * @param list    The list of an account aliases.
   */
  fun addRows(context: Context, list: List<AccountAliases>?): Int {
    return if (list != null && list.isNotEmpty()) {
      val contentResolver = context.contentResolver
      val contentValuesArray = arrayOfNulls<ContentValues>(list.size)

      for (i in list.indices) {
        val accountAliasesDao = list[i]
        contentValuesArray[i] = generateContentValues(accountAliasesDao)
      }

      contentResolver.bulkInsert(baseContentUri, contentValuesArray)
    } else
      0
  }

  /**
   * Get the list of [AccountAliases] object from the local database for some email.
   *
   * @param context Interface to global information about an application environment.
   * @param account An account information.
   * @return The list of [AccountAliases];
   */
  fun getAliases(context: Context, account: AccountDao?): List<AccountAliases> {
    val accountAliasesDaoList = ArrayList<AccountAliases>()
    if (account != null) {
      val selection = AccountDaoSource.COL_EMAIL + " = ? AND " + AccountDaoSource.COL_ACCOUNT_TYPE + " = ?"
      val selectionArgs = arrayOf(account.email, account.accountType!!)
      val cursor = context.contentResolver.query(baseContentUri, null, selection, selectionArgs, null)

      if (cursor != null) {
        while (cursor.moveToNext()) {
          accountAliasesDaoList.add(getCurrent(cursor))
        }
      }

      cursor?.close()
    }

    return accountAliasesDaoList
  }

  /**
   * Update information about aliases of some [AccountDao].
   *
   * @param context Interface to global information about an application environment.
   * @param account The object which contains information about an email account.
   * @param list    The list of an account aliases.
   * @return The count of updated rows. Will be 1 if information about [AccountDao] was
   * updated or -1 otherwise.
   */
  fun updateAliases(context: Context, account: AccountDao, list: List<AccountAliases>): Int {
    deleteAccountAliases(context, account)
    return addRows(context, list)
  }

  /**
   * Delete information about aliases of some [AccountDao].
   *
   * @param context Interface to global information about an application environment.
   * @param account The object which contains information about an email account.
   * @return The count of deleted rows. Will be 1 if information about [AccountDao] was
   * deleted or -1 otherwise.
   */
  fun deleteAccountAliases(context: Context, account: AccountDao?): Int {
    if (account != null) {
      var email = account.email
      email = email.toLowerCase(Locale.getDefault())

      var type = account.accountType
      if (type == null) {
        return -1
      } else {
        type = type.toLowerCase(Locale.getDefault())
      }

      val contentResolver = context.contentResolver
      return if (contentResolver != null) {
        val where = "$COL_EMAIL = ? AND $COL_ACCOUNT_TYPE = ?"
        contentResolver.delete(baseContentUri, where, arrayOf(email, type))
      } else
        -1
    } else
      return -1
  }

  /**
   * Generate a [ContentValues] using [AccountAliases].
   *
   * @param accountAliases The [AccountAliases] object;
   * @return The generated [ContentValues].
   */
  private fun generateContentValues(accountAliases: AccountAliases): ContentValues? {
    val contentValues = ContentValues()
    if (accountAliases.email != null) {
      contentValues.put(COL_EMAIL, accountAliases.email!!.toLowerCase(Locale.getDefault()))
    } else {
      return null
    }

    contentValues.put(COL_ACCOUNT_TYPE, accountAliases.accountType)
    contentValues.put(COL_DISPLAY_NAME, accountAliases.displayName)
    contentValues.put(COL_SEND_AS_EMAIL, accountAliases.sendAsEmail!!.toLowerCase(Locale.getDefault()))
    contentValues.put(COL_SEND_AS_EMAIL, accountAliases.sendAsEmail!!.toLowerCase(Locale.getDefault()))
    contentValues.put(COL_IS_DEFAULT, accountAliases.isDefault)
    contentValues.put(COL_VERIFICATION_STATUS, accountAliases.verificationStatus)
    return contentValues
  }

  companion object {
    const val VERIFICATION_STATUS_ACCEPTED = "accepted"

    const val TABLE_NAME_ACCOUNTS_ALIASES = "accounts_aliases"

    const val COL_EMAIL = "email"
    const val COL_ACCOUNT_TYPE = "account_type"
    const val COL_SEND_AS_EMAIL = "send_as_email"
    const val COL_DISPLAY_NAME = "display_name"
    const val COL_IS_DEFAULT = "is_default"
    const val COL_VERIFICATION_STATUS = "verification_status"

    const val ACCOUNTS_ALIASES_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
        TABLE_NAME_ACCOUNTS_ALIASES + " (" +
        BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COL_EMAIL + " VARCHAR(100) NOT NULL, " +
        COL_ACCOUNT_TYPE + " VARCHAR(100) NOT NULL, " +
        COL_SEND_AS_EMAIL + " VARCHAR(100) NOT NULL, " +
        COL_DISPLAY_NAME + " TEXT DEFAULT NULL, " +
        COL_IS_DEFAULT + " INTEGER DEFAULT 0, " +
        COL_VERIFICATION_STATUS + " TEXT NOT NULL " + ");"

    const val CREATE_INDEX_EMAIL_TYPE_IN_ACCOUNTS_ALIASES = (UNIQUE_INDEX_PREFIX
        + COL_EMAIL + "_" + COL_ACCOUNT_TYPE + "_" + COL_SEND_AS_EMAIL + "_in_" + TABLE_NAME_ACCOUNTS_ALIASES
        + " ON " + TABLE_NAME_ACCOUNTS_ALIASES + " (" + COL_EMAIL + ", " + COL_ACCOUNT_TYPE + ", " +
        COL_SEND_AS_EMAIL + ")")

    /**
     * Generate the [AccountAliases] from the current cursor position;
     *
     * @param cursor The cursor from which to get the data.
     * @return [AccountAliases].
     */
    @JvmStatic
    fun getCurrent(cursor: Cursor): AccountAliases {
      val dao = AccountAliases()
      val accountEmail = cursor.getString(cursor.getColumnIndex(COL_EMAIL))
      dao.email = accountEmail?.toLowerCase(Locale.getDefault())
      dao.accountType = cursor.getString(cursor.getColumnIndex(COL_ACCOUNT_TYPE))
      val sendAsEmail = cursor.getString(cursor.getColumnIndex(COL_SEND_AS_EMAIL))
      dao.sendAsEmail = sendAsEmail?.toLowerCase(Locale.getDefault())
      dao.displayName = cursor.getString(cursor.getColumnIndex(COL_DISPLAY_NAME))
      dao.isDefault = cursor.getInt(cursor.getColumnIndex(COL_IS_DEFAULT)) == 1
      dao.verificationStatus = cursor.getString(cursor.getColumnIndex(COL_VERIFICATION_STATUS))
      return dao
    }
  }
}
