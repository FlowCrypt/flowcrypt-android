/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import com.flowcrypt.email.service.actionqueue.actions.Action;
import com.flowcrypt.email.service.actionqueue.actions.Action.ActionType;
import com.flowcrypt.email.util.google.gson.ActionJsonDeserializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * This object describes a logic of working with {@link Action} in the local database.
 *
 * @author Denis Bondarenko
 * Date: 30.01.2018
 * Time: 10:00
 * E-mail: DenBond7@gmail.com
 */

public class ActionQueueDaoSource extends BaseDaoSource {
  public static final String TABLE_NAME_ACTION_QUEUE = "action_queue";

  public static final String COL_EMAIL = "email";
  public static final String COL_ACTION_TYPE = "action_type";
  public static final String COL_ACTION_JSON = "action_json";

  public static final String ACTION_QUEUE_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
      TABLE_NAME_ACTION_QUEUE + " (" +
      BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
      COL_EMAIL + " VARCHAR(100) NOT NULL, " +
      COL_ACTION_TYPE + " TEXT NOT NULL, " +
      COL_ACTION_JSON + " TEXT NOT NULL " + ");";

  private Gson gson;

  public ActionQueueDaoSource() {
    gson = new GsonBuilder()
        .registerTypeAdapter(Action.class, new ActionJsonDeserializer())
        .create();
  }

  @Override
  public String getTableName() {
    return TABLE_NAME_ACTION_QUEUE;
  }

  /**
   * Save information about an {@link Action} to the database;
   *
   * @param context Interface to global information about an application environment;
   * @param action  An input {@link Action}.
   * @return The created {@link Uri} or null;
   */
  public Uri addAction(Context context, Action action) {
    ContentResolver contentResolver = context.getContentResolver();
    if (action != null && contentResolver != null) {
      ContentValues contentValues = generateContentValues(action);
      if (contentValues == null) return null;

      return contentResolver.insert(getBaseContentUri(), contentValues);
    } else return null;
  }

  /**
   * Save information about an {@link Action} to the database;
   *
   * @param sqLiteDatabase An instance of the local database;
   * @param action         An input {@link Action}.
   * @return the row ID of the newly inserted row, or -1 if an error occurred;
   */
  public long addAction(SQLiteDatabase sqLiteDatabase, Action action) {
    if (action != null && sqLiteDatabase != null) {
      ContentValues contentValues = generateContentValues(action);
      if (contentValues == null) return -1;

      return sqLiteDatabase.insert(TABLE_NAME_ACTION_QUEUE, null, contentValues);
    } else return -1;
  }

  /**
   * This method add rows per single transaction.
   *
   * @param context Interface to global information about an application environment.
   * @param actions The list of {@link Action} objects.
   */
  public int addActions(Context context, List<Action> actions) {
    if (actions != null && !actions.isEmpty()) {
      ContentResolver contentResolver = context.getContentResolver();
      ContentValues[] contentValuesArray = new ContentValues[actions.size()];

      for (int i = 0; i < actions.size(); i++) {
        Action action = actions.get(i);
        contentValuesArray[i] = generateContentValues(action);
      }

      return contentResolver.bulkInsert(getBaseContentUri(), contentValuesArray);
    } else return 0;
  }

  /**
   * Get the list of {@link Action} object from the local database for some email.
   *
   * @param context    Interface to global information about an application environment.
   * @param account An account information.
   * @return The list of {@link Action};
   */
  public List<Action> getActions(Context context, AccountDao account) {
    List<Action> actions = new ArrayList<>();
    if (account != null) {
      Cursor cursor = context.getContentResolver().query(getBaseContentUri(), null,
          ActionQueueDaoSource.COL_EMAIL + " = ? OR " + ActionQueueDaoSource.COL_EMAIL + " = ?",
          new String[]{account.getEmail(), Action.USER_SYSTEM}, null);

      if (cursor != null) {
        while (cursor.moveToNext()) {
          actions.add(getCurrentAction(context, cursor));
        }
      }

      if (cursor != null) {
        cursor.close();
      }
    }

    return actions;
  }

  /**
   * Get the list of {@link Action} object from the local database for some email using some {@link ActionType}.
   *
   * @param context    Interface to global information about an application environment.
   * @param account An account information.
   * @param actionType An action type.
   * @return The list of {@link Action};
   */
  @NonNull
  public List<Action> getActionsByType(Context context, AccountDao account, ActionType actionType) {
    List<Action> actions = new ArrayList<>();
    if (account != null && actionType != null) {
      Cursor cursor = context.getContentResolver().query(getBaseContentUri(), null,
          ActionQueueDaoSource.COL_EMAIL + " = ? AND " + ActionQueueDaoSource.COL_ACTION_TYPE + " = ?",
          new String[]{account.getEmail(), actionType.getValue()}, null);

      if (cursor != null) {
        while (cursor.moveToNext()) {
          actions.add(getCurrentAction(context, cursor));
        }
      }

      if (cursor != null) {
        cursor.close();
      }
    }

    return actions;
  }

  /**
   * Delete an {@link Action} from the database.
   *
   * @param context Interface to global information about an application environment.
   * @param action  An input {@link Action} which will be deleted.
   * @return The count of deleted rows. Will be 1 if information about {@link Action} was
   * deleted or -1 otherwise.
   */
  public int deleteAction(Context context, Action action) {
    if (action != null) {
      ContentResolver contentResolver = context.getContentResolver();
      if (contentResolver != null) {
        return contentResolver.delete(getBaseContentUri().buildUpon().appendPath(String.valueOf(action.getId()))
            .build(), null, null);
      } else return -1;
    } else return -1;
  }

  /**
   * Generate the {@link Action} from the current cursor position;
   *
   * @param context Interface to global information about an application environment;
   * @param cursor  The cursor from which to get the data.
   * @return {@link Action}.
   */
  private Action getCurrentAction(Context context, Cursor cursor) {
    Action action = gson.fromJson(cursor.getString(cursor.getColumnIndex(COL_ACTION_JSON)), Action.class);
    if (action != null) {
      action.setId(cursor.getLong(cursor.getColumnIndex(_ID)));
      return action;
    } else {
      return null;
    }
  }

  /**
   * Generate a {@link ContentValues} using {@link Action}.
   *
   * @param action The {@link Action} object;
   * @return The generated {@link ContentValues}.
   */
  @Nullable
  private ContentValues generateContentValues(Action action) {
    ContentValues contentValues = new ContentValues();
    if (action.getEmail() != null) {
      contentValues.put(COL_EMAIL, action.getEmail().toLowerCase());
    } else {
      return null;
    }

    contentValues.put(COL_ACTION_TYPE, action.getActionType().getValue());
    contentValues.put(COL_ACTION_JSON, gson.toJson(action));
    return contentValues;
  }
}

