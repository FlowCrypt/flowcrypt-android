/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source

import android.accounts.Account
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.text.TextUtils
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailConstants
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import java.security.GeneralSecurityException
import java.util.*

/**
 * This class describe creating of table which has name
 * [AccountDaoSource.TABLE_NAME_ACCOUNTS], add, delete and update rows.
 *
 * @author Denis Bondarenko
 * Date: 14.07.2017
 * Time: 17:43
 * E-mail: DenBond7@gmail.com
 */

class AccountDaoSource : BaseDaoSource() {

  override val tableName: String = TABLE_NAME_ACCOUNTS

  /**
   * Save information about an account using the [GoogleSignInAccount];
   *
   * @param context             Interface to global information about an application environment;
   * @param googleSignInAccount Reflecting the user's sign in information.
   * @return The created [Uri] or null;
   */
  fun addRow(context: Context, googleSignInAccount: GoogleSignInAccount?, uuid: String? = null,
             domainRules: List<String>? = null): Uri? {
    val contentResolver = context.contentResolver
    if (googleSignInAccount != null && contentResolver != null) {
      val contentValues = genContentValues(googleSignInAccount) ?: return null

      uuid?.let {
        contentValues.put(COL_UUID, KeyStoreCryptoManager.encrypt(it))
      }

      domainRules?.let { contentValues.put(COL_DOMAIN_RULES, it.joinToString()) }

      return contentResolver.insert(baseContentUri, contentValues)
    } else
      return null
  }

  /**
   * Save information about an account using the [AccountDao];
   *
   * @param context    Interface to global information about an application environment;
   * @param accountDao An instance of [AccountDao].
   * @return The created [Uri] or null;
   * @throws Exception An exception maybe occurred when encrypt the user password.
   */
  fun addRow(context: Context, accountDao: AccountDao?): Uri? {
    return if (accountDao != null) {
      val contentValues = ContentValues()

      contentValues.put(COL_DISPLAY_NAME, accountDao.displayName)
      contentValues.put(COL_USERNAME, accountDao.email)
      contentValues.put(COL_GIVEN_NAME, accountDao.givenName)
      contentValues.put(COL_FAMILY_NAME, accountDao.familyName)
      contentValues.put(COL_PHOTO_URL, accountDao.photoUrl)
      contentValues.put(COL_IS_CONTACTS_LOADED, accountDao.areContactsLoaded)
      contentValues.put(COL_ACCOUNT_TYPE, accountDao.accountType)

      if (accountDao.authCreds != null) {
        val authCredentialsValues = genContentValues(accountDao.authCreds)
        authCredentialsValues?.let { contentValues.putAll(it) }
      }

      accountDao.uuid?.let {
        contentValues.put(COL_UUID, KeyStoreCryptoManager.encrypt(it))
      }

      accountDao.domainRules?.let { contentValues.put(COL_DOMAIN_RULES, it.joinToString()) }

      context.contentResolver?.insert(baseContentUri, contentValues)
    } else
      null
  }

  /**
   * Save information about an account using the [AuthCredentials];
   *
   * @param context   Interface to global information about an application environment;
   * @param authCreds The sign-in settings of IMAP and SMTP servers.
   * @return The created [Uri] or null;
   * @throws Exception An exception maybe occurred when encrypt the user password.
   */
  fun addRow(context: Context, authCreds: AuthCredentials?): Uri? {
    val contentResolver = context.contentResolver
    if (authCreds != null && contentResolver != null) {
      val contentValues = genContentValues(authCreds) ?: return null

      return contentResolver.insert(baseContentUri, contentValues)
    } else
      return null
  }

  /**
   * Get an active [AccountDao] object from the local database.
   *
   * @param context Interface to global information about an application environment.
   * @return The [AccountDao];
   */
  fun getActiveAccountInformation(context: Context?): AccountDao? {
    val selection = "$COL_IS_ACTIVE = ?"
    val cursor = context?.contentResolver?.query(baseContentUri, null, selection, arrayOf("1"),
        null)

    var account: AccountDao? = null

    if (cursor?.moveToFirst() == true) {
      account = getCurrentAccountDao(cursor)
    }

    cursor?.close()

    return account
  }

