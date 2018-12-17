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
import android.text.TextUtils;

import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.database.dao.source.BaseDaoSource;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * @author Denis Bondarenko
 * Date: 08.08.2017
 * Time: 10:41
 * E-mail: DenBond7@gmail.com
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

  public static final String CREATE_INDEX_EMAIL_UID_FOLDER_IN_ATTACHMENT = INDEX_PREFIX + COL_EMAIL + "_" + COL_UID +
      "_" + COL_FOLDER + "_in_" + TABLE_NAME_ATTACHMENT + " ON " + TABLE_NAME_ATTACHMENT +
      " (" + COL_EMAIL + ", " + COL_UID + ", " + COL_FOLDER + ")";

  /**
   * Prepare the content values for insert to the database.
   *
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param uid     The message UID.
   * @param attInfo The attachment info which will be added to the database.
   * @return generated {@link ContentValues}
   */
  @NonNull
  public static ContentValues prepareContentValues(String email, String label, long uid,
                                                   AttachmentInfo attInfo) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(COL_EMAIL, email);
    contentValues.put(COL_FOLDER, label);
    contentValues.put(COL_UID, uid);
    contentValues.put(COL_NAME, attInfo.getName());
    contentValues.put(COL_ENCODED_SIZE_IN_BYTES, attInfo.getEncodedSize());
    contentValues.put(COL_TYPE, attInfo.getType());
    contentValues.put(COL_ATTACHMENT_ID, attInfo.getId());
    if (attInfo.getUri() != null) {
      contentValues.put(COL_FILE_URI, attInfo.getUri().toString());
    }
    contentValues.put(COL_FORWARDED_FOLDER, attInfo.getFwdFolder());
    contentValues.put(COL_FORWARDED_UID, attInfo.getFwdUid());
    return contentValues;
  }

  /**
   * Generate an {@link AttachmentInfo} object from the current cursor position.
   *
   * @param cursor The {@link Cursor} which contains information about an
   *               {@link AttachmentInfo} object.
   * @return A generated {@link AttachmentInfo}.
   */
  public static AttachmentInfo getAttInfo(Cursor cursor) {
    AttachmentInfo attInfo = new AttachmentInfo();
    attInfo.setEmail(cursor.getString(cursor.getColumnIndex(COL_EMAIL)));
    attInfo.setFolder(cursor.getString(cursor.getColumnIndex(COL_FOLDER)));
    attInfo.setUid(cursor.getInt(cursor.getColumnIndex(COL_UID)));
    attInfo.setName(cursor.getString(cursor.getColumnIndex(COL_NAME)));
    attInfo.setEncodedSize(cursor.getLong(cursor.getColumnIndex(COL_ENCODED_SIZE_IN_BYTES)));
    attInfo.setType(cursor.getString(cursor.getColumnIndex(COL_TYPE)));
    attInfo.setId(cursor.getString(cursor.getColumnIndex(COL_ATTACHMENT_ID)));
    String uriString = cursor.getString(cursor.getColumnIndex(COL_FILE_URI));
    if (!TextUtils.isEmpty(uriString)) {
      attInfo.setUri(Uri.parse(uriString));
    }
    attInfo.setFwdFolder(cursor.getString(cursor.getColumnIndex(COL_FORWARDED_FOLDER)));
    attInfo.setFwdUid(cursor.getInt(cursor.getColumnIndex(COL_FORWARDED_UID)));
    attInfo.setForwarded(!cursor.isNull(cursor.getColumnIndex(COL_FORWARDED_FOLDER))
        && cursor.getInt(cursor.getColumnIndex(COL_FORWARDED_UID)) > 0);
    return attInfo;
  }

  @Override
  public String getTableName() {
    return TABLE_NAME_ATTACHMENT;
  }

  /**
   * Add a new attachment details to the database.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label where exists message which contains a current
   *                attachment.
   * @param uid     The message UID.
   * @param attInfo The attachment details which will be added to the database.
   * @return A {@link Uri} of the created row.
   */
  public Uri addRow(Context context, String email, String label, long uid, AttachmentInfo attInfo) {
    ContentResolver contentResolver = context.getContentResolver();
    if (attInfo != null && label != null && contentResolver != null) {
      ContentValues contentValues = prepareContentValues(email, label, uid, attInfo);
      return contentResolver.insert(getBaseContentUri(), contentValues);
    } else return null;
  }

  /**
   * This method add rows per single transaction.
   *
   * @param context     Interface to global information about an application environment.
   * @param email       The email that the message linked.
   * @param label       The folder label where exists message which contains the current
   *                    attachments.
   * @param uid         The message UID.
   * @param attInfoList The attachments list.
   * @return the number of newly created rows.
   */
  public int addRows(Context context, String email, String label, long uid, List<AttachmentInfo> attInfoList) {
    if (attInfoList != null) {
      ContentResolver contentResolver = context.getContentResolver();
      ContentValues[] contentValuesArray = new ContentValues[attInfoList.size()];

      for (int i = 0; i < attInfoList.size(); i++) {
        AttachmentInfo attachmentInfo = attInfoList.get(i);
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
   * @param attId         The unique attachment id.
   * @param contentValues The {@link ContentValues} which contains new information.
   * @return The count of the updated row or -1 up.
   */
  public int update(Context context, String email, String label, long uid, String attId, ContentValues contentValues) {
    ContentResolver contentResolver = context.getContentResolver();
    if (email != null && label != null && contentResolver != null) {
      String where = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID + " = ? AND " + COL_ATTACHMENT_ID +
          " = ? ";
      String[] selectionArgs = new String[]{email, label, String.valueOf(uid), attId};
      return contentResolver.update(getBaseContentUri(), contentValues, where, selectionArgs);
    } else return -1;
  }

  /**
   * Get all {@link AttachmentInfo} objects from the database for some message.
   *
   * @param email The email that the message linked.
   * @param label The folder label.
   * @param uid   The message UID.
   * @return A  list of {@link AttachmentInfo} objects.
   */
  public ArrayList<AttachmentInfo> getAttInfoList(Context context, String email, String label, long uid) {
    ContentResolver contentResolver = context.getContentResolver();
    String selection = COL_EMAIL + " = ?" + " AND " + COL_FOLDER + " = ?" + " AND " + COL_UID + " = ?";
    String[] selectionArgs = new String[]{email, label, String.valueOf(uid)};
    Cursor cursor = contentResolver.query(getBaseContentUri(), null, selection, selectionArgs, null);

    ArrayList<AttachmentInfo> attInfoList = new ArrayList<>();

    if (cursor != null) {
      while (cursor.moveToNext()) {
        attInfoList.add(getAttInfo(cursor));
      }
      cursor.close();
    }

    return attInfoList;
  }

  /**
   * Delete cached attachments info for some email and some folder.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @return The number of deleted rows.
   */
  public int deleteCachedAttInfo(Context context, String email, String label) {
    ContentResolver contentResolver = context.getContentResolver();
    if (email != null && label != null && contentResolver != null) {
      String where = COL_EMAIL + " = ? AND " + COL_FOLDER + " = ?";
      return contentResolver.delete(getBaseContentUri(), where, new String[]{email, label});
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
  public int deleteAtts(Context context, String email, String label, long uid) {
    ContentResolver contentResolver = context.getContentResolver();
    if (email != null && label != null && contentResolver != null) {
      String where = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID + " = ? ";
      return contentResolver.delete(getBaseContentUri(), where, new String[]{email, label, String.valueOf(uid)});
    } else return -1;
  }
}
