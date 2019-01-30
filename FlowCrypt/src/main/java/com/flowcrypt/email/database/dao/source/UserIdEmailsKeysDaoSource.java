/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Pair;

import com.flowcrypt.email.js.PgpKey;
import com.google.android.gms.common.util.CollectionUtils;

import java.util.ArrayList;
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

  public static final String INDEX_LONG_ID_USER_ID_EMAIL = UNIQUE_INDEX_PREFIX + COL_LONG_ID + "_" +
      COL_USER_ID_EMAIL + "_in_" + TABLE_NAME_USER_ID_EMAILS_AND_KEYS + " ON " + TABLE_NAME_USER_ID_EMAILS_AND_KEYS
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
      contentValues.put(COL_USER_ID_EMAIL, email.toLowerCase());
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
    if (!CollectionUtils.isEmpty(pairs)) {
      ContentResolver contentResolver = context.getContentResolver();
      ContentValues[] contentValuesArray = new ContentValues[pairs.size()];

      for (int i = 0; i < pairs.size(); i++) {
        Pair<String, String> pair = pairs.get(i);
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_LONG_ID, pair.first);
        contentValues.put(COL_USER_ID_EMAIL, pair.second.toLowerCase());
        contentValuesArray[i] = contentValues;
      }

      return contentResolver.bulkInsert(getBaseContentUri(), contentValuesArray);
    } else return 0;
  }

  /**
   * Delete information about a private {@link PgpKey}.
   *
   * @param context Interface to global information about an application environment.
   * @param pgpKey  The object which contains information about the private {@link PgpKey}.
   * @return The count of deleted rows. Will be 1 if information about {@link PgpKey} was deleted or -1 otherwise.
   */
  public int removeKey(Context context, PgpKey pgpKey) {
    if (pgpKey != null) {

      ContentResolver contentResolver = context.getContentResolver();
      if (contentResolver != null) {
        return contentResolver.delete(getBaseContentUri(), COL_LONG_ID + " = ?", new String[]{pgpKey.getLongid()});
      } else return -1;
    } else return -1;
  }

  /**
   * Get a list of longId using a given email.
   *
   * @param context Interface to global information about an application environment.
   * @param email   An email which will be used for searching.
   * @return A list of found longId.
   */
  public List<String> getLongIdsByEmail(Context context, String email) {
    List<String> longIdsList = new ArrayList<>();
    if (!TextUtils.isEmpty(email)) {
      String selection = COL_USER_ID_EMAIL + " = ?";
      String[] selectionArgs = new String[]{email.toLowerCase()};
      Cursor cursor = context.getContentResolver().query(getBaseContentUri(), null, selection, selectionArgs, null);

      if (cursor != null) {
        while (cursor.moveToNext()) {
          longIdsList.add(cursor.getString(cursor.getColumnIndex(COL_LONG_ID)));
        }
      }

      if (cursor != null) {
        cursor.close();
      }
    }

    return longIdsList;
  }
}
