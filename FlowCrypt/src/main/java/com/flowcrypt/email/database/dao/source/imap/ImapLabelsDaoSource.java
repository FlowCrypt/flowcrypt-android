/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source.imap;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.database.dao.source.BaseDaoSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class describes the structure of IMAP labels for different accounts and methods which
 * will be used to manipulate this data.
 *
 * @author DenBond7
 *         Date: 14.06.2017
 *         Time: 15:59
 *         E-mail: DenBond7@gmail.com
 */

public class ImapLabelsDaoSource extends BaseDaoSource {
    public static final String TABLE_NAME_IMAP_LABELS = "imap_labels";

    public static final String COL_EMAIL = "email";
    public static final String COL_FOLDER_NAME = "folder_name";
    public static final String COL_FOLDER_ALIAS = "folder_alias";
    public static final String COL_IS_CUSTOM_LABEL = "is_custom_label";
    public static final String COL_FOLDER_ATTRIBUTES = "folder_attributes";
    public static final String COД_FOLDER_MESSAGE_COUNT = "folder_message_count";

    public static final String IMAP_LABELS_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
            TABLE_NAME_IMAP_LABELS + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_EMAIL + " VARCHAR(100) NOT NULL, " +
            COL_FOLDER_NAME + " VARCHAR(255) NOT NULL, " +
            COL_IS_CUSTOM_LABEL + " INTEGER DEFAULT 0, " +
            COL_FOLDER_ALIAS + " VARCHAR(100) DEFAULT NULL, " +
            COL_FOLDER_ATTRIBUTES + " TEXT NOT NULL, " +
            COД_FOLDER_MESSAGE_COUNT + " INTEGER DEFAULT 0 " + ");";

    @Override
    public String getTableName() {
        return TABLE_NAME_IMAP_LABELS;
    }

    /**
     * @param context     Interface to global information about an application environment.
     * @param accountName The account name which are an owner of the folder.
     * @param folder      The {@link Folder} object which contains an information about
     *                    {@link com.sun.mail.imap.IMAPFolder}.
     * @return A {@link Uri} of the created row.
     */
    public Uri addRow(Context context, String accountName, Folder folder) {
        ContentResolver contentResolver = context.getContentResolver();
        if (!TextUtils.isEmpty(accountName) && folder != null && contentResolver != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL_EMAIL, accountName);
            contentValues.put(COL_FOLDER_NAME, folder.getServerFullFolderName());
            contentValues.put(COL_FOLDER_ALIAS, folder.getFolderAlias());
            contentValues.put(COL_IS_CUSTOM_LABEL, folder.isCustomLabel());
//            contentValues.put(COL_FOLDER_ATTRIBUTES, Arrays.toString(folder.getAttributes()));
            contentValues.put(COL_FOLDER_ATTRIBUTES, "");

            return contentResolver.insert(getBaseContentUri(), contentValues);
        } else return null;
    }

    /**
     * Generate a {@link Folder} object from the current cursor position.
     *
     * @param cursor The {@link Cursor} which contains information about {@link Folder}.
     * @return A generated {@link Folder}.
     */
    public Folder getFolder(Cursor cursor) {
        return new Folder(
                cursor.getString(cursor.getColumnIndex(COL_FOLDER_NAME)),
                cursor.getString(cursor.getColumnIndex(COL_FOLDER_ALIAS)),
                cursor.getInt(cursor.getColumnIndex(COL_IS_CUSTOM_LABEL)) == 1
        );
    }

    /**
     * Get all {@link Folder} objects from the database by an email.
     *
     * @param email The email of the {@link Folder}.
     * @return A  list of {@link Folder} objects.
     */
    public List<Folder> getFolders(Context context, String email) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(getBaseContentUri(),
                null, COL_EMAIL + " = ?", new String[]{email}, null);

        List<Folder> folders = new ArrayList<>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                folders.add(getFolder(cursor));
            }
            cursor.close();
        }

        return folders;
    }

    /**
     * Delete all folders of some email.
     *
     * @param context Interface to global information about an application environment.
     * @param email   The email of the {@link Folder}.
     * @return The count of deleted rows. Will be >1 if a folder(s) was deleted or -1 otherwise.
     */
    public int deleteFolders(Context context, String email) {
        ContentResolver contentResolver = context.getContentResolver();
        if (contentResolver != null) {
            return contentResolver.delete(getBaseContentUri(),
                    COL_EMAIL + " = ?", new String[]{email});
        } else return -1;
    }
}