  /**
   * Get a [AccountDao] object from the local database.
   *
   * @param context Interface to global information about an application environment.
   * @param email   An email of the some account information.
   * @return The [AccountDao];
   */
  fun getAccountInformation(context: Context, email: String): AccountDao? {
    val emailInLowerCase = if (TextUtils.isEmpty(email)) email else email.toLowerCase(Locale.getDefault())
    val selection = "$COL_EMAIL = ?"
    val selectionArgs = arrayOf(emailInLowerCase)
    val cursor = context.contentResolver.query(baseContentUri, null, selection, selectionArgs, null)

    if (cursor != null && cursor.moveToFirst()) {
      return getCurrentAccountDao(cursor)
    }

    cursor?.close()

    return null
  }

  /**
   * Update information about some [AccountDao].
   *
   * @param context    Interface to global information about an application environment.
   * @param googleSign Reflecting the user's sign in information.
   * @return The count of updated rows. Will be 1 if information about [AccountDao] was
   * updated or -1 otherwise.
   */
  fun updateAccountInformation(context: Context, googleSign: GoogleSignInAccount?): Int {
    return if (googleSign != null) {
      updateAccountInformation(context, AccountDao(googleSign), genContentValues(googleSign))
    } else
      -1
  }

  /**
   * Update information about some [AccountDao].
   *
   * @param context       Interface to global information about an application environment.
   * @param account       An [Account] which will be updated
   * @param contentValues Data fro modification
   * @return The count of updated rows. Will be 1 if information about [AccountDao] was
   * updated or -1 otherwise.
   */
  fun updateAccountInformation(context: Context, account: AccountDao?, contentValues:
  ContentValues?): Int {
    if (account != null) {
      var email: String? = account.email
      if (email == null) {
        return -1
      } else {
        email = email.toLowerCase(Locale.getDefault())
      }

      val contentResolver = context.contentResolver
      return if (contentResolver != null) {
        val selection = "$COL_EMAIL = ?"
        contentResolver.update(baseContentUri, contentValues, selection, arrayOf(email))
      } else
        -1
    } else
      return -1
  }

  /**
   * Delete information about some [AccountDao].
   *
   * @param context Interface to global information about an application environment.
   * @param account The object which contains information about an email account.
   * @return The count of deleted rows. Will be 1 if information about [AccountDao] was
   * deleted or -1 otherwise.
   */
  fun deleteAccountInformation(context: Context, account: AccountDao?): Int {
    if (account != null) {

      var email: String? = account.email
      if (email == null) {
        return -1
      } else {
        email = email.toLowerCase(Locale.getDefault())
      }

      var type = account.accountType
      if (type == null) {
        return -1
      } else {
        type = type.toLowerCase(Locale.getDefault())
      }

      val contentResolver = context.contentResolver
      return if (contentResolver != null) {
        val selection = "$COL_EMAIL = ? AND $COL_ACCOUNT_TYPE = ?"
        contentResolver.delete(baseContentUri, selection, arrayOf(email, type))
      } else
        -1
    } else
      return -1
  }

  /**
   * Get the list of all added account without the active account.
   *
   * @param context Interface to global information about an application environment.
   * @param email   An email of the active account.
   * @return The list of all added account without the active account
   */
  fun getAccountsWithoutActive(context: Context, email: String): List<AccountDao> {
    val emailInLowerCase = if (TextUtils.isEmpty(email)) email else email.toLowerCase(Locale.getDefault())

    val selection = "$COL_EMAIL != ?"
    val selectionArgs = arrayOf(emailInLowerCase)
    val cursor = context.contentResolver.query(baseContentUri, null, selection, selectionArgs, null)

    val accountDaoList = ArrayList<AccountDao>()
    if (cursor != null) {
      while (cursor.moveToNext()) {
        accountDaoList.add(getCurrentAccountDao(cursor))
      }
    }

    cursor?.close()

    return accountDaoList
  }

