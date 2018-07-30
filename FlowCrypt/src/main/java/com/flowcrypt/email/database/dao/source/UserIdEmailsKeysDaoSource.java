/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Pair;

import java.util.List;

/**
 * This class describe table {@link UserIdEmailsKeysDaoSource#TABLE_NAME_USER_ID_EMAILS_AND_KEYS} and operations with it
 *
 * @author Denis Bondarenko
 * Date: 30.07.2018
 * Time: 10:16
 * E-mail: DenBond7@gmail.com
 */
public class UserIdEmailsKeysDaoSource extends BaseDaoSource {
    public static final String TABLE_NAME_USER_ID_EMAILS_AND_KEYS = "user_id_emails_and_keys";

    public static final String COL_LONG_ID = "long_id";
    public static final String COL_USER_ID_EMAIL = "user_id_email";

    public static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " +
            TABLE_NAME_USER_ID_EMAILS_AND_KEYS + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_LONG_ID + " VARCHAR(16) NOT NULL, " +
            COL_USER_ID_EMAIL + " VARCHAR(20) NOT NULL " + ");";

    public static final String INDEX_LONG_ID__USER_ID_EMAIL =
            "CREATE UNIQUE INDEX IF NOT EXISTS " + COL_LONG_ID + "_" + COL_USER_ID_EMAIL + "_in_"
                    + TABLE_NAME_USER_ID_EMAILS_AND_KEYS + " ON " + TABLE_NAME_USER_ID_EMAILS_AND_KEYS
                    + " (" + COL_LONG_ID + ", " + COL_USER_ID_EMAIL + ")";

    @Override
    public String getTableName() {
        return TABLE_NAME_USER_ID_EMAILS_AND_KEYS;
    }

    /**
     * Add information about a combination of <code>longId</code> and <code>email</code> to the database.
     *
     * @param context Interface to global information about an application environment.
     * @param longId  A <code>longId</code> value of some private key.
     * @param email   An email of some <code>uid</code> of some key.
     * @return <tt>{@link Uri}</tt> which contain information about an inserted row or null if the
     * row not inserted.
     */
    public Uri addRow(Context context, String longId, String email) {
        ContentResolver contentResolver = context.getContentResolver();
        if (!TextUtils.isEmpty(longId) && !TextUtils.isEmpty(email) && contentResolver != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL_LONG_ID, longId);
            contentValues.put(COL_USER_ID_EMAIL, email);
            return contentResolver.insert(getBaseContentUri(), contentValues);
        } else return null;
    }

    /**
     * This method add rows per single transaction. This method must be called in the non-UI thread.
     *
     * @param context Interface to global information about an application environment.
     * @param pairs   A list of {@link Pair}, which contains information about a combination of <code>longId</code> and
     *                <code>email</code>.
     * @return the number of newly created rows.
     */
    public int addRows(Context context, List<Pair<String, String>> pairs) {
        if (pairs != null && !pairs.isEmpty()) {
            ContentResolver contentResolver = context.getContentResolver();
            ContentValues[] contentValuesArray = new ContentValues[pairs.size()];

            for (int i = 0; i < pairs.size(); i++) {
                Pair<String, String> pair = pairs.get(i);
                ContentValues contentValues = new ContentValues();
                contentValues.put(COL_LONG_ID, pair.first);
                contentValues.put(COL_USER_ID_EMAIL, pair.second);
                contentValuesArray[i] = contentValues;
            }

            return contentResolver.bulkInsert(getBaseContentUri(), contentValuesArray);
        } else return 0;
    }
}
