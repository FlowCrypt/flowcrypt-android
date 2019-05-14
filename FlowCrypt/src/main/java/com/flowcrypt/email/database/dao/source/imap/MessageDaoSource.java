/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source.imap;

import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.LongSparseArray;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.LocalFolder;
import com.flowcrypt.email.api.email.model.MessageFlag;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.database.FlowCryptSQLiteOpenHelper;
import com.flowcrypt.email.database.MessageState;
import com.flowcrypt.email.database.dao.source.BaseDaoSource;
import com.flowcrypt.email.ui.activity.fragment.preferences.NotificationsSettingsFragment;
import com.flowcrypt.email.util.FileAndDirectoryUtils;
import com.flowcrypt.email.util.SharedPreferencesHelper;
import com.google.android.gms.common.util.CollectionUtils;
import com.sun.mail.imap.IMAPFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessageRemovedException;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import androidx.annotation.NonNull;

/**
 * This class describes the dao source for {@link GeneralMessageDetails} class.
 *
 * @author DenBond7
 * Date: 20.06.2017
 * Time: 10:49
 * E-mail: DenBond7@gmail.com
 */

public class MessageDaoSource extends BaseDaoSource {
  public static final String TABLE_NAME_MESSAGES = "messages";

  public static final String COL_EMAIL = "email";
  public static final String COL_FOLDER = "folder";
  public static final String COL_UID = "uid";
  public static final String COL_RECEIVED_DATE = "received_date";
  public static final String COL_SENT_DATE = "sent_date";
  public static final String COL_FROM_ADDRESSES = "from_address";
  public static final String COL_TO_ADDRESSES = "to_address";
  public static final String COL_CC_ADDRESSES = "cc_address";
  public static final String COL_SUBJECT = "subject";
  public static final String COL_FLAGS = "flags";
  public static final String COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS = "raw_message_without_attachments";
  public static final String COL_IS_MESSAGE_HAS_ATTACHMENTS = "is_message_has_attachments";
  public static final String COL_IS_ENCRYPTED = "is_encrypted";
  public static final String COL_IS_NEW = "is_new";
  public static final String COL_STATE = "state";
  public static final String COL_ATTACHMENTS_DIRECTORY = "attachments_directory";
  public static final String COL_ERROR_MSG = "error_msg";

  public static final int ENCRYPTED_STATE_UNDEFINED = -1;

  public static final String IMAP_MESSAGES_INFO_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
      TABLE_NAME_MESSAGES + " (" +
      BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
      COL_EMAIL + " VARCHAR(100) NOT NULL, " +
      COL_FOLDER + " TEXT NOT NULL, " +
      COL_UID + " INTEGER NOT NULL, " +
      COL_RECEIVED_DATE + " INTEGER DEFAULT NULL, " +
      COL_SENT_DATE + " INTEGER DEFAULT NULL, " +
      COL_FROM_ADDRESSES + " TEXT DEFAULT NULL, " +
      COL_TO_ADDRESSES + " TEXT DEFAULT NULL, " +
      COL_CC_ADDRESSES + " TEXT DEFAULT NULL, " +
      COL_SUBJECT + " TEXT DEFAULT NULL, " +
      COL_FLAGS + " TEXT DEFAULT NULL, " +
      COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS + " TEXT DEFAULT NULL, " +
      COL_IS_MESSAGE_HAS_ATTACHMENTS + " INTEGER DEFAULT 0, " +
      COL_IS_ENCRYPTED + " INTEGER DEFAULT -1, " +
      COL_IS_NEW + " INTEGER DEFAULT -1, " +
      COL_STATE + " INTEGER DEFAULT -1, " +
      COL_ATTACHMENTS_DIRECTORY + " TEXT, " +
      COL_ERROR_MSG + " TEXT DEFAULT NULL" + ");";

  public static final String CREATE_INDEX_EMAIL_IN_MESSAGES =
      INDEX_PREFIX + COL_EMAIL + "_in_" + TABLE_NAME_MESSAGES +
          " ON " + TABLE_NAME_MESSAGES + " (" + COL_EMAIL + ")";

  public static final String CREATE_INDEX_EMAIL_UID_FOLDER_IN_MESSAGES =
      UNIQUE_INDEX_PREFIX + COL_EMAIL + "_" + COL_UID + "_" + COL_FOLDER
          + "_in_" + TABLE_NAME_MESSAGES +
          " ON " + TABLE_NAME_MESSAGES +
          " (" + COL_EMAIL + ", " + COL_UID + ", " + COL_FOLDER + ")";