  /**
   * Checking of showing only encrypted messages for the given account.
   *
   * @param context Interface to global information about an application environment.
   * @param email   An email of the active account.
   * @return true if need to show only encrypted messages
   */
  fun isEncryptedModeEnabled(context: Context?, email: String?): Boolean {
    val emailInLowerCase = if (TextUtils.isEmpty(email)) email else email?.toLowerCase(Locale.getDefault())

    val selection = "$COL_EMAIL = ?"
    val selectionArgs = arrayOf(emailInLowerCase)
    val cursor = context?.contentResolver?.query(baseContentUri, null, selection, selectionArgs, null)

    var isEncryptedModeEnabled = false

    if (cursor != null && cursor.moveToFirst()) {
      isEncryptedModeEnabled = cursor.getInt(cursor.getColumnIndex(COL_IS_SHOW_ONLY_ENCRYPTED)) == 1
    }

    cursor?.close()

    return isEncryptedModeEnabled
  }

  /**
   * Change a value of [.COL_IS_SHOW_ONLY_ENCRYPTED] field for the given account.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The account which will be set as active.
   * @return The count of updated rows.
   */
  fun setShowOnlyEncryptedMsgs(context: Context, email: String?, onlyEncryptedMsgs: Boolean): Int {
    if (email == null) {
      return -1
    }

    val emailInLowerCase = if (TextUtils.isEmpty(email)) email else email.toLowerCase(Locale.getDefault())

    val contentResolver = context.contentResolver
    return if (contentResolver != null) {
      val contentValues = ContentValues()
      contentValues.put(COL_IS_SHOW_ONLY_ENCRYPTED, onlyEncryptedMsgs)
      val where = "$COL_EMAIL = ? "
      contentResolver.update(baseContentUri, contentValues, where, arrayOf(emailInLowerCase))
    } else
      -1
  }

  /**
   * Mark some account as active.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The account which will be set as active.
   * @return The count of updated rows.
   */
  fun setActiveAccount(context: Context, email: String?): Int {
    if (email == null) {
      return -1
    }

    val emailInLowerCase = if (TextUtils.isEmpty(email)) email else email.toLowerCase(Locale.getDefault())
    val contentResolver = context.contentResolver
    return if (contentResolver != null) {
      val contentValuesDeactivateAllAccount = ContentValues()
      contentValuesDeactivateAllAccount.put(COL_IS_ACTIVE, 0)
      var updateRowCount = contentResolver.update(baseContentUri, contentValuesDeactivateAllAccount, null, null)

      val valuesActive = ContentValues()
      valuesActive.put(COL_IS_ACTIVE, 1)
      val selection = "$COL_EMAIL = ? "
      val selectionArgs = arrayOf(emailInLowerCase)
      updateRowCount += contentResolver.update(baseContentUri, valuesActive, selection, selectionArgs)

      updateRowCount

    } else
      -1
  }

  /**
   * Generate a [ContentValues] using [GoogleSignInAccount].
   *
   * @param googleSign The [GoogleSignInAccount] object;
   * @return The generated [ContentValues].
   */
  private fun genContentValues(googleSign: GoogleSignInAccount): ContentValues? {
    val contentValues = ContentValues()
    if (googleSign.email != null) {
      contentValues.put(COL_EMAIL, googleSign.email!!.toLowerCase(Locale.getDefault()))
    } else
      return null

    val account = googleSign.account

    account?.type?.let { contentValues.put(COL_ACCOUNT_TYPE, it.toLowerCase(Locale.getDefault())) }

    contentValues.put(COL_DISPLAY_NAME, googleSign.displayName)
    contentValues.put(COL_USERNAME, googleSign.email)
    contentValues.put(COL_PASSWORD, "")
    contentValues.put(COL_IMAP_SERVER, GmailConstants.GMAIL_IMAP_SERVER)
    contentValues.put(COL_IMAP_PORT, GmailConstants.GMAIL_IMAP_PORT)
    contentValues.put(COL_SMTP_SERVER, GmailConstants.GMAIL_SMTP_SERVER)
    contentValues.put(COL_SMTP_PORT, GmailConstants.GMAIL_SMTP_PORT)
    contentValues.put(COL_IMAP_AUTH_MECHANISMS, JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2)
    contentValues.put(COL_SMTP_AUTH_MECHANISMS, JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2)
    contentValues.put(COL_IMAP_IS_USE_SSL_TLS, 1)
    contentValues.put(COL_SMTP_IS_USE_SSL_TLS, 1)
    contentValues.put(COL_GIVEN_NAME, googleSign.givenName)
    contentValues.put(COL_FAMILY_NAME, googleSign.familyName)
    contentValues.put(COL_IS_ACTIVE, true)

    googleSign.photoUrl?.let { contentValues.put(COL_PHOTO_URL, it.toString()) }

    return contentValues
  }

