/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.database.dao.source.KeysDaoSource;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;


/**
 * A helper class to manage database creation and version management.
 *
 * @author DenBond7
 *         Date: 13.05.2017
 *         Time: 12:20
 *         E-mail: DenBond7@gmail.com
 */
public class FlowCryptSQLiteOpenHelper extends SQLiteOpenHelper {
    public static final String COLUMN_NAME_COUNT = "COUNT(*)";
    public static final String DB_NAME = "flowcrypt.db";
    public static final int DB_VERSION = 1;

    private static final String TAG = FlowCryptSQLiteOpenHelper.class.getSimpleName();
    private static final String DROP_TABLE = "DROP TABLE IF EXISTS ";

    public FlowCryptSQLiteOpenHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static void dropTable(SQLiteDatabase sqLiteDatabase, String tableName) {
        sqLiteDatabase.execSQL(DROP_TABLE + tableName);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(KeysDaoSource.KEYS_TABLE_SQL_CREATE);
        sqLiteDatabase.execSQL(KeysDaoSource.CREATE_INDEX_LONG_ID_IN_KEYS);

        sqLiteDatabase.execSQL(ContactsDaoSource.CONTACTS_TABLE_SQL_CREATE);
        sqLiteDatabase.execSQL(ContactsDaoSource.CREATE_INDEX_EMAIL_IN_CONTACT);
        sqLiteDatabase.execSQL(ContactsDaoSource.CREATE_INDEX_NAME_IN_CONTACT);
        sqLiteDatabase.execSQL(ContactsDaoSource.CREATE_INDEX_HAS_PGP_IN_CONTACT);
        sqLiteDatabase.execSQL(ContactsDaoSource.CREATE_INDEX_LONG_ID_IN_CONTACT);
        sqLiteDatabase.execSQL(ContactsDaoSource.CREATE_INDEX_LAST_USE_IN_CONTACT);

        sqLiteDatabase.execSQL(ImapLabelsDaoSource.IMAP_LABELS_TABLE_SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.i(TAG, "Database updated from OLD_VERSION = " + Integer.toString(oldVersion)
                + " to NEW_VERSION = " + Integer.toString(newVersion));
    }
}
