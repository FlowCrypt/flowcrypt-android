/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source.imap;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.database.dao.source.BaseDaoSource;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Denis Bondarenko
 *         Date: 08.08.2017
 *         Time: 10:41
 *         E-mail: DenBond7@gmail.com
 */

public class AttachmentDaoSource extends BaseDaoSource {
    public static final String TABLE_NAME_ATTACHMENT = "attachment";

    public static final String COL_EMAIL = "email";
    public static final String COL_FOLDER = "folder";
    public static final String COL_UID = "uid";
    public static final String COL_NAME = "name";
    public static final String COL_ENCODED_SIZE_IN_BYTES = "encodedSize";
    public static final String COL_TYPE = "type";
    public static final String COL_ATTACHMENT_ID = "attachment_id";
    public static final String COL_FILE_URI = "file_uri";
    public static final String COL_FORWARDED_FOLDER = "forwarded_folder";
    public static final String COL_FORWARDED_UID = "forwarded_uid";

    public static final String ATTACHMENT_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
            TABLE_NAME_ATTACHMENT + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_EMAIL + " VARCHAR(100) NOT NULL, " +
            COL_FOLDER + " TEXT NOT NULL, " +
            COL_UID + " INTEGER NOT NULL, " +
            COL_NAME + " TEXT NOT NULL, " +
            COL_ENCODED_SIZE_IN_BYTES + " INTEGER DEFAULT 0, " +
            COL_TYPE + " VARCHAR(100) NOT NULL, " +
            COL_ATTACHMENT_ID + " TEXT NOT NULL, " +
            COL_FILE_URI + " TEXT, " +
            COL_FORWARDED_FOLDER + " TEXT, " +
            COL_FORWARDED_UID + " INTEGER DEFAULT -1 " + ");";

    public static final String CREATE_INDEX_EMAIL_UID_FOLDER_IN_ATTACHMENT =
            "CREATE INDEX IF NOT EXISTS " + COL_EMAIL + "_" + COL_UID + "_" + COL_FOLDER
                    + "_in_" + TABLE_NAME_ATTACHMENT + " ON " + TABLE_NAME_ATTACHMENT +
                    " (" + COL_EMAIL + ", " + COL_UID + ", " + COL_FOLDER + ")";

