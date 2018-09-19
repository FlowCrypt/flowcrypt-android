/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source.imap;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.database.dao.source.BaseDaoSource;

import java.util.ArrayList;
import java.util.Collection;
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
    public static final String COL_MESSAGE_COUNT = "message_count";
    public static final String COL_IS_CUSTOM_LABEL = "is_custom_label";
    public static final String COL_FOLDER_ATTRIBUTES = "folder_attributes";
    public static final String COL_FOLDER_MESSAGE_COUNT = "folder_message_count";

    public static final String IMAP_LABELS_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
            TABLE_NAME_IMAP_LABELS + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_EMAIL + " VARCHAR(100) NOT NULL, " +
            COL_FOLDER_NAME + " VARCHAR(255) NOT NULL, " +
            COL_IS_CUSTOM_LABEL + " INTEGER DEFAULT 0, " +
            COL_FOLDER_ALIAS + " VARCHAR(100) DEFAULT NULL, " +
            COL_MESSAGE_COUNT + " INTEGER DEFAULT 0, " +
            COL_FOLDER_ATTRIBUTES + " TEXT NOT NULL, " +
            COL_FOLDER_MESSAGE_COUNT + " INTEGER DEFAULT 0 " + ");";

    @Override
    public String getTableName() {
        return TABLE_NAME_IMAP_LABELS;
    }

    /**
     * @param context     Interface to global information about an application environment.
     * @param accountName The account name which are an owner of the folder.
     * @param folder      The {@link Folder} object which contains information about
     *                    {@link com.sun.mail.imap.IMAPFolder}.
     * @return A {@link Uri} of the created row.
     */
    public Uri addRow(Context context, String accountName, Folder folder) {
        ContentResolver contentResolver = context.getContentResolver();
        if (!TextUtils.isEmpty(accountName) && folder != null && contentResolver != null) {
            ContentValues contentValues = prepareContentValues(accountName, folder);
            return contentResolver.insert(getBaseContentUri(), contentValues);
        } else return null;
    }

    /**
     * Add information about folders to local the database.
     *
     * @param context     Interface to global information about an application environment.
     * @param accountName The account name which are an owner of the folder.
     * @param folders     The folders array.
     * @return @return the number of newly created rows.
     */
    public int addRows(Context context, String accountName, Collection<Folder> folders) {
        if (folders != null) {
            ContentResolver contentResolver = context.getContentResolver();
            ContentValues[] contentValuesArray = new ContentValues[folders.size()];

            Folder[] foldersArray = folders.toArray(new Folder[0]);

            for (int i = 0; i < folders.size(); i++) {
                Folder folder = foldersArray[i];
                ContentValues contentValues = prepareContentValues(accountName, folder);
                contentValuesArray[i] = contentValues;
            }

            return contentResolver.bulkInsert(getBaseContentUri(), contentValuesArray);
        } else return 0;
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
                cursor.getInt(cursor.getColumnIndex(COL_MESSAGE_COUNT)),
                parseAttributes(cursor.getString(cursor.getColumnIndex(COL_FOLDER_ATTRIBUTES))),
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
     * Get a {@link Folder} from the database by an email and an alias.
     *
     * @param email       The email of the {@link Folder}.
     * @param folderAlias The folder alias.
     * @return {@link Folder} or null if such folder not found.
     */
    public Folder getFolderByAlias(Context context, String email, String folderAlias) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(getBaseContentUri(), null, COL_EMAIL + " = ?" + " AND " +
                COL_FOLDER_ALIAS + " = ?", new String[]{email, folderAlias}, null);

        Folder folder = null;

        if (cursor != null) {
            while (cursor.moveToNext()) {
                folder = getFolder(cursor);
            }
            cursor.close();
        }

        return folder;
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
            return contentResolver.delete(getBaseContentUri(), COL_EMAIL + " = ?",
                    new String[]{email});
        } else return -1;
    }

    /**
     * Update a message count of some {@link Folder}.
     *
     * @param context         Interface to global information about an application environment.
     * @param email           The account email.
     * @param folderName      A server folder name. Links to {@link #COL_FOLDER_NAME}
     * @param newMessageCount A new message count.
     * @return The count of updated rows. Will be 1 if information about {@link Folder} was
     * updated or -1 otherwise.
     */
    public int updateLabelMessageCount(Context context, String email, String folderName, int newMessageCount) {
        if (context != null && !TextUtils.isEmpty(folderName)) {
            ContentResolver contentResolver = context.getContentResolver();
            if (contentResolver != null) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(COL_MESSAGE_COUNT, newMessageCount);
                return contentResolver.update(getBaseContentUri(), contentValues, COL_EMAIL + "= ? AND " +
                        COL_FOLDER_NAME + " = ? ", new String[]{email, folderName});
            } else return -1;
        } else return -1;
    }

    /**
     * This method update the local labels info. Here we will remove deleted and create new folders.
     *
     * @param context    Interface to global information about an application environment.
     * @param email      The account email.
     * @param oldFolders The list of old {@link Folder} object.
     * @param newFolders The list of new {@link Folder} object.
     * @return the {@link ContentProviderResult} array.
     */
    public ContentProviderResult[] updateLabels(Context context, String email,
                                                Collection<Folder> oldFolders, Collection<Folder> newFolders)
            throws RemoteException, OperationApplicationException {
        ContentResolver contentResolver = context.getContentResolver();
        if (email != null && contentResolver != null) {

            ArrayList<ContentProviderOperation> contentProviderOperations = new ArrayList<>();

            List<Folder> deleteCandidates = new ArrayList<>();
            for (Folder oldFolder : oldFolders) {
                boolean isFolderFound = false;
                for (Folder newFolder : newFolders) {
                    if (newFolder.getServerFullFolderName().equals(oldFolder.getServerFullFolderName())) {
                        isFolderFound = true;
                        break;
                    }
                }

                if (!isFolderFound) {
                    deleteCandidates.add(oldFolder);
                }
            }

            List<Folder> newCandidates = new ArrayList<>();
            for (Folder newFolder : newFolders) {
                boolean isFolderFound = false;
                for (Folder oldFolder : oldFolders) {
                    if (oldFolder.getServerFullFolderName().equals(newFolder.getServerFullFolderName())) {
                        isFolderFound = true;
                        break;
                    }
                }

                if (!isFolderFound) {
                    newCandidates.add(newFolder);
                }
            }

            for (Folder folder : deleteCandidates) {
                contentProviderOperations.add(ContentProviderOperation.newDelete(getBaseContentUri())
                        .withSelection(COL_EMAIL + "= ? AND " + COL_FOLDER_NAME + " = ? ",
                                new String[]{email, folder.getServerFullFolderName()})
                        .withYieldAllowed(true)
                        .build());
            }

            for (Folder folder : newCandidates) {
                contentProviderOperations.add(ContentProviderOperation.newInsert(getBaseContentUri())
                        .withValues(prepareContentValues(email, folder))
                        .withYieldAllowed(true)
                        .build());
            }
            return contentResolver.applyBatch(getBaseContentUri().getAuthority(), contentProviderOperations);
        } else return new ContentProviderResult[0];
    }

    @NonNull
    private ContentValues prepareContentValues(String accountName, Folder folder) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_EMAIL, accountName);
        contentValues.put(COL_FOLDER_NAME, folder.getServerFullFolderName());
        contentValues.put(COL_FOLDER_ALIAS, folder.getFolderAlias());
        contentValues.put(COL_MESSAGE_COUNT, folder.getMessageCount());
        contentValues.put(COL_IS_CUSTOM_LABEL, folder.isCustomLabel());
        contentValues.put(COL_FOLDER_ATTRIBUTES,
                prepareAttributesToSaving(folder.getAttributes()));
        return contentValues;
    }

    private String prepareAttributesToSaving(String[] attributes) {
        if (attributes != null && attributes.length > 0) {
            StringBuilder result = new StringBuilder();
            for (String attribute : attributes) {
                result.append(attribute).append("\t");
            }

            return result.toString();
        } else {
            return "";
        }
    }

    private String[] parseAttributes(String attributesAsString) {
        if (attributesAsString != null && attributesAsString.length() > 0) {
            return attributesAsString.split("\t");
        } else {
            return null;
        }
    }
}
