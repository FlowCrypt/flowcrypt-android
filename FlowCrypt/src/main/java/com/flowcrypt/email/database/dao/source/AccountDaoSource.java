/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.AuthCredentials;
import com.flowcrypt.email.api.email.model.SecurityType;
import com.flowcrypt.email.security.KeyStoreCryptoManager;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * This class describe creating of table which has name
 * {@link AccountDaoSource#TABLE_NAME_ACCOUNTS}, add, delete and update rows.
 *
 * @author Denis Bondarenko
 * Date: 14.07.2017
 * Time: 17:43
 * E-mail: DenBond7@gmail.com
 */

public class AccountDaoSource extends BaseDaoSource {
  public static final String TABLE_NAME_ACCOUNTS = "accounts";

  public static final String COL_EMAIL = "email";
  public static final String COL_ACCOUNT_TYPE = "account_type";
  public static final String COL_DISPLAY_NAME = "display_name";
  public static final String COL_GIVEN_NAME = "given_name";
  public static final String COL_FAMILY_NAME = "family_name";
  public static final String COL_PHOTO_URL = "photo_url";
  public static final String COL_IS_ENABLE = "is_enable";
  public static final String COL_IS_ACTIVE = "is_active";
  public static final String COL_USERNAME = "username";
  public static final String COL_PASSWORD = "password";
  public static final String COL_IMAP_SERVER = "imap_server";
  public static final String COL_IMAP_PORT = "imap_port";
  public static final String COL_IMAP_IS_USE_SSL_TLS = "imap_is_use_ssl_tls";
  public static final String COL_IMAP_IS_USE_STARTTLS = "imap_is_use_starttls";
  public static final String COL_IMAP_AUTH_MECHANISMS = "imap_auth_mechanisms";
  public static final String COL_SMTP_SERVER = "smtp_server";
  public static final String COL_SMTP_PORT = "smtp_port";
  public static final String COL_SMTP_IS_USE_SSL_TLS = "smtp_is_use_ssl_tls";
  public static final String COL_SMTP_IS_USE_STARTTLS = "smtp_is_use_starttls";
  public static final String COL_SMTP_AUTH_MECHANISMS = "smtp_auth_mechanisms";
  public static final String COL_SMTP_IS_USE_CUSTOM_SIGN = "smtp_is_use_custom_sign";
  public static final String COL_SMTP_USERNAME = "smtp_username";
  public static final String COL_SMTP_PASSWORD = "smtp_password";
  public static final String COL_IS_CONTACTS_LOADED = "ic_contacts_loaded";
  public static final String COL_IS_SHOW_ONLY_ENCRYPTED = "is_show_only_encrypted";

  public static final String ACCOUNTS_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
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
      COL_IS_SHOW_ONLY_ENCRYPTED + " INTEGER DEFAULT 0 " + ");";

  public static final String CREATE_INDEX_EMAIL_TYPE_IN_ACCOUNTS = UNIQUE_INDEX_PREFIX
      + COL_EMAIL + "_" + COL_ACCOUNT_TYPE + "_in_" + TABLE_NAME_ACCOUNTS + " ON " + TABLE_NAME_ACCOUNTS +
      " (" + COL_EMAIL + ", " + COL_ACCOUNT_TYPE + ")";

  /**
   * Generate the {@link AccountDao} from the current cursor position;
   *
   * @param context Interface to global information about an application environment;
   * @param cursor  The cursor from which to get the data.
   * @return {@link AccountDao}.
   */
  public static AccountDao getCurrentAccountDao(Context context, Cursor cursor) {
    AuthCredentials authCreds = null;
    try {
      KeyStoreCryptoManager keyStoreCryptoManager = KeyStoreCryptoManager.getInstance(context);
      authCreds = getCurrentAuthCredsFromCursor(context, keyStoreCryptoManager, cursor);
    } catch (Exception e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }

    return new AccountDao(
        cursor.getString(cursor.getColumnIndex(COL_EMAIL)),
        cursor.getString(cursor.getColumnIndex(COL_ACCOUNT_TYPE)),
        cursor.getString(cursor.getColumnIndex(COL_DISPLAY_NAME)),
        cursor.getString(cursor.getColumnIndex(COL_GIVEN_NAME)),
        cursor.getString(cursor.getColumnIndex(COL_FAMILY_NAME)),
        cursor.getString(cursor.getColumnIndex(COL_PHOTO_URL)), authCreds,
        cursor.getInt(cursor.getColumnIndex(COL_IS_CONTACTS_LOADED)) == 1);
  }

  /**
   * Get the current {@link AuthCredentials} object from the current {@link Cursor} position.
   *
   * @param context Interface to global information about an application environment;
   * @param manager The manager which does encryption/decryption work.
   * @param cursor  The cursor from which to get the data.
   * @return Generated {@link AuthCredentials} object.
   * @throws GeneralSecurityException
   */
  public static AuthCredentials getCurrentAuthCredsFromCursor(Context context, KeyStoreCryptoManager manager,
                                                              Cursor cursor) throws Exception {

    SecurityType.Option imapOpt = SecurityType.Option.NONE;

    if (cursor.getInt(cursor.getColumnIndex(COL_IMAP_IS_USE_SSL_TLS)) == 1) {
      imapOpt = SecurityType.Option.SSL_TLS;
    } else if (cursor.getInt(cursor.getColumnIndex(COL_IMAP_IS_USE_STARTTLS)) == 1) {
      imapOpt = SecurityType.Option.STARTLS;
    }

    SecurityType.Option smtpOpt = SecurityType.Option.NONE;

    if (cursor.getInt(cursor.getColumnIndex(COL_SMTP_IS_USE_SSL_TLS)) == 1) {
      smtpOpt = SecurityType.Option.SSL_TLS;
    } else if (cursor.getInt(cursor.getColumnIndex(COL_SMTP_IS_USE_STARTTLS)) == 1) {
      smtpOpt = SecurityType.Option.STARTLS;
    }

    String originalPassword = cursor.getString(cursor.getColumnIndex(COL_PASSWORD));

    //fixed a bug when try to decrypting the template password.
    // See https://github.com/FlowCrypt/flowcrypt-android/issues/168
    if ("password".equalsIgnoreCase(originalPassword)) {
      originalPassword = "";
    }

    return new AuthCredentials(cursor.getString(cursor.getColumnIndex(COL_EMAIL)),
        cursor.getString(cursor.getColumnIndex(COL_USERNAME)),
        manager.decryptWithRSAOrAES(context, originalPassword),
        cursor.getString(cursor.getColumnIndex(COL_IMAP_SERVER)),
        cursor.getInt(cursor.getColumnIndex(COL_IMAP_PORT)),
        imapOpt,
        cursor.getString(cursor.getColumnIndex(COL_SMTP_SERVER)),
        cursor.getInt(cursor.getColumnIndex(COL_SMTP_PORT)),
        smtpOpt,
        cursor.getInt(cursor.getColumnIndex(COL_SMTP_IS_USE_CUSTOM_SIGN)) == 1,
        cursor.getString(cursor.getColumnIndex(COL_SMTP_USERNAME)),
        manager.decryptWithRSAOrAES(context, cursor.getString(cursor.getColumnIndex(COL_SMTP_PASSWORD))
        ));
  }

  @Override
  public String getTableName() {
    return TABLE_NAME_ACCOUNTS;
  }

  /**
   * Save information about an account using the {@link GoogleSignInAccount};
   *
   * @param context             Interface to global information about an application environment;
   * @param googleSignInAccount Reflecting the user's sign in information.
   * @return The created {@link Uri} or null;
   */
  public Uri addRow(Context context, GoogleSignInAccount googleSignInAccount) {
    ContentResolver contentResolver = context.getContentResolver();
    if (googleSignInAccount != null && contentResolver != null) {
      ContentValues contentValues = genContentValues(googleSignInAccount);
      if (contentValues == null) return null;

      return contentResolver.insert(getBaseContentUri(), contentValues);
    } else return null;
  }

  /**
   * Save information about an account using the {@link AuthCredentials};
   *
   * @param context   Interface to global information about an application environment;
   * @param authCreds The sign-in settings of IMAP and SMTP servers.
   * @return The created {@link Uri} or null;
   * @throws Exception An exception maybe occurred when encrypt the user password.
   */
  public Uri addRow(Context context, AuthCredentials authCreds) throws Exception {
    ContentResolver contentResolver = context.getContentResolver();
    if (authCreds != null && contentResolver != null) {
      ContentValues contentValues = genContentValues(context, authCreds);
      if (contentValues == null) return null;

      return contentResolver.insert(getBaseContentUri(), contentValues);
    } else return null;
  }

  /**
   * Get an active {@link AccountDao} object from the local database.
   *
   * @param context Interface to global information about an application environment.
   * @return The {@link AccountDao};
   */
  public AccountDao getActiveAccountInformation(Context context) {
    String selection = AccountDaoSource.COL_IS_ACTIVE + " = ?";
    Cursor cursor = context.getContentResolver().query(getBaseContentUri(), null, selection, new String[]{"1"}, null);

    AccountDao account = null;

    if (cursor != null && cursor.moveToFirst()) {
      account = getCurrentAccountDao(context, cursor);
    }

    if (cursor != null) {
      cursor.close();
    }

    return account;
  }

  /**
   * Get a {@link AccountDao} object from the local database.
   *
   * @param context Interface to global information about an application environment.
   * @param email   An email of the some account information.
   * @return The {@link AccountDao};
   */
  public AccountDao getAccountInformation(Context context, String email) {
    String emailInLowerCase = TextUtils.isEmpty(email) ? email : email.toLowerCase();
    String selection = AccountDaoSource.COL_EMAIL + " = ?";
    String[] selectionArgs = new String[]{emailInLowerCase};
    Cursor cursor = context.getContentResolver().query(getBaseContentUri(), null, selection, selectionArgs, null);

    if (cursor != null && cursor.moveToFirst()) {
      return getCurrentAccountDao(context, cursor);
    }

    if (cursor != null) {
      cursor.close();
    }

    return null;
  }

  /**
   * Update information about some {@link AccountDao}.
   *
   * @param context    Interface to global information about an application environment.
   * @param googleSign Reflecting the user's sign in information.
   * @return The count of updated rows. Will be 1 if information about {@link AccountDao} was
   * updated or -1 otherwise.
   */
  public int updateAccountInformation(Context context, GoogleSignInAccount googleSign) {
    if (googleSign != null) {
      return updateAccountInformation(context, googleSign.getAccount(), genContentValues(googleSign));
    } else return -1;
  }

  /**
   * Update information about some {@link AccountDao}.
   *
   * @param context       Interface to global information about an application environment.
   * @param account       An {@link Account} which will be updated
   * @param contentValues Data fro modification
   * @return The count of updated rows. Will be 1 if information about {@link AccountDao} was
   * updated or -1 otherwise.
   */
  public int updateAccountInformation(Context context, Account account, ContentValues contentValues) {
    if (account != null) {
      String email = account.name;
      if (email == null) {
        return -1;
      } else {
        email = email.toLowerCase();
      }

      String type = account.type;
      if (type == null) {
        return -1;
      } else {
        type = type.toLowerCase();
      }

      ContentResolver contentResolver = context.getContentResolver();
      if (contentResolver != null) {
        String selection = COL_EMAIL + " = ? AND " + COL_ACCOUNT_TYPE + " = ?";
        return contentResolver.update(getBaseContentUri(), contentValues, selection, new String[]{email, type});
      } else return -1;
    } else return -1;
  }

  /**
   * Delete information about some {@link AccountDao}.
   *
   * @param context Interface to global information about an application environment.
   * @param account The object which contains information about an email account.
   * @return The count of deleted rows. Will be 1 if information about {@link AccountDao} was
   * deleted or -1 otherwise.
   */
  public int deleteAccountInformation(Context context, AccountDao account) {
    if (account != null) {

      String email = account.getEmail();
      if (email == null) {
        return -1;
      } else {
        email = email.toLowerCase();
      }

      String type = account.getAccountType();
      if (type == null) {
        return -1;
      } else {
        type = type.toLowerCase();
      }

      ContentResolver contentResolver = context.getContentResolver();
      if (contentResolver != null) {
        String selection = COL_EMAIL + " = ? AND " + COL_ACCOUNT_TYPE + " = ?";
        return contentResolver.delete(getBaseContentUri(), selection, new String[]{email, type});
      } else return -1;
    } else return -1;
  }

  /**
   * Get the list of all added account without the active account.
   *
   * @param context Interface to global information about an application environment.
   * @param email   An email of the active account.
   * @return The list of all added account without the active account
   */
  public List<AccountDao> getAccountsWithoutActive(Context context, String email) {
    String emailInLowerCase = TextUtils.isEmpty(email) ? email : email.toLowerCase();

    String selection = AccountDaoSource.COL_EMAIL + " != ?";
    String[] selectionArgs = new String[]{emailInLowerCase};
    Cursor cursor = context.getContentResolver().query(getBaseContentUri(), null, selection, selectionArgs, null);

    List<AccountDao> accountDaoList = new ArrayList<>();
    if (cursor != null) {
      while (cursor.moveToNext()) {
        accountDaoList.add(getCurrentAccountDao(context, cursor));
      }
    }

    if (cursor != null) {
      cursor.close();
    }

    return accountDaoList;
  }

  /**
   * Checking of showing only encrypted messages for the given account.
   *
   * @param context Interface to global information about an application environment.
   * @param email   An email of the active account.
   * @return true if need to show only encrypted messages
   */
  public boolean isEncryptedModeEnabled(Context context, String email) {
    String emailInLowerCase = TextUtils.isEmpty(email) ? email : email.toLowerCase();

    String selection = AccountDaoSource.COL_EMAIL + " = ?";
    String[] selectionArgs = new String[]{emailInLowerCase};
    Cursor cursor = context.getContentResolver().query(getBaseContentUri(), null, selection, selectionArgs, null);

    boolean isEncryptedModeEnabled = false;

    if (cursor != null && cursor.moveToFirst()) {
      isEncryptedModeEnabled = cursor.getInt(cursor.getColumnIndex(COL_IS_SHOW_ONLY_ENCRYPTED)) == 1;
    }

    if (cursor != null) {
      cursor.close();
    }

    return isEncryptedModeEnabled;
  }

  /**
   * Change a value of {@link #COL_IS_SHOW_ONLY_ENCRYPTED} field for the given account.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The account which will be set as active.
   * @return The count of updated rows.
   */
  public int setShowOnlyEncryptedMsgs(Context context, String email, boolean onlyEncryptedMsgs) {
    if (email == null) {
      return -1;
    }

    String emailInLowerCase = TextUtils.isEmpty(email) ? email : email.toLowerCase();

    ContentResolver contentResolver = context.getContentResolver();
    if (contentResolver != null) {
      ContentValues contentValues = new ContentValues();
      contentValues.put(COL_IS_SHOW_ONLY_ENCRYPTED, onlyEncryptedMsgs);
      String where = COL_EMAIL + " = ? ";
      return contentResolver.update(getBaseContentUri(), contentValues, where, new String[]{emailInLowerCase});
    } else return -1;
  }

  /**
   * Mark some account as active.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The account which will be set as active.
   * @return The count of updated rows.
   */
  public int setActiveAccount(Context context, String email) {
    if (email == null) {
      return -1;
    }

    String emailInLowerCase = TextUtils.isEmpty(email) ? email : email.toLowerCase();
    ContentResolver contentResolver = context.getContentResolver();
    if (contentResolver != null) {
      ContentValues contentValuesDeactivateAllAccount = new ContentValues();
      contentValuesDeactivateAllAccount.put(COL_IS_ACTIVE, 0);
      int updateRowCount = contentResolver.update(getBaseContentUri(), contentValuesDeactivateAllAccount, null, null);

      ContentValues valuesActive = new ContentValues();
      valuesActive.put(COL_IS_ACTIVE, 1);
      String selection = COL_EMAIL + " = ? ";
      String[] selectionArgs = new String[]{emailInLowerCase};
      updateRowCount += contentResolver.update(getBaseContentUri(), valuesActive, selection, selectionArgs);

      return updateRowCount;

    } else return -1;
  }

  /**
   * Generate a {@link ContentValues} using {@link GoogleSignInAccount}.
   *
   * @param googleSign The {@link GoogleSignInAccount} object;
   * @return The generated {@link ContentValues}.
   */
  @Nullable
  private ContentValues genContentValues(GoogleSignInAccount googleSign) {
    ContentValues contentValues = new ContentValues();
    if (googleSign.getEmail() != null) {
      contentValues.put(COL_EMAIL, googleSign.getEmail().toLowerCase());
    } else return null;

    Account account = googleSign.getAccount();

    if (account != null && account.type != null) {
      contentValues.put(COL_ACCOUNT_TYPE, account.type.toLowerCase());
    }

    contentValues.put(COL_DISPLAY_NAME, googleSign.getDisplayName());
    contentValues.put(COL_USERNAME, googleSign.getEmail());
    contentValues.put(COL_PASSWORD, "");
    contentValues.put(COL_IMAP_SERVER, GmailConstants.GMAIL_IMAP_SERVER);
    contentValues.put(COL_IMAP_PORT, GmailConstants.GMAIL_IMAP_PORT);
    contentValues.put(COL_SMTP_SERVER, GmailConstants.GMAIL_SMTP_SERVER);
    contentValues.put(COL_SMTP_PORT, GmailConstants.GMAIL_SMTP_PORT);
    contentValues.put(COL_IMAP_AUTH_MECHANISMS, JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2);
    contentValues.put(COL_SMTP_AUTH_MECHANISMS, JavaEmailConstants.AUTH_MECHANISMS_XOAUTH2);
    contentValues.put(COL_IMAP_IS_USE_SSL_TLS, 1);
    contentValues.put(COL_SMTP_IS_USE_SSL_TLS, 1);
    contentValues.put(COL_GIVEN_NAME, googleSign.getGivenName());
    contentValues.put(COL_FAMILY_NAME, googleSign.getFamilyName());
    contentValues.put(COL_IS_ACTIVE, true);
    if (googleSign.getPhotoUrl() != null) {
      contentValues.put(COL_PHOTO_URL, googleSign.getPhotoUrl().toString());
    }
    return contentValues;
  }

  /**
   * Generate a {@link ContentValues} using {@link AuthCredentials}.
   *
   * @param context   Interface to global information about an application environment;
   * @param authCreds The {@link AuthCredentials} object;
   * @return The generated {@link ContentValues}.
   */
  private ContentValues genContentValues(Context context, AuthCredentials authCreds) throws Exception {
    ContentValues contentValues = new ContentValues();
    String email = authCreds.getEmail();
    if (!TextUtils.isEmpty(email)) {
      contentValues.put(COL_EMAIL, email.toLowerCase());
    } else return null;

    KeyStoreCryptoManager keyStoreCryptoManager = KeyStoreCryptoManager.getInstance(context);

    contentValues.put(COL_ACCOUNT_TYPE, email.substring(email.indexOf('@') + 1));
    contentValues.put(COL_USERNAME, authCreds.getUsername());
    contentValues.put(COL_PASSWORD, keyStoreCryptoManager.encryptWithRSAOrAES(authCreds.getPassword()));
    contentValues.put(COL_IMAP_SERVER, authCreds.getImapServer());
    contentValues.put(COL_IMAP_PORT, authCreds.getImapPort());
    contentValues.put(COL_IMAP_IS_USE_SSL_TLS, authCreds.getImapOpt() == SecurityType.Option.SSL_TLS);
    contentValues.put(COL_IMAP_IS_USE_STARTTLS, authCreds.getImapOpt() == SecurityType.Option.STARTLS);
    contentValues.put(COL_SMTP_SERVER, authCreds.getSmtpServer());
    contentValues.put(COL_SMTP_PORT, authCreds.getSmtpPort());
    contentValues.put(COL_SMTP_IS_USE_SSL_TLS, authCreds.getSmtpOpt() == SecurityType.Option.SSL_TLS);
    contentValues.put(COL_SMTP_IS_USE_STARTTLS, authCreds.getSmtpOpt() == SecurityType.Option.STARTLS);
    contentValues.put(COL_SMTP_IS_USE_CUSTOM_SIGN, authCreds.getHasCustomSignInForSmtp());
    contentValues.put(COL_SMTP_USERNAME, authCreds.getSmtpSigInUsername());
    contentValues.put(COL_SMTP_PASSWORD, keyStoreCryptoManager.encryptWithRSAOrAES(authCreds.getSmtpSignInPassword()));

    contentValues.put(COL_IS_ACTIVE, true);

    return contentValues;
  }

}