  /**
   * Prepare the content values for insert to the database. This method must be called in the
   * non-UI thread.
   *
   * @param email The email that the message linked.
   * @param label The folder label.
   * @param msg   The message which will be added to the database.
   * @param uid   The message UID.
   * @param isNew true if need to mark a given message as new
   * @return generated {@link ContentValues}
   * @throws MessagingException This exception may be occured when we call methods of thr
   *                            {@link Message} object</code>
   */
  @NonNull
  public static ContentValues prepareContentValues(String email, String label, Message msg, long uid, boolean isNew)
      throws MessagingException {
    ContentValues contentValues = new ContentValues();
    contentValues.put(COL_EMAIL, email);
    contentValues.put(COL_FOLDER, label);
    contentValues.put(COL_UID, uid);
    if (msg.getReceivedDate() != null) {
      contentValues.put(COL_RECEIVED_DATE, msg.getReceivedDate().getTime());
    }
    if (msg.getSentDate() != null) {
      contentValues.put(COL_SENT_DATE, msg.getSentDate().getTime());
    }
    contentValues.put(COL_FROM_ADDRESSES, InternetAddress.toString(msg.getFrom()));
    contentValues.put(COL_TO_ADDRESSES, InternetAddress.toString(msg.getRecipients(Message.RecipientType.TO)));
    contentValues.put(COL_CC_ADDRESSES, InternetAddress.toString(msg.getRecipients(Message.RecipientType.CC)));
    contentValues.put(COL_SUBJECT, msg.getSubject());
    contentValues.put(COL_FLAGS, msg.getFlags().toString().toUpperCase());
    contentValues.put(COL_IS_MESSAGE_HAS_ATTACHMENTS, hasAtt(msg));
    if (!msg.getFlags().contains(Flags.Flag.SEEN)) {
      contentValues.put(COL_IS_NEW, isNew);
    }
    return contentValues;
  }

