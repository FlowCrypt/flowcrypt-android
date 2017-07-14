/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
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
import android.support.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

/**
 * This class describe creating of table which has name
 * {@link AccountDaoSource#TABLE_NAME_ACCOUNTS}, add, delete and update rows.
 *
 * @author Denis Bondarenko
 *         Date: 14.07.2017
 *         Time: 17:43
 *         E-mail: DenBond7@gmail.com
 */

public class AccountDaoSource extends BaseDaoSource {
    public static final String TABLE_NAME_ACCOUNTS = "accounts";

    public static final String COL_EMAIL = "email";
    public static final String COL_ACCOUNT_TYPE = "account_type";
    public static final String COL_DISPLAY_NAME = "display_name";
    public static final String COL_GIVEN_NAME = "given_name";
    public static final String COL_FAMILY_NAME = "family_name";
    public static final String COL_PHOTO_URL = "photo_url";

    public static final String ACCOUNTS_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
            TABLE_NAME_ACCOUNTS + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_EMAIL + " VARCHAR(100) NOT NULL, " +
            COL_ACCOUNT_TYPE + " VARCHAR(100) DEFAULT NULL, " +
            COL_DISPLAY_NAME + " VARCHAR(100) DEFAULT NULL, " +
            COL_GIVEN_NAME + " VARCHAR(100) DEFAULT NULL, " +
            COL_FAMILY_NAME + " VARCHAR(100) DEFAULT NULL, " +
            COL_PHOTO_URL + " TEXT DEFAULT NULL " + ");";

    public static final String CREATE_INDEX_EMAIL_TYPE_IN_ACCOUNTS =
            "CREATE UNIQUE INDEX IF NOT EXISTS " + COL_EMAIL + "_" + COL_ACCOUNT_TYPE +
                    "_in_" + TABLE_NAME_ACCOUNTS + " ON " + TABLE_NAME_ACCOUNTS +
                    " (" + COL_EMAIL + ", " + COL_ACCOUNT_TYPE + ")";

    /**
     * Generate the {@link AccountDao} from the current cursor position;
     *
     * @param cursor The cursor from which to get the data.
     * @return {@link AccountDao}.
     */
    public static AccountDao getCurrentAccountDao(Cursor cursor) {
        return new AccountDao(
                cursor.getString(cursor.getColumnIndex(COL_EMAIL)),
                cursor.getString(cursor.getColumnIndex(COL_ACCOUNT_TYPE)),
                cursor.getString(cursor.getColumnIndex(COL_DISPLAY_NAME)),
                cursor.getString(cursor.getColumnIndex(COL_GIVEN_NAME)),
                cursor.getString(cursor.getColumnIndex(COL_FAMILY_NAME)),
                cursor.getString(cursor.getColumnIndex(COL_PHOTO_URL)));
    }

    @Override
    public String getTableName() {
        return TABLE_NAME_ACCOUNTS;
    }

    /**
     * Save an information about an account using the {@link GoogleSignInAccount};
     *
     * @param context             Interface to global information about an application environment;
     * @param googleSignInAccount Reflecting the user's sign in information.
     * @return The created {@link Uri} or null;
     */
    public Uri addRow(Context context, GoogleSignInAccount googleSignInAccount) {
        ContentResolver contentResolver = context.getContentResolver();
        if (googleSignInAccount != null
                && contentResolver != null) {
            ContentValues contentValues = generateContentValues(googleSignInAccount);
            if (contentValues == null) return null;

            return contentResolver.insert(getBaseContentUri(), contentValues);
        } else return null;
    }

    /**
     * Get a {@link AccountDao} object from the local database.
     *
     * @param context Interface to global information about an application environment.
     * @param email   An email of the some account information.
     * @return The {@link AccountDao};
     */
    public AccountDao getAccountInformation(Context context, String email) {
        if (email != null) {
            email = email.toLowerCase();
        }

        Cursor cursor = context.getContentResolver().query(
                getBaseContentUri(), null, ContactsDaoSource.COL_EMAIL +
                        " = ?", new String[]{email}, null);

        if (cursor != null && cursor.moveToFirst()) {
            return getCurrentAccountDao(cursor);
        }

        if (cursor != null) {
            cursor.close();
        }

        return null;
    }

    /**
     * Update an information about some {@link AccountDao}.
     *
     * @param context             Interface to global information about an application environment.
     * @param googleSignInAccount Reflecting the user's sign in information.
     * @return The count of updated rows. Will be 1 if information about {@link AccountDao} was
     * updated or -1 otherwise.
     */
    public int updateAccountInformation(Context context, GoogleSignInAccount googleSignInAccount) {
        if (googleSignInAccount != null) {
            Account account = googleSignInAccount.getAccount();

            if (account == null) {
                return -1;
            }

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
                ContentValues contentValues = generateContentValues(googleSignInAccount);
                return contentResolver.update(getBaseContentUri(),
                        contentValues,
                        COL_EMAIL + " = ? AND " + COL_ACCOUNT_TYPE + " = ?",
                        new String[]{email, type});
            } else return -1;
        } else return -1;
    }

    /**
     * Delete an information about some {@link AccountDao}.
     *
     * @param context Interface to global information about an application environment.
     * @param account The account name and type.
     * @return The count of updated rows. Will be 1 if information about {@link AccountDao} was
     * updated or -1 otherwise.
     */
    public int deleteAccountInformation(Context context, Account account) {
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
                return contentResolver.delete(getBaseContentUri(),
                        COL_EMAIL + " = ? AND " + COL_ACCOUNT_TYPE + " = ?",
                        new String[]{email, type});
            } else return -1;
        } else return -1;
    }

    /**
     * Generate a {@link ContentValues} using {@link GoogleSignInAccount}.
     *
     * @param googleSignInAccount The {@link GoogleSignInAccount} object;
     * @return The generated {@link ContentValues}.
     */
    @Nullable
    private ContentValues generateContentValues(GoogleSignInAccount googleSignInAccount) {
        ContentValues contentValues = new ContentValues();
        if (googleSignInAccount.getEmail() != null) {
            contentValues.put(COL_EMAIL, googleSignInAccount.getEmail().toLowerCase());
        } else return null;

        Account account = googleSignInAccount.getAccount();

        if (account != null && account.type != null) {
            contentValues.put(COL_ACCOUNT_TYPE, account.type.toLowerCase());
        }

        contentValues.put(COL_DISPLAY_NAME, googleSignInAccount.getDisplayName());
        contentValues.put(COL_GIVEN_NAME, googleSignInAccount.getGivenName());
        contentValues.put(COL_FAMILY_NAME, googleSignInAccount.getFamilyName());
        if (googleSignInAccount.getPhotoUrl() != null) {
            contentValues.put(COL_PHOTO_URL, googleSignInAccount.getPhotoUrl().toString());
        }
        return contentValues;
    }
}
