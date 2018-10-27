/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.LongSparseArray;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.MessageFlag;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessageRemovedException;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * This class describes the dao source for {@link GeneralMessageDetails} class.
 *
 * @author DenBond7
 *         Date: 20.06.2017
 *         Time: 10:49
 *         E-mail: DenBond7@gmail.com
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
            COL_ATTACHMENTS_DIRECTORY + " TEXT " + ");";

    public static final String CREATE_INDEX_EMAIL_IN_MESSAGES =
            "CREATE INDEX IF NOT EXISTS " + COL_EMAIL + "_in_" + TABLE_NAME_MESSAGES +
                    " ON " + TABLE_NAME_MESSAGES + " (" + COL_EMAIL + ")";

    public static final String CREATE_INDEX_EMAIL_UID_FOLDER_IN_MESSAGES =
            "CREATE UNIQUE INDEX IF NOT EXISTS " + COL_EMAIL + "_" + COL_UID + "_" + COL_FOLDER
                    + "_in_" + TABLE_NAME_MESSAGES +
                    " ON " + TABLE_NAME_MESSAGES +
                    " (" + COL_EMAIL + ", " + COL_UID + ", " + COL_FOLDER + ")";

    /**
     * Prepare the content values for insert to the database. This method must be called in the
     * non-UI thread.
     *
     * @param email   The email that the message linked.
     * @param label   The folder label.
     * @param message The message which will be added to the database.
     * @param uid     The message UID.
     * @param isNew   true if need to mark a given message as new
     * @return generated {@link ContentValues}
     * @throws MessagingException This exception may be occured when we call methods of thr
     *                            {@link Message} object</code>
     */
    @NonNull
    public static ContentValues prepareContentValues(String email, String label, Message message, long uid, boolean
            isNew) throws MessagingException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_EMAIL, email);
        contentValues.put(COL_FOLDER, label);
        contentValues.put(COL_UID, uid);
        if (message.getReceivedDate() != null) {
            contentValues.put(COL_RECEIVED_DATE, message.getReceivedDate().getTime());
        }
        if (message.getSentDate() != null) {
            contentValues.put(COL_SENT_DATE, message.getSentDate().getTime());
        }
        contentValues.put(COL_FROM_ADDRESSES, InternetAddress.toString(message.getFrom()));
        contentValues.put(COL_TO_ADDRESSES, InternetAddress.toString(message.getRecipients(Message.RecipientType.TO)));
        contentValues.put(COL_CC_ADDRESSES, InternetAddress.toString(message.getRecipients(Message.RecipientType.CC)));
        contentValues.put(COL_SUBJECT, message.getSubject());
        contentValues.put(COL_FLAGS, message.getFlags().toString().toUpperCase());
        contentValues.put(COL_IS_MESSAGE_HAS_ATTACHMENTS, isMessageHasAttachment(message));
        if (!message.getFlags().contains(Flags.Flag.SEEN)) {
            contentValues.put(COL_IS_NEW, isNew);
        }
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
     * This method add rows per single transaction. This method must be called in the non-UI thread.
     *
     * @param context     Interface to global information about an application environment.
     * @param email       The email that the message linked.
     * @param label       The folder label.
     * @param imapFolder  The {@link IMAPFolder} object which contains information about a
     *                    remote folder.
     * @param messages    The messages array.
     * @param isNew       true if need to mark messages as new.
     * @param isEncrypted true if the given messages are encrypted.
     * @return the number of newly created rows.
     * @throws MessagingException This exception may be occured when we call <code>mapFolder
     *                            .getUID(message)</code>
     */
    public int addRows(Context context, String email, String label, IMAPFolder imapFolder, Message[] messages,
                       boolean isNew, boolean isEncrypted) throws MessagingException {
        return addRows(context, email, label, imapFolder, messages, new LongSparseArray<Boolean>(), isNew, isEncrypted);
    }

    /**
     * This method add rows per single transaction. This method must be called in the non-UI thread.
     *
     * @param context                 Interface to global information about an application environment.
     * @param email                   The email that the message linked.
     * @param label                   The folder label.
     * @param imapFolder              The {@link IMAPFolder} object which contains information about a
     *                                remote folder.
     * @param messages                The messages array.
     * @param isMessageEncryptedInfo  An array which contains info about a message encryption state
     * @param areAllMessagesEncrypted true if the given messages are encrypted.
     * @return the number of newly created rows.
     * @throws MessagingException This exception may be occured when we call <code>mapFolder
     *                            .getUID(message)</code>
     */
    public int addRows(Context context, String email, String label, IMAPFolder imapFolder, Message[] messages,
                       LongSparseArray<Boolean> isMessageEncryptedInfo, boolean isNew, boolean areAllMessagesEncrypted)
            throws MessagingException {
        if (messages != null) {
            ContentResolver contentResolver = context.getContentResolver();
            ArrayList<ContentValues> contentValuesArrayList = new ArrayList<>();

            boolean isNotificationDisabled = NotificationsSettingsFragment.NOTIFICATION_LEVEL_NEVER.equals
                    (SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(context),
                            Constants.PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER, ""));

            boolean isEncryptedMessagesOnly = NotificationsSettingsFragment.NOTIFICATION_LEVEL_ENCRYPTED_MESSAGES_ONLY
                    .equals(SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(context),
                            Constants.PREFERENCES_KEY_MESSAGES_NOTIFICATION_FILTER, ""));

            for (Message message : messages) {
                try {
                    ContentValues contentValues = prepareContentValues(email, label, message,
                            imapFolder.getUID(message), isNew);

                    if (isNotificationDisabled) {
                        contentValues.put(COL_IS_NEW, false);
                    }

                    Boolean isMessageEncrypted = areAllMessagesEncrypted ? Boolean.valueOf(true)
                            : isMessageEncryptedInfo.get(imapFolder.getUID(message));
                    if (isMessageEncrypted != null) {
                        contentValues.put(COL_IS_ENCRYPTED, isMessageEncrypted);

                        if (isEncryptedMessagesOnly && !isMessageEncrypted) {
                            contentValues.put(COL_IS_NEW, false);
                        }
                    }

                    contentValuesArrayList.add(contentValues);
                } catch (MessageRemovedException e) {
                    e.printStackTrace();
                }
            }

            return contentResolver.bulkInsert(getBaseContentUri(),
                    contentValuesArrayList.toArray(new ContentValues[0]));
        } else return 0;
    }

    /**
     * This method delete cached messages.
     *
     * @param context     Interface to global information about an application environment.
     * @param email       The email that the message linked.
     * @param label       The folder label.
     * @param messagesUID The list of messages UID.
     */
    public void deleteMessagesByUID(Context context, String email, String label, Collection<Long> messagesUID) throws
            RemoteException, OperationApplicationException {
        ContentResolver contentResolver = context.getContentResolver();
        if (email != null && label != null && contentResolver != null) {
            int numberOfMaxInPerStep = 50;

            List<String> selectionArgs = new LinkedList<>();
            selectionArgs.add(email);
            selectionArgs.add(label);

            ArrayList<Long> list = new ArrayList<>(messagesUID);

            if (messagesUID.size() <= numberOfMaxInPerStep) {
                for (Long uid : messagesUID) {
                    selectionArgs.add(String.valueOf(uid));
                }

                contentResolver.delete(getBaseContentUri(), COL_EMAIL + "= ? AND "
                                + COL_FOLDER + " = ? AND "
                                + COL_UID + " IN (" + prepareSelectionArgsString(messagesUID.toArray()) + ");",
                        selectionArgs.toArray(new String[0]));
            } else {
                ArrayList<ContentProviderOperation> ops = new ArrayList<>();

                for (int i = 0; i < list.size(); i += numberOfMaxInPerStep) {
                    List<Long> stepUIDs = (list.size() - i > numberOfMaxInPerStep) ? list.subList(i, i +
                            numberOfMaxInPerStep) : list.subList(i, list.size());
                    List<String> selectionArgsForStep = new LinkedList<>(selectionArgs);

                    for (Long uid : stepUIDs) {
                        selectionArgsForStep.add(String.valueOf(uid));
                    }

                    ops.add(ContentProviderOperation.newDelete(getBaseContentUri())
                            .withSelection(COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID
                                            + " IN (" + prepareSelectionArgsString(stepUIDs.toArray()) + ");",
                                    selectionArgsForStep.toArray(new String[0]))
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
     * @param context    Interface to global information about an application environment.
     * @param email      The email that the message linked.
     * @param label      The folder label.
     * @param imapFolder The {@link IMAPFolder} object which contains information about a
     *                   remote folder.
     * @param messages   The messages array.
     * @return the {@link ContentProviderResult} array.
     */
    public ContentProviderResult[] updateMessagesByUID(Context context, String email, String label,
                                                       IMAPFolder imapFolder, Message[] messages)
            throws RemoteException, OperationApplicationException, MessagingException {
        ContentResolver contentResolver = context.getContentResolver();
        if (email != null && label != null && contentResolver != null && messages != null && messages.length > 0) {

            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            for (Message message : messages) {
                ContentProviderOperation.Builder builder = ContentProviderOperation.newUpdate(getBaseContentUri())
                        .withValue(COL_FLAGS, message.getFlags().toString().toUpperCase())
                        .withSelection(COL_EMAIL + "= ? AND "
                                        + COL_FOLDER + " = ? AND "
                                        + COL_UID + " = ? ",
                                new String[]{email, label, String.valueOf(imapFolder.getUID(message))})
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
    public int updateMessageRawText(Context context, String email, String label, long uid,
                                    String raw) {
        ContentResolver contentResolver = context.getContentResolver();
        if (email != null && label != null && contentResolver != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS, raw);
            return contentResolver.update(getBaseContentUri(), contentValues,
                    COL_EMAIL + "= ? AND "
                            + COL_FOLDER + " = ? AND "
                            + COL_UID + " = ? ", new String[]{email, label, String.valueOf(uid)});
        } else return -1;
    }

    /**
     * Update some message by the given parameters.
     *
     * @param context       Interface to global information about an application environment.
     * @param email         The email that the message linked.
     * @param label         The folder label.
     * @param uid           The message UID.
     * @param contentValues The {@link ContentValues} which contains new information.
     * @return The count of the updated row or -1 up.
     */
    public int updateMessage(Context context, String email, String label, long uid, ContentValues contentValues) {
        ContentResolver contentResolver = context.getContentResolver();
        if (email != null && label != null && contentResolver != null) {
            return contentResolver.update(getBaseContentUri(), contentValues,
                    COL_EMAIL + "= ? AND "
                            + COL_FOLDER + " = ? AND "
                            + COL_UID + " = ? ", new String[]{email, label, String.valueOf(uid)});
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
    public int updateMessageState(Context context, String email, String label, long uid, MessageState messageState) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_STATE, messageState.getValue());

        return updateMessage(context, email, label, uid, contentValues);
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
    public int setSeenStatusForLocalMessage(Context context, String email, String label, long uid) {
        ContentResolver contentResolver = context.getContentResolver();
        if (email != null && label != null && contentResolver != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL_FLAGS, MessageFlag.SEEN);
            return contentResolver.update(getBaseContentUri(), contentValues,
                    COL_EMAIL + "= ? AND "
                            + COL_FOLDER + " = ? AND "
                            + COL_UID + " = ? ", new String[]{email, label, String.valueOf(uid)});
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
    public int setOldStatusForLocalMessages(Context context, String email, String label) {
        ContentResolver contentResolver = context.getContentResolver();
        if (email != null && label != null && contentResolver != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL_IS_NEW, false);
            return contentResolver.update(getBaseContentUri(), contentValues,
                    COL_EMAIL + "= ? AND " + COL_FOLDER + " = ?", new String[]{email, label});
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
    public int setOldStatusForLocalMessages(Context context, String email, String label, List<String> uidList) {
        ContentResolver contentResolver = context.getContentResolver();
        if (contentResolver != null && email != null && label != null && uidList != null && !uidList.isEmpty()) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL_IS_NEW, false);

            List<String> args = new ArrayList<>();
            args.add(0, email);
            args.add(1, label);
            args.addAll(uidList);

            return contentResolver.update(getBaseContentUri(), contentValues,
                    COL_EMAIL + "= ? AND "
                            + COL_FOLDER + " = ? AND "
                            + COL_UID + " IN (" + TextUtils.join(",", Collections.nCopies(uidList.size(), "?")) + ")",
                    args.toArray(new String[]{}));
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
    public int updateFlagsForLocalMessage(Context context, String email, String label, long uid, @NonNull Flags flags) {
        ContentResolver contentResolver = context.getContentResolver();
        if (email != null && label != null && contentResolver != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL_FLAGS, flags.toString().toUpperCase());
            if (flags.contains(Flags.Flag.SEEN)) {
                contentValues.put(COL_IS_NEW, false);
            }
            return contentResolver.update(getBaseContentUri(), contentValues,
                    COL_EMAIL + "= ? AND "
                            + COL_FOLDER + " = ? AND "
                            + COL_UID + " = ? ", new String[]{email, label, String.valueOf(uid)});
        } else return -1;
    }

    /**
     * Generate a {@link Folder} object from the current cursor position.
     *
     * @param cursor The {@link Cursor} which contains information about {@link Folder}.
     * @return A generated {@link Folder}.
     */
    public GeneralMessageDetails getMessageInfo(Cursor cursor) {
        GeneralMessageDetails generalMessageDetails = new GeneralMessageDetails();

        generalMessageDetails.setEmail(cursor.getString(cursor.getColumnIndex(COL_EMAIL)));
        generalMessageDetails.setLabel(cursor.getString(cursor.getColumnIndex(COL_FOLDER)));
        generalMessageDetails.setUid(cursor.getInt(cursor.getColumnIndex(COL_UID)));
        generalMessageDetails.setReceivedDateInMillisecond(
                cursor.getLong(cursor.getColumnIndex(COL_RECEIVED_DATE)));
        generalMessageDetails.setSentDateInMillisecond(
                cursor.getLong(cursor.getColumnIndex(COL_SENT_DATE)));
        generalMessageDetails.setSubject(cursor.getString(cursor.getColumnIndex(COL_SUBJECT)));
        generalMessageDetails.setFlags(parseFlags(cursor.getString(cursor.getColumnIndex
                (COL_FLAGS))));
        generalMessageDetails.setRawMessageWithoutAttachments(
                cursor.getString(cursor.getColumnIndex(COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS)));
        generalMessageDetails.setMessageHasAttachment(cursor.getInt(cursor.getColumnIndex
                (COL_IS_MESSAGE_HAS_ATTACHMENTS)) == 1);
        generalMessageDetails.setEncrypted(cursor.getInt(cursor.getColumnIndex
                (COL_IS_ENCRYPTED)) == 1);

        generalMessageDetails.setMessageState(MessageState.generate(cursor.getInt(cursor.getColumnIndex(COL_STATE))));

        try {
            String fromAddresses = cursor.getString(cursor.getColumnIndex(COL_FROM_ADDRESSES));
            generalMessageDetails.setFrom(TextUtils.isEmpty(fromAddresses) ? null
                    : InternetAddress.parse(fromAddresses));
        } catch (AddressException e) {
            e.printStackTrace();
        }

        try {
            String toAddresses = cursor.getString(cursor.getColumnIndex(COL_TO_ADDRESSES));
            generalMessageDetails.setTo(TextUtils.isEmpty(toAddresses) ? null : InternetAddress.parse(toAddresses));
        } catch (AddressException e) {
            e.printStackTrace();
        }

        try {
            String ccAddresses = cursor.getString(cursor.getColumnIndex(COL_CC_ADDRESSES));
            generalMessageDetails.setCc(TextUtils.isEmpty(ccAddresses) ? null : InternetAddress.parse(ccAddresses));
        } catch (AddressException e) {
            e.printStackTrace();
        }

        generalMessageDetails.setAttachmentsDirectory(cursor.getString(cursor.getColumnIndex
                (COL_ATTACHMENTS_DIRECTORY)));

        return generalMessageDetails;
    }

    /**
     * Get all messages of some folder.
     *
     * @param context Interface to global information about an application environment.
     * @param email   The email of the {@link Folder}.
     * @param label   The label name.
     * @return A  list of {@link GeneralMessageDetails} objects.
     */
    public List<GeneralMessageDetails> getMessages(Context context, String email, String label) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(getBaseContentUri(),
                null, COL_EMAIL + "= ? AND "
                        + COL_FOLDER + " = ?", new String[]{email, label}, null);

        List<GeneralMessageDetails> generalMessageDetailsList = new ArrayList<>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                generalMessageDetailsList.add(getMessageInfo(cursor));
            }
            cursor.close();
        }

        return generalMessageDetailsList;
    }

    /**
     * Get all messages of the outbox folder.
     *
     * @param context      Interface to global information about an application environment.
     * @param email        The email of the {@link Folder}.
     * @param messageState The message state which will be used for filter results.
     * @return A  list of {@link GeneralMessageDetails} objects.
     */
    public List<GeneralMessageDetails> getOutboxMessages(Context context, String email, MessageState messageState) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(getBaseContentUri(),
                null, COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_STATE + " = ?",
                new String[]{email, JavaEmailConstants.FOLDER_OUTBOX, String.valueOf(messageState.getValue())}, null);

        List<GeneralMessageDetails> generalMessageDetailsList = new ArrayList<>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                generalMessageDetailsList.add(getMessageInfo(cursor));
            }
            cursor.close();
        }

        return generalMessageDetailsList;
    }

    /**
     * Get all messages of the outbox folder which are not sent.
     *
     * @param context Interface to global information about an application environment.
     * @param email   The email of the {@link Folder}.
     * @return A  list of {@link GeneralMessageDetails} objects.
     */
    public List<GeneralMessageDetails> getOutboxMessages(Context context, String email) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(getBaseContentUri(),
                null, COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_STATE + " NOT IN (?, ?)",
                new String[]{email, JavaEmailConstants.FOLDER_OUTBOX, String.valueOf(MessageState.SENT.getValue()),
                        String.valueOf(MessageState.SENT_WITHOUT_LOCAL_COPY.getValue())},
                null);

        List<GeneralMessageDetails> generalMessageDetailsList = new ArrayList<>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                generalMessageDetailsList.add(getMessageInfo(cursor));
            }
            cursor.close();
        }

        return generalMessageDetailsList;
    }

    /**
     * Get new messages.
     *
     * @param context Interface to global information about an application environment.
     * @param email   The user email.
     * @param label   The label name.
     * @return A  list of {@link GeneralMessageDetails} objects.
     */
    public List<GeneralMessageDetails> getNewMessages(Context context, String email, String label) {
        ContentResolver contentResolver = context.getContentResolver();

        String orderType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            orderType = "ASC";
        } else {
            orderType = "DESC";
        }

        Cursor cursor = contentResolver.query(getBaseContentUri(),
                null, COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_IS_NEW + " = 1 ",
                new String[]{email, label}, COL_RECEIVED_DATE + " " + orderType);

        List<GeneralMessageDetails> generalMessageDetailsList = new ArrayList<>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                generalMessageDetailsList.add(getMessageInfo(cursor));
            }
            cursor.close();
        }

        return generalMessageDetailsList;
    }

    /**
     * Get all {@link Folder} objects from the database by an email.
     *
     * @param context Interface to global information about an application environment.
     * @param email   The user email.
     * @param label   The label name.
     * @param uid     The uid of the message.
     * @return A  list of {@link Folder} objects.
     */
    public GeneralMessageDetails getMessage(Context context, String email, String label, int uid) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(getBaseContentUri(),
                null, COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID + " = ? ",
                new String[]{email, label, String.valueOf(uid)}, null);

        GeneralMessageDetails generalMessageDetails = null;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                generalMessageDetails = getMessageInfo(cursor);
            }
            cursor.close();
        }

        return generalMessageDetails;
    }

    /**
     * Check is the message exists in the local database.
     *
     * @param context Interface to global information about an application environment.
     * @param uid     The UID of the message.
     * @return true if message exists in the database, false otherwise.
     */
    public boolean isMessageExistsInDatabase(Context context, long uid) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(getBaseContentUri(),
                null, COL_UID + " = ?", new String[]{String.valueOf(uid)}, null);

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
    public int getMinUIDforLabel(Context context, String email, String label) {
        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = contentResolver.query(getBaseContentUri(),
                new String[]{"min(" + COL_UID + ")"}, COL_EMAIL + " = ? AND " + MessageDaoSource.COL_FOLDER + " = ?",
                new String[]{email, label}, null);

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
    public int getLastUIDOfMessageInLabel(Context context, String email, String label) {
        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = contentResolver.query(getBaseContentUri(),
                new String[]{"max(" + COL_UID + ")"}, COL_EMAIL + " = ? AND " + MessageDaoSource.COL_FOLDER + " = ?",
                new String[]{email, label}, null);

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
    public int getOldestUIDOfMessageInLabel(Context context, String email, String label) {
        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = contentResolver.query(getBaseContentUri(),
                new String[]{"min(" + COL_UID + ")"}, COL_EMAIL + " = ? AND " + MessageDaoSource.COL_FOLDER + " = ?",
                new String[]{email, label}, null);

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
    public List<String> getUIDsOfMessagesInLabel(Context context, String email, String label) {
        ContentResolver contentResolver = context.getContentResolver();
        List<String> uidList = new ArrayList<>();

        Cursor cursor = contentResolver.query(getBaseContentUri(),
                new String[]{COL_UID}, COL_EMAIL + " = ? AND " + COL_FOLDER + " = ?",
                new String[]{email, label}, null);

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
    public List<Long> getUIDsOfMessagesWhichWereNotCheckedToEncryption(Context context, String email, String label) {
        ContentResolver contentResolver = context.getContentResolver();
        List<Long> uidList = new ArrayList<>();

        Cursor cursor = contentResolver.query(getBaseContentUri(),
                new String[]{COL_UID}, COL_EMAIL + " = ? AND " + COL_FOLDER + " = ?" + " AND "
                        + COL_IS_ENCRYPTED + " = " + ENCRYPTED_STATE_UNDEFINED,
                new String[]{email, label},
                null);

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
    public Map<Long, String> getMapOfUIDAndMessagesFlags(Context context, String email, String label) {
        ContentResolver contentResolver = context.getContentResolver();
        Map<Long, String> uidList = new HashMap<>();

        Cursor cursor = contentResolver.query(getBaseContentUri(),
                new String[]{COL_UID, COL_FLAGS}, COL_EMAIL + " = ? AND " + COL_FOLDER + " = ?",
                new String[]{email, label}, null);

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
    public List<Integer> getUIDOfUnseenMessages(Context context, String email, String label) {
        ContentResolver contentResolver = context.getContentResolver();
        List<Integer> uidList = new ArrayList<>();

        Cursor cursor = contentResolver.query(getBaseContentUri(),
                new String[]{COL_UID}, COL_EMAIL + " = ? AND " + COL_FOLDER + " = ? AND "
                        + COL_FLAGS + " NOT LIKE '%" + MessageFlag.SEEN + "%'",
                new String[]{email, label}, null);

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
    public int getCountOfMessagesForLabel(Context context, String email, String label) {
        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = contentResolver.query(getBaseContentUri(),
                new String[]{FlowCryptSQLiteOpenHelper.COLUMN_NAME_COUNT},
                COL_EMAIL + " = ? AND " + MessageDaoSource.COL_FOLDER + " = ?",
                new String[]{email, label},
                null);

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
    public int deleteMessageFromFolder(Context context, String email, String label, long uid) {
        ContentResolver contentResolver = context.getContentResolver();
        if (email != null && label != null && contentResolver != null) {
            return contentResolver.delete(getBaseContentUri(), COL_EMAIL + "= ? AND "
                    + COL_FOLDER + " = ? AND " + COL_UID + " = ? ", new String[]{email, label, String.valueOf(uid)});
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
    public int deleteCachedMessagesOfFolder(Context context, String email, String label) {
        ContentResolver contentResolver = context.getContentResolver();
        if (email != null && label != null && contentResolver != null) {
            return contentResolver.delete(getBaseContentUri(), COL_EMAIL + "= ? AND "
                    + COL_FOLDER + " = ?", new String[]{email, label});
        } else return -1;
    }

    /**
     * @param context                Interface to global information about an application environment.
     * @param email                  The email that the message linked.
     * @param label                  The folder label.
     * @param booleanLongSparseArray The array which contains information about an encrypted state of some messages
     * @return the {@link ContentProviderResult} array.
     * @throws RemoteException
     * @throws OperationApplicationException
     */
    public ContentProviderResult[] updateMessagesEncryptionStateByUID(Context context, String email, String label,
                                                                      LongSparseArray<Boolean> booleanLongSparseArray)
            throws RemoteException, OperationApplicationException {
        ContentResolver contentResolver = context.getContentResolver();

        if (booleanLongSparseArray != null && booleanLongSparseArray.size() > 0) {
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            for (int i = 0, arraySize = booleanLongSparseArray.size(); i < arraySize; i++) {
                long uid = booleanLongSparseArray.keyAt(i);
                Boolean b = booleanLongSparseArray.get(uid);
                ops.add(ContentProviderOperation.newUpdate(getBaseContentUri())
                        .withValue(COL_IS_ENCRYPTED, b)
                        .withSelection(COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND " + COL_UID + " = ? ",
                                new String[]{email, label, String.valueOf(uid)})
                        .withYieldAllowed(true)
                        .build());
            }
            return contentResolver.applyBatch(getBaseContentUri().getAuthority(), ops);
        } else return new ContentProviderResult[0];
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
     * Delete an outgoing message.
     *
     * @param context               Interface to global information about an application environment.
     * @param generalMessageDetails Input details about the outgoing message.
     * @return The number of rows deleted.
     */
    public int deleteOutgoingMessage(Context context, GeneralMessageDetails generalMessageDetails) {
        int deletedRows;
        ContentResolver contentResolver = context.getContentResolver();

        if (generalMessageDetails.getEmail() != null && generalMessageDetails.getLabel() != null && contentResolver
                != null) {
            deletedRows = contentResolver.delete(getBaseContentUri(), COL_EMAIL + "= ? AND " + COL_FOLDER + " = ? AND" +
                            " " + COL_UID + " = ? AND " + COL_STATE + " != " + MessageState.SENDING.getValue(),
                    new String[]{generalMessageDetails.getEmail(), generalMessageDetails.getLabel(),
                            String.valueOf(generalMessageDetails.getUid())});
        } else {
            deletedRows = -1;
        }

        if (deletedRows > 0) {
            new ImapLabelsDaoSource().updateLabelMessageCount(context, generalMessageDetails.getEmail(),
                    JavaEmailConstants.FOLDER_OUTBOX, new MessageDaoSource().getOutboxMessages(context,
                            generalMessageDetails.getEmail()).size());

            if (generalMessageDetails.isMessageHasAttachment()) {
                AttachmentDaoSource attachmentDaoSource = new AttachmentDaoSource();

                List<AttachmentInfo> attachmentInfoList =
                        attachmentDaoSource.getAttachmentInfoList(context, generalMessageDetails.getEmail(),
                                JavaEmailConstants.FOLDER_OUTBOX, generalMessageDetails.getUid());

                if (!CollectionUtils.isEmpty(attachmentInfoList)) {
                    new AttachmentDaoSource().deleteAttachments(context, generalMessageDetails.getEmail(),
                            generalMessageDetails.getLabel(), generalMessageDetails.getUid());

                    if (!TextUtils.isEmpty(generalMessageDetails.getAttachmentsDirectory())) {
                        try {
                            FileAndDirectoryUtils.deleteDirectory(new File(new File(context.getCacheDir(),
                                    Constants.ATTACHMENTS_CACHE_DIR), generalMessageDetails.getAttachmentsDirectory()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        return deletedRows;
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
    private static boolean isMessageHasAttachment(Part part) {
        try {
            if (part.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
                Multipart multiPart = (Multipart) part.getContent();
                int numberOfParts = multiPart.getCount();
                for (int partCount = 0; partCount < numberOfParts; partCount++) {
                    BodyPart bodyPart = multiPart.getBodyPart(partCount);
                    if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
                        boolean isMessageHasAttachment = isMessageHasAttachment(bodyPart);
                        if (isMessageHasAttachment) {
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
