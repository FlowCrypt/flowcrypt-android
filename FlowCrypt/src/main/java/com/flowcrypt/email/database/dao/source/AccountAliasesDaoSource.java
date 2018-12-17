/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.Nullable;

/**
 * This object describes a logic of work with {@link AccountAliasesDao}.
 *
 * @author Denis Bondarenko
 * Date: 26.10.2017
 * Time: 15:51
 * E-mail: DenBond7@gmail.com
 */

public class AccountAliasesDaoSource extends BaseDaoSource {
  public static final String VERIFICATION_STATUS_ACCEPTED = "accepted";

  public static final String TABLE_NAME_ACCOUNTS_ALIASES = "accounts_aliases";

  public static final String COL_EMAIL = "email";
  public static final String COL_ACCOUNT_TYPE = "account_type";
  public static final String COL_SEND_AS_EMAIL = "send_as_email";
  public static final String COL_DISPLAY_NAME = "display_name";
  public static final String COL_IS_DEFAULT = "is_default";
  public static final String COL_VERIFICATION_STATUS = "verification_status";

  public static final String ACCOUNTS_ALIASES_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
      TABLE_NAME_ACCOUNTS_ALIASES + " (" +
      BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
      COL_EMAIL + " VARCHAR(100) NOT NULL, " +
      COL_ACCOUNT_TYPE + " VARCHAR(100) NOT NULL, " +
      COL_SEND_AS_EMAIL + " VARCHAR(100) NOT NULL, " +
      COL_DISPLAY_NAME + " TEXT DEFAULT NULL, " +
      COL_IS_DEFAULT + " INTEGER DEFAULT 0, " +
      COL_VERIFICATION_STATUS + " TEXT NOT NULL " + ");";

  public static final String CREATE_INDEX_EMAIL_TYPE_IN_ACCOUNTS_ALIASES = UNIQUE_INDEX_PREFIX
      + COL_EMAIL + "_" + COL_ACCOUNT_TYPE + "_" + COL_SEND_AS_EMAIL + "_in_" + TABLE_NAME_ACCOUNTS_ALIASES
      + " ON " + TABLE_NAME_ACCOUNTS_ALIASES + " (" + COL_EMAIL + ", " + COL_ACCOUNT_TYPE + ", " +
      COL_SEND_AS_EMAIL + ")";

  /**
   * Generate the {@link AccountAliasesDao} from the current cursor position;
   *
   * @param cursor The cursor from which to get the data.
   * @return {@link AccountAliasesDao}.
   */
  public static AccountAliasesDao getCurrent(Cursor cursor) {
    AccountAliasesDao dao = new AccountAliasesDao();
    String accountEmail = cursor.getString(cursor.getColumnIndex(COL_EMAIL));
    dao.setEmail(accountEmail != null ? accountEmail.toLowerCase() : null);
    dao.setAccountType(cursor.getString(cursor.getColumnIndex(COL_ACCOUNT_TYPE)));
    String sendAsEmail = cursor.getString(cursor.getColumnIndex(COL_SEND_AS_EMAIL));
    dao.setSendAsEmail(sendAsEmail != null ? sendAsEmail.toLowerCase() : null);
    dao.setDisplayName(cursor.getString(cursor.getColumnIndex(COL_DISPLAY_NAME)));
    dao.setDefault(cursor.getInt(cursor.getColumnIndex(COL_IS_DEFAULT)) == 1);
    dao.setVerificationStatus(cursor.getString(cursor.getColumnIndex(COL_VERIFICATION_STATUS)));
    return dao;
  }

  @Override
  public String getTableName() {
    return TABLE_NAME_ACCOUNTS_ALIASES;
  }

  /**
   * Save information about an account alias using the {@link AccountAliasesDao};
   *
   * @param context Interface to global information about an application environment;
   * @param dao     The user's alias information.
   * @return The created {@link Uri} or null;
   */
  public Uri addRow(Context context, AccountAliasesDao dao) {
    ContentResolver contentResolver = context.getContentResolver();
    if (dao != null && contentResolver != null) {
      ContentValues contentValues = generateContentValues(dao);
      if (contentValues == null) return null;

      return contentResolver.insert(getBaseContentUri(), contentValues);
    } else return null;
  }