  /**
   * Generate a [ContentValues] using [AuthCredentials].
   *
   * @param context   Interface to global information about an application environment;
   * @param authCreds The [AuthCredentials] object;
   * @return The generated [ContentValues].
   */
  private fun genContentValues(authCreds: AuthCredentials): ContentValues? {
    val contentValues = ContentValues()
    val email = authCreds.email
    if (!TextUtils.isEmpty(email)) {
      contentValues.put(COL_EMAIL, email.toLowerCase(Locale.getDefault()))
    } else
      return null

    contentValues.put(COL_ACCOUNT_TYPE, email.substring(email.indexOf('@') + 1))
    contentValues.put(COL_USERNAME, authCreds.username)
    contentValues.put(COL_PASSWORD, KeyStoreCryptoManager.encrypt(authCreds.password))
    contentValues.put(COL_IMAP_SERVER, authCreds.imapServer)
    contentValues.put(COL_IMAP_PORT, authCreds.imapPort)
    contentValues.put(COL_IMAP_IS_USE_SSL_TLS, authCreds.imapOpt === SecurityType.Option.SSL_TLS)
    contentValues.put(COL_IMAP_IS_USE_STARTTLS, authCreds.imapOpt === SecurityType.Option.STARTLS)
    contentValues.put(COL_SMTP_SERVER, authCreds.smtpServer)
    contentValues.put(COL_SMTP_PORT, authCreds.smtpPort)
    contentValues.put(COL_SMTP_IS_USE_SSL_TLS, authCreds.smtpOpt === SecurityType.Option.SSL_TLS)
    contentValues.put(COL_SMTP_IS_USE_STARTTLS, authCreds.smtpOpt === SecurityType.Option.STARTLS)
    contentValues.put(COL_SMTP_IS_USE_CUSTOM_SIGN, authCreds.hasCustomSignInForSmtp)
    contentValues.put(COL_SMTP_USERNAME, authCreds.smtpSigInUsername)
    contentValues.put(COL_SMTP_PASSWORD,
        authCreds.smtpSignInPassword?.let { KeyStoreCryptoManager.encrypt(it) })

    contentValues.put(COL_IS_ACTIVE, true)

    return contentValues
  }

