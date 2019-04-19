/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import android.text.TextUtils;

import com.flowcrypt.email.api.email.LocalFolder;
import com.flowcrypt.email.database.dao.source.BaseDaoSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * This class describes the structure of IMAP labels for different accounts and methods which
 * will be used to manipulate this data.
 *
 * @author DenBond7
 * Date: 14.06.2017
 * Time: 15:59
 * E-mail: DenBond7@gmail.com
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
   * @param localFolder The {@link LocalFolder} object which contains information about
   *                    {@link com.sun.mail.imap.IMAPFolder}.
   * @return A {@link Uri} of the created row.
   */
  public Uri addRow(Context context, String accountName, LocalFolder localFolder) {
    ContentResolver contentResolver = context.getContentResolver();
    if (!TextUtils.isEmpty(accountName) && localFolder != null && contentResolver != null) {
      ContentValues contentValues = prepareContentValues(accountName, localFolder);
      return contentResolver.insert(getBaseContentUri(), contentValues);
    } else return null;
  }

  /**
   * Add information about folders to local the database.
   *
   * @param context      Interface to global information about an application environment.
   * @param accountName  The account name which are an owner of the folder.
   * @param localFolders The folders array.
   * @return @return the number of newly created rows.
   */
  public int addRows(Context context, String accountName, Collection<LocalFolder> localFolders) {
    if (localFolders != null) {
      ContentResolver contentResolver = context.getContentResolver();
      ContentValues[] contentValuesArray = new ContentValues[localFolders.size()];

      LocalFolder[] foldersArray = localFolders.toArray(new LocalFolder[0]);

      for (int i = 0; i < localFolders.size(); i++) {
        LocalFolder localFolder = foldersArray[i];
        ContentValues contentValues = prepareContentValues(accountName, localFolder);
        contentValuesArray[i] = contentValues;
      }

      return contentResolver.bulkInsert(getBaseContentUri(), contentValuesArray);
    } else return 0;
  }

  /**
   * Get all {@link LocalFolder} objects from the database by an email.
   *
   * @param email The email of the {@link LocalFolder}.
   * @return A  list of {@link LocalFolder} objects.
   */
  public List<LocalFolder> getFolders(Context context, String email) {
    ContentResolver contentResolver = context.getContentResolver();
    Cursor cursor = contentResolver.query(getBaseContentUri(),
        null, COL_EMAIL + " = ?", new String[]{email}, null);

    List<LocalFolder> localFolders = new ArrayList<>();

    if (cursor != null) {
      while (cursor.moveToNext()) {
        localFolders.add(getFolder(cursor));
      }
      cursor.close();
    }

    return localFolders;
  }

  /**
   * Generate a {@link LocalFolder} object from the current cursor position.
   *
   * @param cursor The {@link Cursor} which contains information about {@link LocalFolder}.
   * @return A generated {@link LocalFolder}.
   */
  public LocalFolder getFolder(Cursor cursor) {
    return new LocalFolder(
        cursor.getString(cursor.getColumnIndex(COL_FOLDER_NAME)),
        cursor.getString(cursor.getColumnIndex(COL_FOLDER_ALIAS)),
        cursor.getInt(cursor.getColumnIndex(COL_MESSAGE_COUNT)),
        parseAttributes(cursor.getString(cursor.getColumnIndex(COL_FOLDER_ATTRIBUTES))),
        cursor.getInt(cursor.getColumnIndex(COL_IS_CUSTOM_LABEL)) == 1
    );
  }

  /**
   * Get a {@link LocalFolder} from the database by an email and a name.
   *
   * @param email      The email of the {@link LocalFolder}.
   * @param folderName The folder name.
   * @return {@link LocalFolder} or null if such folder not found.
   */
  public LocalFolder getFolder(Context context, String email, String folderName) {
    ContentResolver contentResolver = context.getContentResolver();
    Cursor cursor = contentResolver.query(getBaseContentUri(), null, COL_EMAIL + " = ?" + " AND " +
        COL_FOLDER_NAME + " = ?", new String[]{email, folderName}, null);

    LocalFolder localFolder = null;

    if (cursor != null) {
      while (cursor.moveToNext()) {
        localFolder = getFolder(cursor);
      }
      cursor.close();
    }

    return localFolder;
  }

  /**
   * Delete all folders of some email.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email of the {@link LocalFolder}.
   * @return The count of deleted rows. Will be >1 if a folder(s) was deleted or -1 otherwise.
   */
  public int deleteFolders(Context context, String email) {
    ContentResolver contentResolver = context.getContentResolver();
    if (contentResolver != null) {
      return contentResolver.delete(getBaseContentUri(), COL_EMAIL + " = ?", new String[]{email});
    } else return -1;
  }

  /**
   * Update a message count of some {@link LocalFolder}.
   *
   * @param context     Interface to global information about an application environment.
   * @param email       The account email.
   * @param folderName  A server folder name. Links to {@link #COL_FOLDER_NAME}
   * @param newMsgCount A new message count.
   * @return The count of updated rows. Will be 1 if information about {@link LocalFolder} was
   * updated or -1 otherwise.
   */
  public int updateLabelMsgsCount(Context context, String email, String folderName, int newMsgCount) {
    if (context != null && !TextUtils.isEmpty(folderName)) {
      ContentResolver contentResolver = context.getContentResolver();
      if (contentResolver != null) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_MESSAGE_COUNT, newMsgCount);
        String where = COL_EMAIL + "= ? AND " + COL_FOLDER_NAME + " = ? ";
        return contentResolver.update(getBaseContentUri(), contentValues, where, new String[]{email, folderName});
      } else return -1;
    } else return -1;
  }

  /**
   * This method update the local labels info. Here we will remove deleted and create new folders.
   *
   * @param context         Interface to global information about an application environment.
   * @param email           The account email.
   * @param oldLocalFolders The list of old {@link LocalFolder} object.
   * @param newLocalFolders The list of new {@link LocalFolder} object.
   * @return the {@link ContentProviderResult} array.
   */
  public ContentProviderResult[] updateLabels(Context context, String email, Collection<LocalFolder> oldLocalFolders,
                                              Collection<LocalFolder> newLocalFolders)
      throws RemoteException, OperationApplicationException {
    ContentResolver contentResolver = context.getContentResolver();
    if (email != null && contentResolver != null) {

      ArrayList<ContentProviderOperation> contentProviderOperations = new ArrayList<>();

      List<LocalFolder> deleteCandidates = new ArrayList<>();
      for (LocalFolder oldLocalFolder : oldLocalFolders) {
        boolean isFolderFound = false;
        for (LocalFolder newLocalFolder : newLocalFolders) {
          if (newLocalFolder.getFullName().equals(oldLocalFolder.getFullName())) {
            isFolderFound = true;
            break;
          }
        }

        if (!isFolderFound) {
          deleteCandidates.add(oldLocalFolder);
        }
      }

      List<LocalFolder> newCandidates = new ArrayList<>();
      for (LocalFolder newLocalFolder : newLocalFolders) {
        boolean isFolderFound = false;
        for (LocalFolder oldLocalFolder : oldLocalFolders) {
          if (oldLocalFolder.getFullName().equals(newLocalFolder.getFullName())) {
            isFolderFound = true;
            break;
          }
        }

        if (!isFolderFound) {
          newCandidates.add(newLocalFolder);
        }
      }

      for (LocalFolder localFolder : deleteCandidates) {
        String[] args = new String[]{email, localFolder.getFullName()};

        contentProviderOperations.add(ContentProviderOperation.newDelete(getBaseContentUri())
            .withSelection(COL_EMAIL + "= ? AND " + COL_FOLDER_NAME + " = ? ", args)
            .withYieldAllowed(true)
            .build());
      }

      for (LocalFolder localFolder : newCandidates) {
        contentProviderOperations.add(ContentProviderOperation.newInsert(getBaseContentUri())
            .withValues(prepareContentValues(email, localFolder))
            .withYieldAllowed(true)
            .build());
      }
      return contentResolver.applyBatch(getBaseContentUri().getAuthority(), contentProviderOperations);
    } else return new ContentProviderResult[0];
  }

  @NonNull
  private ContentValues prepareContentValues(String accountName, LocalFolder localFolder) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(COL_EMAIL, accountName);
    contentValues.put(COL_FOLDER_NAME, localFolder.getFullName());
    contentValues.put(COL_FOLDER_ALIAS, localFolder.getFolderAlias());
    contentValues.put(COL_MESSAGE_COUNT, localFolder.getMsgCount());
    contentValues.put(COL_IS_CUSTOM_LABEL, localFolder.isCustom());
    contentValues.put(COL_FOLDER_ATTRIBUTES, prepareAttributesToSaving(localFolder.getAttributes()));
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