  /**
   * This method add rows per single transaction.
   *
   * @param context Interface to global information about an application environment.
   * @param list    The list of an account aliases.
   */
  public int addRows(Context context, List<AccountAliasesDao> list) {
    if (list != null && !list.isEmpty()) {
      ContentResolver contentResolver = context.getContentResolver();
      ContentValues[] contentValuesArray = new ContentValues[list.size()];

      for (int i = 0; i < list.size(); i++) {
        AccountAliasesDao accountAliasesDao = list.get(i);
        contentValuesArray[i] = generateContentValues(accountAliasesDao);
      }

      return contentResolver.bulkInsert(getBaseContentUri(), contentValuesArray);
    } else return 0;
  }

  /**
   * Get the list of {@link AccountAliasesDao} object from the local database for some email.
   *
   * @param context Interface to global information about an application environment.
   * @param account An account information.
   * @return The list of {@link AccountAliasesDao};
   */
  public List<AccountAliasesDao> getAliases(Context context, AccountDao account) {
    List<AccountAliasesDao> accountAliasesDaoList = new ArrayList<>();
    if (account != null) {
      String selection = AccountDaoSource.COL_EMAIL + " = ? AND " + AccountDaoSource.COL_ACCOUNT_TYPE + " = ?";
      String[] selectionArgs = new String[]{account.getEmail(), account.getAccountType()};
      Cursor cursor = context.getContentResolver().query(getBaseContentUri(), null, selection, selectionArgs, null);

      if (cursor != null) {
        while (cursor.moveToNext()) {
          accountAliasesDaoList.add(getCurrent(cursor));
        }
      }

      if (cursor != null) {
        cursor.close();
      }
    }

    return accountAliasesDaoList;
  }

  /**
   * Update information about aliases of some {@link AccountDao}.
   *
   * @param context Interface to global information about an application environment.
   * @param account The object which contains information about an email account.
   * @param list    The list of an account aliases.
   * @return The count of updated rows. Will be 1 if information about {@link AccountDao} was
   * updated or -1 otherwise.
   */
  public int updateAliases(Context context, AccountDao account, List<AccountAliasesDao> list) {
    deleteAccountAliases(context, account);
    return addRows(context, list);
  }

  /**
   * Delete information about aliases of some {@link AccountDao}.
   *
   * @param context Interface to global information about an application environment.
   * @param account The object which contains information about an email account.
   * @return The count of deleted rows. Will be 1 if information about {@link AccountDao} was
   * deleted or -1 otherwise.
   */
  public int deleteAccountAliases(Context context, AccountDao account) {
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
        String where = COL_EMAIL + " = ? AND " + COL_ACCOUNT_TYPE + " = ?";
        return contentResolver.delete(getBaseContentUri(), where, new String[]{email, type});
      } else return -1;
    } else return -1;
  }

  /**
   * Generate a {@link ContentValues} using {@link AccountAliasesDao}.
   *
   * @param accountAliasesDao The {@link AccountAliasesDao} object;
   * @return The generated {@link ContentValues}.
   */
  @Nullable
  private ContentValues generateContentValues(AccountAliasesDao accountAliasesDao) {
    ContentValues contentValues = new ContentValues();
    if (accountAliasesDao.getEmail() != null) {
      contentValues.put(COL_EMAIL, accountAliasesDao.getEmail().toLowerCase());
    } else {
      return null;
    }

    contentValues.put(COL_ACCOUNT_TYPE, accountAliasesDao.getAccountType());
    contentValues.put(COL_DISPLAY_NAME, accountAliasesDao.getDisplayName());
    contentValues.put(COL_SEND_AS_EMAIL, accountAliasesDao.getSendAsEmail().toLowerCase());
    contentValues.put(COL_SEND_AS_EMAIL, accountAliasesDao.getSendAsEmail().toLowerCase());
    contentValues.put(COL_IS_DEFAULT, accountAliasesDao.isDefault());
    contentValues.put(COL_VERIFICATION_STATUS, accountAliasesDao.getVerificationStatus());
    return contentValues;
  }

}
