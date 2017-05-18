package com.flowcrypt.email.database.dao.source;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.provider.BaseColumns;

import com.flowcrypt.email.test.PgpContact;

/**
 * This class describe creating of table which has name
 * {@link ContactsDaoSource#TABLE_NAME_CONTACTS}, add, delete and update rows.
 *
 * @author DenBond7
 *         Date: 17.05.2017
 *         Time: 12:22
 *         E-mail: DenBond7@gmail.com
 */

public class ContactsDaoSource extends BaseDaoSource {
    public static final String TABLE_NAME_CONTACTS = "contacts";

    public static final String COL_EMAIL = "email";
    public static final String COL_NAME = "name";
    public static final String COL_PUBLIC_KEY = "public_key";
    public static final String COL_HAS_PGP = "has_pgp";
    public static final String COL_CLIENT = "client";
    public static final String COL_ATTESTED = "attested";
    public static final String COL_FINGERPRINT = "fingerprint";
    public static final String COL_LONG_ID = "long_id";
    public static final String COL_KEYWORDS = "keywords";
    public static final String COL_LAST_USE = "last_use";

    public static final String CONTACTS_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
            TABLE_NAME_CONTACTS + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_EMAIL + " VARCHAR(100) NOT NULL, " +
            COL_NAME + " VARCHAR(50) NOT NULL, " +
            COL_PUBLIC_KEY + " BLOB NOT NULL, " +
            COL_HAS_PGP + " BOOLEAN NOT NULL, " +
            COL_CLIENT + " VARCHAR(20) DEFAULT NULL, " +
            COL_ATTESTED + " BOOLEAN DEFAULT NULL, " +
            COL_FINGERPRINT + " VARCHAR(40) DEFAULT NULL, " +
            COL_LONG_ID + " VARCHAR(16) DEFAULT NULL, " +
            COL_KEYWORDS + " VARCHAR(100) DEFAULT NULL, " +
            COL_LAST_USE + " INTEGER DEFAULT 0 " + ");";

    public static final String CREATE_INDEX_EMAIL_IN_CONTACT =
            "CREATE UNIQUE INDEX IF NOT EXISTS " + COL_EMAIL + "_in_" + TABLE_NAME_CONTACTS +
                    " ON " + TABLE_NAME_CONTACTS + " (" + COL_EMAIL + ")";

    public static final String CREATE_INDEX_NAME_IN_CONTACT =
            "CREATE INDEX IF NOT EXISTS " + COL_NAME + "_in_" + TABLE_NAME_CONTACTS +
                    " ON " + TABLE_NAME_CONTACTS + " (" + COL_NAME + ")";

    public static final String CREATE_INDEX_HAS_PGP_IN_CONTACT =
            "CREATE INDEX IF NOT EXISTS " + COL_HAS_PGP + "_in_" + TABLE_NAME_CONTACTS +
                    " ON " + TABLE_NAME_CONTACTS + " (" + COL_HAS_PGP + ")";

    public static final String CREATE_INDEX_LONG_ID_IN_CONTACT =
            "CREATE INDEX IF NOT EXISTS " + COL_LONG_ID + "_in_" + TABLE_NAME_CONTACTS +
                    " ON " + TABLE_NAME_CONTACTS + " (" + COL_LONG_ID + ")";

    public static final String CREATE_INDEX_LAST_USE_IN_CONTACT =
            "CREATE INDEX IF NOT EXISTS " + COL_LAST_USE + "_in_" + TABLE_NAME_CONTACTS +
                    " ON " + TABLE_NAME_CONTACTS + " (" + COL_LAST_USE + ")";

    @Override
    public String getTableName() {
        return TABLE_NAME_CONTACTS;
    }

    public Uri addRow(Context context, PgpContact pgpContact) {
        ContentResolver contentResolver = context.getContentResolver();
        if (pgpContact != null && contentResolver != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL_EMAIL, pgpContact.getEmail());
            contentValues.put(COL_NAME, pgpContact.getName());
            contentValues.put(COL_PUBLIC_KEY, pgpContact.getPubkey());
            contentValues.put(COL_HAS_PGP, pgpContact.getHasPgp());
            contentValues.put(COL_CLIENT, pgpContact.getClient());
            contentValues.put(COL_ATTESTED, pgpContact.getAttested());
            contentValues.put(COL_FINGERPRINT, pgpContact.getFingerprint());
            contentValues.put(COL_LONG_ID, pgpContact.getLongid());
            contentValues.put(COL_KEYWORDS, pgpContact.getKeywords());
            contentValues.put(COL_LAST_USE, pgpContact.getLastUse());

            return contentResolver.insert(getBaseContentUri(), contentValues);
        } else return null;
    }
}