  /**
   * Prepare {@link ContentValues} using {@link OutgoingMessageInfo}
   *
   * @param email The email that the message linked.
   * @param label The folder label.
   * @param uid   The message UID.
   * @param info  The input {@link OutgoingMessageInfo}
   * @return generated {@link ContentValues}
   */
  @NonNull
  public static ContentValues prepareContentValues(String email, String label, long uid, OutgoingMessageInfo info) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(COL_EMAIL, email);
    contentValues.put(COL_FOLDER, label);
    contentValues.put(COL_UID, uid);
    contentValues.put(COL_SENT_DATE, System.currentTimeMillis());
    contentValues.put(COL_SUBJECT, info.getSubject());
    contentValues.put(COL_FLAGS, MessageFlag.SEEN.getValue());
    contentValues.put(COL_IS_MESSAGE_HAS_ATTACHMENTS, !CollectionUtils.isEmpty(info.getAtts()) ||
        !CollectionUtils.isEmpty(info.getForwardedAtts()));
    return contentValues;
  }

  @Override
  public String getTableName() {
    return TABLE_NAME_MESSAGES;
  }

  /**
   * Add a new message details to the database. This method must be called in the non-UI thread.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param uid     The message UID.
   * @param message The message which will be added to the database.
   * @param isNew   true if need to mark a given message as new.
   * @return A {@link Uri} of the created row.
   */
  public Uri addRow(Context context, String email, String label, long uid, Message message, boolean isNew)
      throws MessagingException {
    ContentResolver contentResolver = context.getContentResolver();
    if (message != null && label != null && contentResolver != null) {
      ContentValues contentValues = prepareContentValues(email, label, message, uid, isNew);
      return contentResolver.insert(getBaseContentUri(), contentValues);
    } else return null;
  }

  /**
   * Add a new message details to the database.
   *
   * @param context       Interface to global information about an application environment.
   * @param contentValues {@link ContentValues} which contains information about a new message.
   * @return A {@link Uri} of the created row.
   */
  public Uri addRow(Context context, ContentValues contentValues) {
    ContentResolver contentResolver = context.getContentResolver();
    if (contentValues != null && contentResolver != null) {
      return contentResolver.insert(getBaseContentUri(), contentValues);
    } else return null;
  }

  /**
   * This method add rows per single transaction. This method must be called in the non-UI thread.
   *
   * @param context     Interface to global information about an application environment.
   * @param email       The email that the message linked.
   * @param label       The folder label.
   * @param folder      The {@link IMAPFolder} object which contains information about a remote folder.
   * @param msgs        The messages array.
   * @param isNew       true if need to mark messages as new.
   * @param isEncrypted true if the given messages are encrypted.
   * @return the number of newly created rows.
   * @throws MessagingException This exception may be occured when we call <code>mapFolder.getUID(message)</code>
   */
  public int addRows(Context context, String email, String label, IMAPFolder folder, Message[] msgs,
                     boolean isNew, boolean isEncrypted) throws MessagingException {
    return addRows(context, email, label, folder, msgs, new LongSparseArray<Boolean>(), isNew, isEncrypted);
  }

  /**
   * This method add rows per single transaction. This method must be called in the non-UI thread.
   *
   * @param context              Interface to global information about an application environment.
   * @param email                The email that the message linked.
   * @param label                The folder label.
   * @param folder               The {@link IMAPFolder} object which contains information about a remote folder.
   * @param msgs                 The messages array.
   * @param msgsEncryptionStates An array which contains info about a message encryption state
   * @param areAllMsgsEncrypted  true if the given messages are encrypted.
   * @return the number of newly created rows.
   * @throws MessagingException This exception may be occured when we call <code>mapFolder.getUID(message)</code>
   */
  public int addRows(Context context, String email, String label, IMAPFolder folder, Message[] msgs,
                     LongSparseArray<Boolean> msgsEncryptionStates, boolean isNew, boolean areAllMsgsEncrypted)
      throws MessagingException {
    if (msgs != null) {
      ContentResolver contentResolver = context.getContentResolver();
      ArrayList<ContentValues> contentValuesList = new ArrayList<>();

      boolean isNotificationDisabled = NotificationsSettingsFragment.NOTIFICATION_LEVEL_NEVER.equals
          (SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(context),
              Constants.PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER, ""));

      boolean onlyEncryptedMsgs = NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY
          .equals(SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(context),
              Constants.PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER, ""));

      for (Message msg : msgs) {
        try {
          ContentValues contentValues = prepareContentValues(email, label, msg, folder.getUID(msg), isNew);

          if (isNotificationDisabled) {
            contentValues.put(COL_IS_NEW, false);
          }

          Boolean isMsgEncrypted = areAllMsgsEncrypted ? Boolean.valueOf(true) :
              msgsEncryptionStates.get(folder.getUID(msg));
          if (isMsgEncrypted != null) {
            contentValues.put(COL_IS_ENCRYPTED, isMsgEncrypted);

            if (onlyEncryptedMsgs && !isMsgEncrypted) {
              contentValues.put(COL_IS_NEW, false);
            }
          }

          contentValuesList.add(contentValues);
        } catch (MessageRemovedException e) {
          e.printStackTrace();
        }
      }

      return contentResolver.bulkInsert(getBaseContentUri(), contentValuesList.toArray(new ContentValues[0]));
    } else return 0;
  }

  /**
   * This method delete cached messages.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param msgsUID The list of messages UID.
   */
  public void deleteMsgsByUID(Context context, String email, String label, Collection<Long> msgsUID) throws
      RemoteException, OperationApplicationException {
    ContentResolver contentResolver = context.getContentResolver();
    if (email != null && label != null && contentResolver != null) {
      int step = 50;

      List<String> selectionArgs = new LinkedList<>();
      selectionArgs.add(email);
      selectionArgs.add(label);

      ArrayList<Long> list = new ArrayList<>(msgsUID);

      if (msgsUID.size() <= step) {
        for (Long uid : msgsUID) {
          selectionArgs.add(String.valueOf(uid));
        }

        String where = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID + " IN (" +
            prepareSelectionArgsString(msgsUID.toArray()) + ");";

        contentResolver.delete(getBaseContentUri(), where, selectionArgs.toArray(new String[0]));
      } else {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        for (int i = 0; i < list.size(); i += step) {
          List<Long> stepUIDs = (list.size() - i > step) ? list.subList(i, i + step) : list.subList(i, list.size());
          List<String> selectionArgsForStep = new LinkedList<>(selectionArgs);

          for (Long uid : stepUIDs) {
            selectionArgsForStep.add(String.valueOf(uid));
          }

          String selection = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID
              + " IN (" + prepareSelectionArgsString(stepUIDs.toArray()) + ");";

          ops.add(ContentProviderOperation.newDelete(getBaseContentUri())
              .withSelection(selection, selectionArgsForStep.toArray(new String[0]))
              .withYieldAllowed(true)
              .build());
        }

        contentResolver.applyBatch(getBaseContentUri().getAuthority(), ops);
      }
    }
  }

  /**
   * This method update cached messages.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param folder  The {@link IMAPFolder} object which contains information about a
   *                remote folder.
   * @param msgs    The messages array.
   * @return the {@link ContentProviderResult} array.
   */
  public ContentProviderResult[] updateMsgsByUID(Context context, String email, String label,
                                                 IMAPFolder folder, Message[] msgs)
      throws RemoteException, OperationApplicationException, MessagingException {
    ContentResolver contentResolver = context.getContentResolver();
    if (email != null && label != null && contentResolver != null && msgs != null && msgs.length > 0) {

      ArrayList<ContentProviderOperation> ops = new ArrayList<>();
      for (Message message : msgs) {
        String selection = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID + " = ? ";

        ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(getBaseContentUri())
            .withValue(COL_FLAGS, message.getFlags().toString().toUpperCase())
            .withSelection(selection, new String[]{email, label, String.valueOf(folder.getUID(message))})
            .withYieldAllowed(true);

        if (message.getFlags().contains(Flags.Flag.SEEN)) {
          builder.withValue(COL_IS_NEW, false);
        }

        ops.add(builder.build());
      }
      return contentResolver.applyBatch(getBaseContentUri().getAuthority(), ops);
    } else return new ContentProviderResult[0];
  }

  /**
   * Add a new message details to the database. This method must be called in the non-UI thread.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param uid     The message UID.
   * @param raw     The raw message text which will be added to the database.
   * @return The count of the updated row or -1 up.
   */
  public int updateMsgRawText(Context context, String email, String label, long uid, String raw) {
    ContentResolver resolver = context.getContentResolver();
    if (email != null && label != null && resolver != null) {
      String where = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID + " = ? ";
      ContentValues values = new ContentValues();
      values.put(COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS, raw);
      return resolver.update(getBaseContentUri(), values, where, new String[]{email, label, String.valueOf(uid)});
    } else return -1;
  }

  /**
   * Update some message by the given parameters.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param uid     The message UID.
   * @param values  The {@link ContentValues} which contains new information.
   * @return The count of the updated row or -1 up.
   */
  public int updateMsg(Context context, String email, String label, long uid, ContentValues values) {
    ContentResolver resolver = context.getContentResolver();
    if (email != null && label != null && resolver != null) {
      String where = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID + " = ? ";
      return resolver.update(getBaseContentUri(), values, where, new String[]{email, label, String.valueOf(uid)});
    } else return -1;
  }

  /**
   * Update a state of some message.
   *
   * @param context      Interface to global information about an application environment
   * @param email        The email that the message linked
   * @param label        The folder label
   * @param uid          The message UID
   * @param messageState A new message state.
   * @return The count of the updated row or -1 up.
   */
  public int updateMsgState(Context context, String email, String label, long uid, MessageState messageState) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(COL_STATE, messageState.getValue());

    return updateMsg(context, email, label, uid, contentValues);
  }

  /**
   * Mark message as seen in the local database.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param uid     The message UID.
   * @return The count of the updated row or -1 up.
   */
  public int setSeenStatus(Context context, String email, String label, long uid) {
    ContentResolver resolver = context.getContentResolver();
    if (email != null && label != null && resolver != null) {
      ContentValues values = new ContentValues();
      values.put(COL_FLAGS, MessageFlag.SEEN.getValue());

      String where = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID + " = ? ";
      return resolver.update(getBaseContentUri(), values, where, new String[]{email, label, String.valueOf(uid)});
    } else return -1;
  }

  /**
   * Mark messages as old in the local database.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @return The count of the updated row or -1 up.
   */
  public int setOldStatus(Context context, String email, String label) {
    ContentResolver contentResolver = context.getContentResolver();
    if (email != null && label != null && contentResolver != null) {
      ContentValues contentValues = new ContentValues();
      contentValues.put(COL_IS_NEW, false);
      String where = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ?";
      return contentResolver.update(getBaseContentUri(), contentValues, where, new String[]{email, label});
    } else return -1;
  }

  /**
   * Mark messages as old in the local database.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param uidList The list of the UIDs.
   * @return The count of the updated row or -1 up.
   */
  public int setOldStatus(Context context, String email, String label, List<String> uidList) {
    ContentResolver contentResolver = context.getContentResolver();
    if (contentResolver != null && email != null && label != null && uidList != null && !uidList.isEmpty()) {
      ContentValues contentValues = new ContentValues();
      contentValues.put(COL_IS_NEW, false);

      List<String> args = new ArrayList<>();
      args.add(0, email);
      args.add(1, label);
      args.addAll(uidList);

      String where = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID + " IN (" + TextUtils.join(",",
          Collections.nCopies(uidList.size(), "?")) + ")";

      return contentResolver.update(getBaseContentUri(), contentValues, where, args.toArray(new String[]{}));
    } else return -1;
  }

  /**
   * Update the message flags in the local database.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param uid     The message UID.
   * @param flags   The message flags.
   * @return The count of the updated row or -1 up.
   */
  public int updateLocalMsgFlags(Context context, String email, String label, long uid, @NonNull Flags flags) {
    ContentResolver resolver = context.getContentResolver();
    if (email != null && label != null && resolver != null) {
      ContentValues values = new ContentValues();
      values.put(COL_FLAGS, flags.toString().toUpperCase());
      if (flags.contains(Flags.Flag.SEEN)) {
        values.put(COL_IS_NEW, false);
      }
      String where = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID + " = ? ";
      return resolver.update(getBaseContentUri(), values, where, new String[]{email, label, String.valueOf(uid)});
    } else return -1;
  }

  /**
   * Generate a {@link LocalFolder} object from the current cursor position.
   *
   * @param cursor The {@link Cursor} which contains information about {@link LocalFolder}.
   * @return A generated {@link LocalFolder}.
   */
  public GeneralMessageDetails getMsgInfo(Cursor cursor) {
    GeneralMessageDetails details = new GeneralMessageDetails(
        cursor.getString(cursor.getColumnIndex(COL_EMAIL)),
        cursor.getString(cursor.getColumnIndex(COL_FOLDER)),
        cursor.getInt(cursor.getColumnIndex(COL_UID)),
        cursor.getLong(cursor.getColumnIndex(COL_RECEIVED_DATE)),
        cursor.getLong(cursor.getColumnIndex(COL_SENT_DATE)),
        null,
        null,
        null,
        cursor.getString(cursor.getColumnIndex(COL_SUBJECT)),
        Arrays.asList(parseFlags(cursor.getString(cursor.getColumnIndex(COL_FLAGS)))),
        cursor.getString(cursor.getColumnIndex(COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS)),
        cursor.getInt(cursor.getColumnIndex(COL_IS_MESSAGE_HAS_ATTACHMENTS)) == 1,
        cursor.getInt(cursor.getColumnIndex(COL_IS_ENCRYPTED)) == 1,
        Objects.requireNonNull(MessageState.generate(cursor.getInt(cursor.getColumnIndex(COL_STATE)))),
        cursor.getString(cursor.getColumnIndex(COL_ATTACHMENTS_DIRECTORY)),
        cursor.getString(cursor.getColumnIndex(COL_ERROR_MSG))
    );

    try {
      String fromAddresses = cursor.getString(cursor.getColumnIndex(COL_FROM_ADDRESSES));
      details.setFrom(TextUtils.isEmpty(fromAddresses) ? null : Arrays.asList(InternetAddress.parse(fromAddresses)));
    } catch (AddressException e) {
      e.printStackTrace();
    }

    try {
      String toAddresses = cursor.getString(cursor.getColumnIndex(COL_TO_ADDRESSES));
      details.setTo(TextUtils.isEmpty(toAddresses) ? null : Arrays.asList(InternetAddress.parse(toAddresses)));
    } catch (AddressException e) {
      e.printStackTrace();
    }

    try {
      String ccAddresses = cursor.getString(cursor.getColumnIndex(COL_CC_ADDRESSES));
      details.setCc(TextUtils.isEmpty(ccAddresses) ? null : Arrays.asList(InternetAddress.parse(ccAddresses)));
    } catch (AddressException e) {
      e.printStackTrace();
    }

    return details;
  }

  /**
   * Get all messages of some folder.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email of the {@link LocalFolder}.
   * @param label   The label name.
   * @return A  list of {@link GeneralMessageDetails} objects.
   */
  public List<GeneralMessageDetails> getMsgs(Context context, String email, String label) {
    ContentResolver contentResolver = context.getContentResolver();
    String selection = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ?";
    Cursor cursor = contentResolver.query(getBaseContentUri(), null, selection, new String[]{email, label}, null);

    List<GeneralMessageDetails> generalMsgDetailsList = new ArrayList<>();

    if (cursor != null) {
      while (cursor.moveToNext()) {
        generalMsgDetailsList.add(getMsgInfo(cursor));
      }
      cursor.close();
    }

    return generalMsgDetailsList;
  }

  /**
   * Get all messages of the outbox folder.
   *
   * @param context  Interface to global information about an application environment.
   * @param email    The email of the {@link LocalFolder}.
   * @param msgState The message state which will be used for filter results.
   * @return A  list of {@link GeneralMessageDetails} objects.
   */
  public List<GeneralMessageDetails> getOutboxMsgs(Context context, String email, MessageState msgState) {
    ContentResolver contentResolver = context.getContentResolver();
    String selection = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_STATE + " = ?";
    String folder = JavaEmailConstants.FOLDER_OUTBOX;
    String[] selectionArgs = new String[]{email, folder, String.valueOf(msgState.getValue())};
    Cursor cursor = contentResolver.query(getBaseContentUri(), null, selection, selectionArgs, null);

    List<GeneralMessageDetails> generalMsgDetailsList = new ArrayList<>();

    if (cursor != null) {
      while (cursor.moveToNext()) {
        generalMsgDetailsList.add(getMsgInfo(cursor));
      }
      cursor.close();
    }

    return generalMsgDetailsList;
  }

  /**
   * Get all messages of the outbox folder which are not sent.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email of the {@link LocalFolder}.
   * @return A  list of {@link GeneralMessageDetails} objects.
   */
  public List<GeneralMessageDetails> getOutboxMsgs(Context context, String email) {
    ContentResolver contentResolver = context.getContentResolver();
    String selection = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_STATE + " NOT IN (?, ?)";
    String[] selectionArgs = new String[]{email, JavaEmailConstants.FOLDER_OUTBOX, String.valueOf(MessageState.SENT
        .getValue()), String.valueOf(MessageState.SENT_WITHOUT_LOCAL_COPY.getValue())};
    Cursor cursor = contentResolver.query(getBaseContentUri(), null, selection, selectionArgs, null);

    List<GeneralMessageDetails> generalMsgDetailsList = new ArrayList<>();

    if (cursor != null) {
      while (cursor.moveToNext()) {
        generalMsgDetailsList.add(getMsgInfo(cursor));
      }
      cursor.close();
    }

    return generalMsgDetailsList;
  }

  /**
   * Get new messages.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @return A  list of {@link GeneralMessageDetails} objects.
   */
  public List<GeneralMessageDetails> getNewMsgs(Context context, String email, String label) {
    ContentResolver contentResolver = context.getContentResolver();

    String orderType;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
      orderType = "ASC";
    } else {
      orderType = "DESC";
    }

    String selection = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_IS_NEW + " = 1 ";
    String[] selectionArgs = new String[]{email, label};
    Cursor cursor = contentResolver.query(getBaseContentUri(), null, selection, selectionArgs,
        COL_RECEIVED_DATE + " " + orderType);

    List<GeneralMessageDetails> detailsList = new ArrayList<>();

    if (cursor != null) {
      while (cursor.moveToNext()) {
        detailsList.add(getMsgInfo(cursor));
      }
      cursor.close();
    }

    return detailsList;
  }

  /**
   * Get all {@link LocalFolder} objects from the database by an email.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @param uid     The uid of the message.
   * @return {@link GeneralMessageDetails} if the information about a message is exists.
   */
  public GeneralMessageDetails getMsg(Context context, String email, String label, long uid) {
    ContentResolver contentResolver = context.getContentResolver();
    String selection = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID + " = ? ";
    String[] selectionArgs = new String[]{email, label, String.valueOf(uid)};
    Cursor cursor = contentResolver.query(getBaseContentUri(), null, selection, selectionArgs, null);

    GeneralMessageDetails details = null;

    if (cursor != null) {
      if (cursor.moveToFirst()) {
        details = getMsgInfo(cursor);
      }
      cursor.close();
    }

    return details;
  }

  /**
   * Check is the message exists in the local database.
   *
   * @param context Interface to global information about an application environment.
   * @param uid     The UID of the message.
   * @return true if message exists in the database, false otherwise.
   */
  public boolean hasMassage(Context context, long uid) {
    ContentResolver contentResolver = context.getContentResolver();
    String selection = COL_UID + " = ?";
    String[] selectionArgs = new String[]{String.valueOf(uid)};
    Cursor cursor = contentResolver.query(getBaseContentUri(), null, selection, selectionArgs, null);

    if (cursor != null) {
      boolean result = cursor.getCount() == 1;
      cursor.close();
      return result;
    }

    return false;
  }

  /**
   * Get the minimum UID in the database for some label.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @return The minimum UID for the current label or -1 if it not exists.
   */
  public int getLabelMinUID(Context context, String email, String label) {
    ContentResolver contentResolver = context.getContentResolver();

    String[] projection = new String[]{"min(" + COL_UID + ")"};
    String selection = COL_EMAIL + " = ? AND " + MessageDaoSource.COL_FOLDER + " = ?";
    String[] selectionArgs = new String[]{email, label};

    Cursor cursor = contentResolver.query(getBaseContentUri(), projection, selection, selectionArgs, null);

    if (cursor != null && cursor.moveToFirst()) {
      int uid = cursor.getInt(0);
      cursor.close();
      return uid;
    }

    return -1;
  }

  /**
   * Get the last UID of a message in the database for some label.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @return The last UID for the current label or -1 if it not exists.
   */
  public int getLastUIDOfMsgInLabel(Context context, String email, String label) {
    ContentResolver contentResolver = context.getContentResolver();

    String[] projection = new String[]{"max(" + COL_UID + ")"};
    String selection = COL_EMAIL + " = ? AND " + MessageDaoSource.COL_FOLDER + " = ?";
    String[] selectionArgs = new String[]{email, label};

    Cursor cursor = contentResolver.query(getBaseContentUri(), projection, selection, selectionArgs, null);

    if (cursor != null && cursor.moveToFirst()) {
      int uid = cursor.getInt(0);
      cursor.close();
      return uid;
    }

    return -1;
  }

  /**
   * Get the oldest UID of a message in the database for some label.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @return The last UID for the current label or -1 if it not exists.
   */
  public int getOldestUIDOfMsgInLabel(Context context, String email, String label) {
    ContentResolver contentResolver = context.getContentResolver();

    String[] projection = new String[]{"min(" + COL_UID + ")"};
    String selection = COL_EMAIL + " = ? AND " + MessageDaoSource.COL_FOLDER + " = ?";
    String[] selectionArgs = new String[]{email, label};

    Cursor cursor = contentResolver.query(getBaseContentUri(), projection, selection, selectionArgs, null);

    if (cursor != null && cursor.moveToFirst()) {
      int uid = cursor.getInt(0);
      cursor.close();
      return uid;
    }

    return -1;
  }

  /**
   * Get the list of UID of all messages in the database for some label.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @return The list of UID of all messages in the database for some label.
   */
  public List<String> getUIDsOfMsgsInLabel(Context context, String email, String label) {
    ContentResolver contentResolver = context.getContentResolver();
    List<String> uidList = new ArrayList<>();

    String[] projection = new String[]{COL_UID};
    String selection = COL_EMAIL + " = ? AND " + COL_FOLDER + " = ?";
    String[] selectionArgs = new String[]{email, label};

    Cursor cursor = contentResolver.query(getBaseContentUri(), projection, selection, selectionArgs, null);

    if (cursor != null) {
      while (cursor.moveToNext()) {
        uidList.add(cursor.getString(cursor.getColumnIndex(COL_UID)));
      }
      cursor.close();
    }

    return uidList;
  }

  /**
   * Get the list of UID of all messages in the database which were not checked to encryption.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @return The list of UID of selected messages in the database for some label.
   */
  public List<Long> getNotCheckedUIDs(Context context, String email, String label) {
    ContentResolver contentResolver = context.getContentResolver();
    List<Long> uidList = new ArrayList<>();

    String[] projection = new String[]{COL_UID};
    String selection = COL_EMAIL + " = ? AND " + COL_FOLDER + " = ?" + " AND " + COL_IS_ENCRYPTED + " = " +
        ENCRYPTED_STATE_UNDEFINED;
    String[] selectionArgs = new String[]{email, label};

    Cursor cursor = contentResolver.query(getBaseContentUri(), projection, selection, selectionArgs, null);

    if (cursor != null) {
      while (cursor.moveToNext()) {
        uidList.add(cursor.getLong(cursor.getColumnIndex(COL_UID)));
      }
      cursor.close();
    }

    return uidList;
  }

  /**
   * Get a map of UID and flags of all messages in the database for some label.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @return The map of UID and flags of all messages in the database for some label.
   */
  @SuppressLint("UseSparseArrays")
  public Map<Long, String> getMapOfUIDAndMsgFlags(Context context, String email, String label) {
    ContentResolver contentResolver = context.getContentResolver();
    Map<Long, String> uidList = new HashMap<>();
    String[] projection = new String[]{COL_UID, COL_FLAGS};
    String selection = COL_EMAIL + " = ? AND " + COL_FOLDER + " = ?";
    String[] selectionArgs = new String[]{email, label};

    Cursor cursor = contentResolver.query(getBaseContentUri(), projection, selection, selectionArgs, null);

    if (cursor != null) {
      while (cursor.moveToNext()) {
        uidList.put(cursor.getLong(cursor.getColumnIndex(COL_UID)),
            cursor.getString(cursor.getColumnIndex(COL_FLAGS)));
      }
      cursor.close();
    }

    return uidList;
  }

  /**
   * Get a list of UID and flags of all unseen messages in the database for some label.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @return The list of UID and flags of all unseen messages in the database for some label.
   */
  @SuppressLint("UseSparseArrays")
  public List<Integer> getUIDOfUnseenMsgs(Context context, String email, String label) {
    ContentResolver contentResolver = context.getContentResolver();

    String[] projection = new String[]{COL_UID};
    String selection = COL_EMAIL + " = ? AND " + COL_FOLDER + " = ? AND " + COL_FLAGS + " NOT LIKE '%"
        + MessageFlag.SEEN.getValue() + "%'";
    String[] selectionArgs = new String[]{email, label};

    Cursor cursor = contentResolver.query(getBaseContentUri(), projection, selection, selectionArgs, null);

    List<Integer> uidList = new ArrayList<>();
    if (cursor != null) {
      while (cursor.moveToNext()) {
        uidList.add(cursor.getInt(cursor.getColumnIndex(COL_UID)));
      }
      cursor.close();
    }

    return uidList;
  }

  /**
   * Get the count of messages in the database for some label.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The user email.
   * @param label   The label name.
   * @return The count of messages for the current label.
   */
  public int getLabelMsgsCount(Context context, String email, String label) {
    ContentResolver contentResolver = context.getContentResolver();

    String[] projection = new String[]{FlowCryptSQLiteOpenHelper.COLUMN_NAME_COUNT};
    String selection = COL_EMAIL + " = ? AND " + MessageDaoSource.COL_FOLDER + " = ?";
    String[] selectionArgs = new String[]{email, label};

    Cursor cursor = contentResolver.query(getBaseContentUri(), projection, selection, selectionArgs, null);

    if (cursor != null && cursor.moveToFirst()) {
      int uid = cursor.getInt(cursor.getColumnIndex(FlowCryptSQLiteOpenHelper
          .COLUMN_NAME_COUNT));
      cursor.close();
      return uid;
    }

    return 0;
  }

  /**
   * Delete a message from a some folder in the local database.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param uid     The message UID.
   * @return The number of rows deleted.
   */
  public int deleteMsg(Context context, String email, String label, long uid) {
    ContentResolver contentResolver = context.getContentResolver();
    if (email != null && label != null && contentResolver != null) {
      String where = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID + " = ? ";
      String[] selectionArgs = new String[]{email, label, String.valueOf(uid)};
      return contentResolver.delete(getBaseContentUri(), where, selectionArgs);
    } else return -1;
  }

  /**
   * Delete cached messages.
   *
   * @param context Interface to global information about an application environment.
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @return The number of rows deleted.
   */
  public int deleteCachedMsgs(Context context, String email, String label) {
    ContentResolver contentResolver = context.getContentResolver();
    if (email != null && label != null && contentResolver != null) {
      String where = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ?";
      String[] selectionArgs = new String[]{email, label};
      return contentResolver.delete(getBaseContentUri(), where, selectionArgs);
    } else return -1;
  }

  /**
   * @param context              Interface to global information about an application environment.
   * @param email                The email that the message linked.
   * @param label                The folder label.
   * @param msgsEncryptionStates The array which contains information about an encrypted state of some messages
   * @return the {@link ContentProviderResult} array.
   * @throws RemoteException
   * @throws OperationApplicationException
   */
  public ContentProviderResult[] updateEncryptionStates(Context context, String email, String label,
                                                        LongSparseArray<Boolean> msgsEncryptionStates)
      throws RemoteException, OperationApplicationException {
    ContentResolver contentResolver = context.getContentResolver();

    if (msgsEncryptionStates != null && msgsEncryptionStates.size() > 0) {
      ArrayList<ContentProviderOperation> ops = new ArrayList<>();
      for (int i = 0, arraySize = msgsEncryptionStates.size(); i < arraySize; i++) {
        long uid = msgsEncryptionStates.keyAt(i);
        Boolean b = msgsEncryptionStates.get(uid);
        String selection = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID + " = ? ";

        ops.add(ContentProviderOperation.newUpdate(getBaseContentUri())
            .withValue(COL_IS_ENCRYPTED, b)
            .withSelection(selection, new String[]{email, label, String.valueOf(uid)})
            .withYieldAllowed(true)
            .build());
      }
      return contentResolver.applyBatch(getBaseContentUri().getAuthority(), ops);
    } else return new ContentProviderResult[0];
  }

  /**
   * Delete an outgoing message.
   *
   * @param context Interface to global information about an application environment.
   * @param details Input details about the outgoing message.
   * @return The number of rows deleted.
   */
  public int deleteOutgoingMsg(Context context, GeneralMessageDetails details) {
    int deletedRows = -1;
    ContentResolver contentResolver = context.getContentResolver();

    if (details.getEmail() != null && details.getLabel() != null && contentResolver != null) {
      String where = COL_EMAIL + "= ? AND "
          + COL_FOLDER + " = ? AND "
          + COL_UID + " = ? AND "
          + COL_STATE + " != " + MessageState.SENDING.getValue() + " AND "
          + COL_STATE + " != " + MessageState.SENT_WITHOUT_LOCAL_COPY.getValue();
      String[] selectionArgs = new String[]{details.getEmail(), details.getLabel(), String.valueOf(details.getUid())};
      deletedRows = contentResolver.delete(getBaseContentUri(), where, selectionArgs);
    }

    if (deletedRows > 0) {
      new ImapLabelsDaoSource().updateLabelMsgsCount(context, details.getEmail(),
          JavaEmailConstants.FOLDER_OUTBOX, new MessageDaoSource().getOutboxMsgs(context,
              details.getEmail()).size());

      if (details.getHasAtts()) {
        AttachmentDaoSource attDaoSource = new AttachmentDaoSource();

        List<AttachmentInfo> attachmentInfoList = attDaoSource.getAttInfoList(context, details
            .getEmail(), JavaEmailConstants.FOLDER_OUTBOX, details.getUid());

        if (!CollectionUtils.isEmpty(attachmentInfoList)) {
          attDaoSource.deleteAtts(context, details.getEmail(),
              details.getLabel(), details.getUid());

          if (!TextUtils.isEmpty(details.getAttsDir())) {
            try {
              String parentDirName = details.getAttsDir();
              File dir = new File(new File(context.getCacheDir(), Constants.ATTACHMENTS_CACHE_DIR), parentDirName);
              FileAndDirectoryUtils.deleteDir(dir);
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
        }
      }
    }

    return deletedRows;
  }

  /**
   * Add the messages which have a current state equal {@link MessageState#SENDING} to the sending queue again.
   *
   * @param context Interface to global information about an application environment
   * @param email   The email that the message linked
   * @return The count of the updated row or -1 up.
   */
  public int resetMsgsWithSendingState(Context context, String email) {
    ContentValues contentValues = new ContentValues();
    contentValues.put(COL_STATE, MessageState.QUEUED.getValue());

    ContentResolver contentResolver = context.getContentResolver();
    if (email != null && contentResolver != null) {
      String selection = COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_STATE + " = ? ";
      String[] selectionArgs = new String[]{email, JavaEmailConstants.FOLDER_OUTBOX,
          String.valueOf(MessageState.SENDING.getValue())};
      return contentResolver.update(getBaseContentUri(), contentValues, selection, selectionArgs);
    } else return -1;
  }

  private static String[] parseArray(String attributesAsString, String regex) {
    if (attributesAsString != null && attributesAsString.length() > 0) {
      return attributesAsString.split(regex);
    } else {
      return null;
    }
  }

  /**
   * Check is {@link Part} has attachment.
   * <p>
   * If the part contains a wrong MIME structure we will receive the exception "Unable to load BODYSTRUCTURE" when
   * calling {@link Part#isMimeType(String)}
   *
   * @param part The parent part.
   * @return <tt>boolean</tt> true if {@link Part} has attachment, false otherwise or if an error has occurred.
   */
  private static boolean hasAtt(Part part) {
    try {
      if (part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
        Multipart multiPart = (Multipart) part.getContent();
        int partsNumber = multiPart.getCount();
        for (int partCount = 0; partCount < partsNumber; partCount++) {
          BodyPart bodyPart = multiPart.getBodyPart(partCount);
          if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
            boolean hasAtt = hasAtt(bodyPart);
            if (hasAtt) {
              return true;
            }
          } else if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
            return true;
          }
        }
        return false;
      } else {
        return false;
      }
    } catch (MessagingException | IOException e) {
      e.printStackTrace();
      return false;
    }
  }

  private String[] parseFlags(String string) {
    return parseArray(string, "\\s");
  }
}