  companion object {
    const val TABLE_NAME_ACCOUNTS = "accounts"

    const val COL_EMAIL = "email"
    const val COL_ACCOUNT_TYPE = "account_type"
    const val COL_DISPLAY_NAME = "display_name"
    const val COL_GIVEN_NAME = "given_name"
    const val COL_FAMILY_NAME = "family_name"
    const val COL_PHOTO_URL = "photo_url"
    const val COL_IS_ENABLE = "is_enable"
    const val COL_IS_ACTIVE = "is_active"
    const val COL_USERNAME = "username"
    const val COL_PASSWORD = "password"
    const val COL_IMAP_SERVER = "imap_server"
    const val COL_IMAP_PORT = "imap_port"
    const val COL_IMAP_IS_USE_SSL_TLS = "imap_is_use_ssl_tls"
    const val COL_IMAP_IS_USE_STARTTLS = "imap_is_use_starttls"
    const val COL_IMAP_AUTH_MECHANISMS = "imap_auth_mechanisms"
    const val COL_SMTP_SERVER = "smtp_server"
    const val COL_SMTP_PORT = "smtp_port"
    const val COL_SMTP_IS_USE_SSL_TLS = "smtp_is_use_ssl_tls"
    const val COL_SMTP_IS_USE_STARTTLS = "smtp_is_use_starttls"
    const val COL_SMTP_AUTH_MECHANISMS = "smtp_auth_mechanisms"
    const val COL_SMTP_IS_USE_CUSTOM_SIGN = "smtp_is_use_custom_sign"
    const val COL_SMTP_USERNAME = "smtp_username"
    const val COL_SMTP_PASSWORD = "smtp_password"
    const val COL_IS_CONTACTS_LOADED = "ic_contacts_loaded"
    const val COL_IS_SHOW_ONLY_ENCRYPTED = "is_show_only_encrypted"
    const val COL_UUID = "uuid"
    const val COL_DOMAIN_RULES = "domain_rules"
    const val COL_IS_RESTORE_ACCESS_REQUIRED = "is_restore_access_required"

    const val ACCOUNTS_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
        TABLE_NAME_ACCOUNTS + " (" +
        BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COL_EMAIL + " VARCHAR(100) NOT NULL, " +
        COL_ACCOUNT_TYPE + " VARCHAR(100) DEFAULT NULL, " +
        COL_DISPLAY_NAME + " VARCHAR(100) DEFAULT NULL, " +
        COL_GIVEN_NAME + " VARCHAR(100) DEFAULT NULL, " +
        COL_FAMILY_NAME + " VARCHAR(100) DEFAULT NULL, " +
        COL_PHOTO_URL + " TEXT DEFAULT NULL, " +
        COL_IS_ENABLE + " INTEGER DEFAULT 1, " +
        COL_IS_ACTIVE + " INTEGER DEFAULT 0, " +
        COL_USERNAME + " TEXT NOT NULL, " +
        COL_PASSWORD + " TEXT NOT NULL, " +
        COL_IMAP_SERVER + " TEXT NOT NULL, " +
        COL_IMAP_PORT + " INTEGER DEFAULT 143, " +
        COL_IMAP_IS_USE_SSL_TLS + " INTEGER DEFAULT 0, " +
        COL_IMAP_IS_USE_STARTTLS + " INTEGER DEFAULT 0, " +
        COL_IMAP_AUTH_MECHANISMS + " TEXT, " +
        COL_SMTP_SERVER + " TEXT NOT NULL, " +
        COL_SMTP_PORT + " INTEGER DEFAULT 25, " +
        COL_SMTP_IS_USE_SSL_TLS + " INTEGER DEFAULT 0, " +
        COL_SMTP_IS_USE_STARTTLS + " INTEGER DEFAULT 0, " +
        COL_SMTP_AUTH_MECHANISMS + " TEXT, " +
        COL_SMTP_IS_USE_CUSTOM_SIGN + " INTEGER DEFAULT 0, " +
        COL_SMTP_USERNAME + " TEXT DEFAULT NULL, " +
        COL_SMTP_PASSWORD + " TEXT DEFAULT NULL, " +
        COL_IS_CONTACTS_LOADED + " INTEGER DEFAULT 0, " +
        COL_IS_SHOW_ONLY_ENCRYPTED + " INTEGER DEFAULT 0, " +
        COL_UUID + " TEXT DEFAULT NULL, " +
        COL_DOMAIN_RULES + " TEXT DEFAULT NULL, " +
        COL_IS_RESTORE_ACCESS_REQUIRED + " INTEGER DEFAULT 0 " + ");"

    const val CREATE_INDEX_EMAIL_TYPE_IN_ACCOUNTS = (UNIQUE_INDEX_PREFIX
        + COL_EMAIL + "_" + COL_ACCOUNT_TYPE + "_in_" + TABLE_NAME_ACCOUNTS + " ON " + TABLE_NAME_ACCOUNTS +
        " (" + COL_EMAIL + ", " + COL_ACCOUNT_TYPE + ")")

    /**
     * Generate the [AccountDao] from the current cursor position;
     *
     * @param cursor  The cursor from which to get the data.
     * @return [AccountDao].
     */
    @JvmStatic
    fun getCurrentAccountDao(cursor: Cursor): AccountDao {
      var authCreds: AuthCredentials? = null
      var uuid: String? = null
      try {
        authCreds = getCurrentAuthCredsFromCursor(cursor)
        val encryptedUuid = cursor.getString(cursor.getColumnIndex(COL_UUID))
        if (!encryptedUuid.isNullOrEmpty()) {
          uuid = KeyStoreCryptoManager.decrypt(encryptedUuid)
        }
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
      }

      val domainRulesString = cursor.getString(cursor.getColumnIndex(COL_DOMAIN_RULES))
      val domainRules = if (domainRulesString.isNullOrEmpty()) {
        emptyList()
      } else {
        domainRulesString.split(",").map { it.trim() }
      }

      return AccountDao(
          cursor.getString(cursor.getColumnIndex(COL_EMAIL)),
          cursor.getString(cursor.getColumnIndex(COL_ACCOUNT_TYPE)),
          cursor.getString(cursor.getColumnIndex(COL_DISPLAY_NAME)),
          cursor.getString(cursor.getColumnIndex(COL_GIVEN_NAME)),
          cursor.getString(cursor.getColumnIndex(COL_FAMILY_NAME)),
          cursor.getString(cursor.getColumnIndex(COL_PHOTO_URL)),
          cursor.getInt(cursor.getColumnIndex(COL_IS_CONTACTS_LOADED)) == 1,
          authCreds,
          uuid,
          domainRules,
          cursor.getInt(cursor.getColumnIndex(COL_IS_RESTORE_ACCESS_REQUIRED)) == 1)
    }

    /**
     * Get the current [AuthCredentials] object from the current [Cursor] position.
     *
     * @param cursor  The cursor from which to get the data.
     * @return Generated [AuthCredentials] object.
     * @throws GeneralSecurityException
     */
    @JvmStatic
    private fun getCurrentAuthCredsFromCursor(cursor: Cursor): AuthCredentials {

      var imapOpt: SecurityType.Option = SecurityType.Option.NONE

      if (cursor.getInt(cursor.getColumnIndex(COL_IMAP_IS_USE_SSL_TLS)) == 1) {
        imapOpt = SecurityType.Option.SSL_TLS
      } else if (cursor.getInt(cursor.getColumnIndex(COL_IMAP_IS_USE_STARTTLS)) == 1) {
        imapOpt = SecurityType.Option.STARTLS
      }

      var smtpOpt: SecurityType.Option = SecurityType.Option.NONE

      if (cursor.getInt(cursor.getColumnIndex(COL_SMTP_IS_USE_SSL_TLS)) == 1) {
        smtpOpt = SecurityType.Option.SSL_TLS
      } else if (cursor.getInt(cursor.getColumnIndex(COL_SMTP_IS_USE_STARTTLS)) == 1) {
        smtpOpt = SecurityType.Option.STARTLS
      }

      var originalPassword = cursor.getString(cursor.getColumnIndex(COL_PASSWORD))

      //fixed a bug when try to decrypting the template password.
      // See https://github.com/FlowCrypt/flowcrypt-android/issues/168
      if ("password".equals(originalPassword, ignoreCase = true)) {
        originalPassword = ""
      }

      return AuthCredentials(cursor.getString(cursor.getColumnIndex(COL_EMAIL)),
          cursor.getString(cursor.getColumnIndex(COL_USERNAME)),
          KeyStoreCryptoManager.decrypt(originalPassword),
          cursor.getString(cursor.getColumnIndex(COL_IMAP_SERVER)),
          cursor.getInt(cursor.getColumnIndex(COL_IMAP_PORT)),
          imapOpt,
          cursor.getString(cursor.getColumnIndex(COL_SMTP_SERVER)),
          cursor.getInt(cursor.getColumnIndex(COL_SMTP_PORT)),
          smtpOpt,
          cursor.getInt(cursor.getColumnIndex(COL_SMTP_IS_USE_CUSTOM_SIGN)) == 1,
          cursor.getString(cursor.getColumnIndex(COL_SMTP_USERNAME)),
          KeyStoreCryptoManager.decrypt(cursor.getString(cursor.getColumnIndex(COL_SMTP_PASSWORD))))
    }
  }
}
