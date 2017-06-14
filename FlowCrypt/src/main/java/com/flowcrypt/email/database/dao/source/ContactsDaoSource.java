/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.flowcrypt.email.test.PgpContact;

import java.util.ArrayList;
import java.util.List;

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
    public static final String CLIENT_FLOWCRYPT = "flowcrypt";
    public static final String CLIENT_PGP = "pgp";

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
            COL_NAME + " VARCHAR(50) DEFAULT NULL, " +
            COL_PUBLIC_KEY + " BLOB DEFAULT NULL, " +
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
            contentValues.put(COL_EMAIL, pgpContact.getEmail().toLowerCase());
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

    /**
     * Generate a {@link PgpContact} object from the current cursor position.
     *
     * @param cursor The {@link Cursor} which contains information about {@link PgpContact}.
     * @return A generated {@link PgpContact}.
     */
    public PgpContact getCurrentPgpContact(Cursor cursor) {
        return new PgpContact(
                cursor.getString(cursor.getColumnIndex(COL_EMAIL)),
                cursor.getString(cursor.getColumnIndex(COL_NAME)),
                cursor.getString(cursor.getColumnIndex(COL_PUBLIC_KEY)),
                cursor.getInt(cursor.getColumnIndex(COL_HAS_PGP)) == 1,
                cursor.getString(cursor.getColumnIndex(COL_CLIENT)),
                cursor.getInt(cursor.getColumnIndex(COL_ATTESTED)) == 1,
                cursor.getString(cursor.getColumnIndex(COL_FINGERPRINT)),
                cursor.getString(cursor.getColumnIndex(COL_LONG_ID)),
                cursor.getString(cursor.getColumnIndex(COL_KEYWORDS)),
                cursor.getInt(cursor.getColumnIndex(COL_LAST_USE))
        );
    }

    /**
     * Get a {@link PgpContact} object from the database by an email.
     *
     * @param email The email of the {@link PgpContact}.
     * @return A {@link PgpContact} object.
     */
    public PgpContact getPgpContact(Context context, String email) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(getBaseContentUri(),
                null, COL_EMAIL + " = ?", new String[]{email}, null);

        PgpContact pgpContact = null;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                pgpContact = getCurrentPgpContact(cursor);
            }
            cursor.close();
        }

        return pgpContact;
    }

    /**
     * Get a list of {@link PgpContact} objects from the local database.
     *
     * @param context Interface to global information about an application environment.
     * @param emails  A list of emails.
     * @return <tt>List<PgpContact></tt> Return a list of existed(created) {@link PgpContact}
     * objects from the search by emails.
     */
    public List<PgpContact> getPgpContactsListFromDatabase(Context context, List<String> emails) {
        Cursor cursor = context.getContentResolver().query(
                getBaseContentUri(), null, ContactsDaoSource.COL_EMAIL +
                        " IN " + prepareSelection(emails), emails.toArray(new String[0]), null);

        List<PgpContact> pgpContacts = new ArrayList<>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                pgpContacts.add(getCurrentPgpContact(cursor));
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return pgpContacts;
    }

    /**
     * Update an information about some {@link PgpContact}.
     *
     * @param context    Interface to global information about an application environment.
     * @param pgpContact A new information of {@link PgpContact} in the database.
     * @return The count of updated rows. Will be 1 if information about {@link PgpContact} was
     * updated or -1 otherwise.
     */
    public int updatePgpContact(Context context, PgpContact pgpContact) {
        ContentResolver contentResolver = context.getContentResolver();
        if (pgpContact != null && contentResolver != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL_NAME, pgpContact.getName());
            contentValues.put(COL_PUBLIC_KEY, pgpContact.getPubkey());
            contentValues.put(COL_HAS_PGP, pgpContact.getHasPgp());
            contentValues.put(COL_CLIENT, pgpContact.getClient());
            contentValues.put(COL_ATTESTED, pgpContact.getAttested());
            contentValues.put(COL_FINGERPRINT, pgpContact.getFingerprint());
            contentValues.put(COL_LONG_ID, pgpContact.getLongid());
            contentValues.put(COL_KEYWORDS, pgpContact.getKeywords());

            return contentResolver.update(getBaseContentUri(),
                    contentValues,
                    COL_EMAIL + " = ?",
                    new String[]{pgpContact.getEmail()});
        } else return -1;
    }

    /**
     * Update a last use entry of {@link PgpContact}.
     *
     * @param context    Interface to global information about an application environment.
     * @param pgpContact A new information of {@link PgpContact} in the database.
     * @return The count of updated rows. Will be 1 if information about {@link PgpContact} was
     * updated or -1 otherwise.
     */
    public int updateLastUseOfPgpContact(Context context, PgpContact pgpContact) {
        ContentResolver contentResolver = context.getContentResolver();
        if (pgpContact != null && contentResolver != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL_LAST_USE, System.currentTimeMillis());

            return contentResolver.update(getBaseContentUri(),
                    contentValues,
                    COL_EMAIL + " = ?",
                    new String[]{pgpContact.getEmail()});
        } else return -1;
    }

    /**
     * Update a name of the email entry in the database.
     *
     * @param context Interface to global information about an application environment.
     * @param email   An email in the database.
     * @param name    A new information about name of the email.
     * @return The count of updated rows. Will be 1 if an information was updated or -1 otherwise.
     */
    public int updateNameOfPgpContact(Context context, String email, String name) {
        ContentResolver contentResolver = context.getContentResolver();
        if (contentResolver != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL_NAME, name);

            return contentResolver.update(getBaseContentUri(),
                    contentValues,
                    COL_EMAIL + " = ?",
                    new String[]{email});
        } else return -1;
    }

    /**
     * Delete a {@link PgpContact} object from the database by an email.
     *
     * @param context Interface to global information about an application environment.
     * @param email   The email of the {@link PgpContact}.
     * @return The count of deleted rows. Will be 1 if a contact was deleted or -1 otherwise.
     */
    public int deletePgpContact(Context context, String email) {
        ContentResolver contentResolver = context.getContentResolver();
        if (contentResolver != null) {
            return contentResolver.delete(getBaseContentUri(),
                    COL_EMAIL + " = ?", new String[]{email});
        } else return -1;
    }
}
