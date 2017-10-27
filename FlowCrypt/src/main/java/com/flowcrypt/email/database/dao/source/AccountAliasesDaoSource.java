/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * This object describes a logic of work with {@link AccountAliasesDao}.
 *
 * @author Denis Bondarenko
 *         Date: 26.10.2017
 *         Time: 15:51
 *         E-mail: DenBond7@gmail.com
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

    public static final String CREATE_INDEX_EMAIL_TYPE_IN_ACCOUNTS_ALIASES = "CREATE UNIQUE INDEX IF NOT EXISTS "
            + COL_EMAIL + "_" + COL_ACCOUNT_TYPE + "_in_" + TABLE_NAME_ACCOUNTS_ALIASES + " ON " +
            TABLE_NAME_ACCOUNTS_ALIASES +
            " (" + COL_EMAIL + ", " + COL_ACCOUNT_TYPE + ")";

    /**
     * Generate the {@link AccountAliasesDao} from the current cursor position;
     *
     * @param context Interface to global information about an application environment;
     * @param cursor  The cursor from which to get the data.
     * @return {@link AccountAliasesDao}.
     */
    public static AccountAliasesDao getCurrentAccountAliasesDao(Context context, Cursor cursor) {
        AccountAliasesDao accountAliasesDao = new AccountAliasesDao();
        accountAliasesDao.setEmail(cursor.getString(cursor.getColumnIndex(COL_EMAIL)));
        accountAliasesDao.setAccountType(cursor.getString(cursor.getColumnIndex(COL_ACCOUNT_TYPE)));
        accountAliasesDao.setSendAsEmail(cursor.getString(cursor.getColumnIndex(COL_SEND_AS_EMAIL)));
        accountAliasesDao.setDisplayName(cursor.getString(cursor.getColumnIndex(COL_DISPLAY_NAME)));
        accountAliasesDao.setDefault(cursor.getInt(cursor.getColumnIndex(COL_IS_DEFAULT)) == 1);
        accountAliasesDao.setVerificationStatus(cursor.getString(cursor.getColumnIndex(COL_VERIFICATION_STATUS)));
        return accountAliasesDao;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME_ACCOUNTS_ALIASES;
    }

    /**
     * Save information about an account alias using the {@link AccountAliasesDao};
     *
     * @param context           Interface to global information about an application environment;
     * @param accountAliasesDao The user's alias information.
     * @return The created {@link Uri} or null;
     */
    public Uri addRow(Context context, AccountAliasesDao accountAliasesDao) {
        ContentResolver contentResolver = context.getContentResolver();
        if (accountAliasesDao != null && contentResolver != null) {
            ContentValues contentValues = generateContentValues(accountAliasesDao);
            if (contentValues == null) return null;

            return contentResolver.insert(getBaseContentUri(), contentValues);
        } else return null;
    }

    /**
     * This method add rows per single transaction.
     *
     * @param context               Interface to global information about an application environment.
     * @param accountAliasesDaoList The list of an account aliases.
     */
    public int addRows(Context context, List<AccountAliasesDao> accountAliasesDaoList) {
        if (accountAliasesDaoList != null && !accountAliasesDaoList.isEmpty()) {
            ContentResolver contentResolver = context.getContentResolver();
            ContentValues[] contentValuesArray = new ContentValues[accountAliasesDaoList.size()];

            for (int i = 0; i < accountAliasesDaoList.size(); i++) {
                AccountAliasesDao accountAliasesDao = accountAliasesDaoList.get(i);
                contentValuesArray[i] = generateContentValues(accountAliasesDao);
            }

            return contentResolver.bulkInsert(getBaseContentUri(), contentValuesArray);
        } else return 0;
    }

    /**
     * Get the list of {@link AccountAliasesDao} object from the local database for some email.
     *
     * @param context    Interface to global information about an application environment.
     * @param accountDao An account information.
     * @return The list of {@link AccountAliasesDao};
     */
    public List<AccountAliasesDao> getAliases(Context context, AccountDao accountDao) {
        List<AccountAliasesDao> accountAliasesDaoList = new ArrayList<>();
        if (accountDao != null) {
            Cursor cursor = context.getContentResolver().query(getBaseContentUri(), null,
                    AccountDaoSource.COL_EMAIL + " = ? AND " + AccountDaoSource.COL_ACCOUNT_TYPE + " = ?",
                    new String[]{accountDao.getEmail(), accountDao.getAccountType()}, null);

            if (cursor != null) {
                while (cursor.moveToNext()) {
                    accountAliasesDaoList.add(getCurrentAccountAliasesDao(context, cursor));
                }
            }

            if (cursor != null) {
                cursor.close();
            }
        }

        return accountAliasesDaoList;
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