    /**
     * Prepare the content values for insert to the database.
     *
     * @param email          The email that the message linked.
     * @param label          The folder label.
     * @param uid            The message UID.
     * @param attachmentInfo The attachment info which will be added to the database.
     * @return generated {@link ContentValues}
     */
    @NonNull
    public static ContentValues prepareContentValues(String email, String label, long uid,
                                                     AttachmentInfo attachmentInfo) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_EMAIL, email);
        contentValues.put(COL_FOLDER, label);
        contentValues.put(COL_UID, uid);
        contentValues.put(COL_NAME, attachmentInfo.getName());
        contentValues.put(COL_ENCODED_SIZE_IN_BYTES, attachmentInfo.getEncodedSize());
        contentValues.put(COL_TYPE, attachmentInfo.getType());
        contentValues.put(COL_ATTACHMENT_ID, attachmentInfo.getId());
        if (attachmentInfo.getUri() != null) {
            contentValues.put(COL_FILE_URI, attachmentInfo.getUri().toString());
        }
        contentValues.put(COL_FORWARDED_FOLDER, attachmentInfo.getForwardedFolder());
        contentValues.put(COL_FORWARDED_UID, attachmentInfo.getForwardedUid());
        return contentValues;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME_ATTACHMENT;
    }

    /**
     * Add a new attachment details to the database.
     *
     * @param context        Interface to global information about an application environment.
     * @param email          The email that the message linked.
     * @param label          The folder label where exists message which contains a current
     *                       attachment.
     * @param uid            The message UID.
     * @param attachmentInfo The attachment details which will be added to the database.
     * @return A {@link Uri} of the created row.
     */
    public Uri addRow(Context context, String email, String label, long uid,
                      AttachmentInfo attachmentInfo) {
        ContentResolver contentResolver = context.getContentResolver();
        if (attachmentInfo != null && label != null && contentResolver != null) {
            ContentValues contentValues = prepareContentValues(email, label, uid, attachmentInfo);
            return contentResolver.insert(getBaseContentUri(), contentValues);
        } else return null;
    }

    /**
     * This method add rows per single transaction.
     *
     * @param context            Interface to global information about an application environment.
     * @param email              The email that the message linked.
     * @param label              The folder label where exists message which contains the current
     *                           attachments.
     * @param uid                The message UID.
     * @param attachmentInfoList The attachments list.
     * @return the number of newly created rows.
     */
    public int addRows(Context context, String email, String label, long uid,
                       List<AttachmentInfo> attachmentInfoList) {
        if (attachmentInfoList != null) {
            ContentResolver contentResolver = context.getContentResolver();
            ContentValues[] contentValuesArray = new ContentValues[attachmentInfoList.size()];

            for (int i = 0; i < attachmentInfoList.size(); i++) {
                AttachmentInfo attachmentInfo = attachmentInfoList.get(i);
                ContentValues contentValues = prepareContentValues(email, label, uid, attachmentInfo);

                contentValuesArray[i] = contentValues;
            }

            return contentResolver.bulkInsert(getBaseContentUri(), contentValuesArray);
        } else return 0;
    }

    /**
     * This method add rows per single transaction.
     *
     * @param context       Interface to global information about an application environment.
     * @param contentValues The array of prepared {@link ContentValues}.
     * @return the number of newly created rows.
     */
    public int addRows(Context context, ContentValues[] contentValues) {
        if (contentValues != null) {
            ContentResolver contentResolver = context.getContentResolver();
            return contentResolver.bulkInsert(getBaseContentUri(), contentValues);
        } else return 0;
    }

    /**
     * Update some attachment by the given parameters.
     *
     * @param context       Interface to global information about an application environment.
     * @param email         The email that the attachment linked.
     * @param label         The folder that the attachment linked.
     * @param uid           The message UID that the attachment linked.
     * @param attachmentId  The unique attachment id.
     * @param contentValues The {@link ContentValues} which contains new information.
     * @return The count of the updated row or -1 up.
     */
    public int update(Context context, String email, String label, long uid, String attachmentId,
                      ContentValues contentValues) {
        ContentResolver contentResolver = context.getContentResolver();
        if (email != null && label != null && contentResolver != null) {
            return contentResolver.update(getBaseContentUri(), contentValues,
                    COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID + " = ? AND "
                            + COL_ATTACHMENT_ID + " = ? ", new String[]{email, label, String.valueOf(uid),
                            attachmentId});
        } else return -1;
    }

    /**
     * Generate an {@link AttachmentInfo} object from the current cursor position.
     *
     * @param cursor The {@link Cursor} which contains information about an
     *               {@link AttachmentInfo} object.
     * @return A generated {@link AttachmentInfo}.
     */
    public AttachmentInfo getAttachmentInfo(Cursor cursor) {
        AttachmentInfo attachmentInfo = new AttachmentInfo();
        attachmentInfo.setEmail(cursor.getString(cursor.getColumnIndex(COL_EMAIL)));
        attachmentInfo.setFolder(cursor.getString(cursor.getColumnIndex(COL_FOLDER)));
        attachmentInfo.setUid(cursor.getInt(cursor.getColumnIndex(COL_UID)));
        attachmentInfo.setName(cursor.getString(cursor.getColumnIndex(COL_NAME)));
        attachmentInfo.setEncodedSize(cursor.getLong(cursor.getColumnIndex(COL_ENCODED_SIZE_IN_BYTES)));
        attachmentInfo.setType(cursor.getString(cursor.getColumnIndex(COL_TYPE)));
        attachmentInfo.setId(cursor.getString(cursor.getColumnIndex(COL_ATTACHMENT_ID)));
        String uriString = cursor.getString(cursor.getColumnIndex(COL_FILE_URI));
        if (!TextUtils.isEmpty(uriString)) {
            attachmentInfo.setUri(Uri.parse(uriString));
        }
        attachmentInfo.setForwardedFolder(cursor.getString(cursor.getColumnIndex(COL_FORWARDED_FOLDER)));
        attachmentInfo.setForwardedUid(cursor.getInt(cursor.getColumnIndex(COL_FORWARDED_UID)));
        attachmentInfo.setForwarded(!cursor.isNull(cursor.getColumnIndex(COL_FORWARDED_FOLDER))
                && cursor.getInt(cursor.getColumnIndex(COL_FORWARDED_UID)) > 0);
        return attachmentInfo;
    }

    /**
     * Get all {@link AttachmentInfo} objects from the database for some message.
     *
     * @param email The email that the message linked.
     * @param label The folder label.
     * @param uid   The message UID.
     * @return A  list of {@link AttachmentInfo} objects.
     */
    public ArrayList<AttachmentInfo> getAttachmentInfoList(Context context, String email,
                                                           String label, long uid) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(getBaseContentUri(),
                null, COL_EMAIL + " = ?" + " AND " + COL_FOLDER + " = ?" + " AND " + COL_UID +
                        " = ?", new String[]{email, label, String.valueOf(uid)}, null);

        ArrayList<AttachmentInfo> attachmentInfoList = new ArrayList<>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                attachmentInfoList.add(getAttachmentInfo(cursor));
            }
            cursor.close();
        }

        return attachmentInfoList;
    }

    /**
     * Delete cached attachments info for some email and some folder.
     *
     * @param context Interface to global information about an application environment.
     * @param email   The email that the message linked.
     * @param label   The folder label.
     * @return The number of deleted rows.
     */
    public int deleteCachedAttachmentInfoOfFolder(Context context, String email, String label) {
        ContentResolver contentResolver = context.getContentResolver();
        if (email != null && label != null && contentResolver != null) {
            return contentResolver.delete(getBaseContentUri(), COL_EMAIL + " = ? AND "
                    + COL_FOLDER + " = ?", new String[]{email, label});
        } else return -1;
    }

    /**
     * Delete attachments of some message in the local database.
     *
     * @param context Interface to global information about an application environment.
     * @param email   The email that the message linked.
     * @param label   The folder label.
     * @param uid     The message UID.
     * @return The number of rows deleted.
     */
    public int deleteAttachments(Context context, String email, String label, long uid) {
        ContentResolver contentResolver = context.getContentResolver();
        if (email != null && label != null && contentResolver != null) {
            return contentResolver.delete(getBaseContentUri(), COL_EMAIL + "= ? AND "
                    + COL_FOLDER + " = ? AND "
                    + COL_UID + " = ? ", new String[]{email, label, String.valueOf(uid)});
        } else return -1;
    }
}
