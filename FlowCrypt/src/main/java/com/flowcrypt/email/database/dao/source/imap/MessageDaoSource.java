/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
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
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;

import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.MessageFlag;
import com.flowcrypt.email.database.FlowCryptSQLiteOpenHelper;
import com.flowcrypt.email.database.dao.source.BaseDaoSource;
import com.sun.mail.imap.IMAPFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
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
    public static final String COL_SUBJECT = "subject";
    public static final String COL_FLAGS = "flags";
    public static final String COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS =
            "raw_message_without_attachments";
    public static final String COL_IS_MESSAGE_HAS_ATTACHMENTS = "is_message_has_attachments";

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
            COL_SUBJECT + " TEXT DEFAULT NULL, " +
            COL_FLAGS + " TEXT DEFAULT NULL, " +
            COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS + " TEXT DEFAULT NULL, " +
            COL_IS_MESSAGE_HAS_ATTACHMENTS + " INTEGER DEFAULT 0 " + ");";

    public static final String CREATE_INDEX_EMAIL_IN_MESSAGES =
            "CREATE INDEX IF NOT EXISTS " + COL_EMAIL + "_in_" + TABLE_NAME_MESSAGES +
                    " ON " + TABLE_NAME_MESSAGES + " (" + COL_EMAIL + ")";

    public static final String CREATE_INDEX_EMAIL_UID_FOLDER_IN_MESSAGES =
            "CREATE UNIQUE INDEX IF NOT EXISTS " + COL_EMAIL + "_" + COL_UID + "_" + COL_FOLDER
                    + "_in_" + TABLE_NAME_MESSAGES +
                    " ON " + TABLE_NAME_MESSAGES +
                    " (" + COL_EMAIL + ", " + COL_UID + ", " + COL_FOLDER + ")";

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
     * @return A {@link Uri} of the created row.
     */
    public Uri addRow(Context context, String email, String label, long uid, Message message)
            throws MessagingException, IOException {
        ContentResolver contentResolver = context.getContentResolver();
        if (message != null && label != null && contentResolver != null) {
            ContentValues contentValues = prepareContentValues(email, label, message, uid);
            return contentResolver.insert(getBaseContentUri(), contentValues);
        } else return null;
    }

    /**
     * This method add rows per single transaction. This method must be called in the non-UI thread.
     *
     * @param context    Interface to global information about an application environment.
     * @param email      The email that the message linked.
     * @param label      The folder label.
     * @param imapFolder The {@link IMAPFolder} object which contains information about a
     *                   remote folder.
     * @param messages   The messages array.
     * @return the number of newly created rows.
     * @throws MessagingException This exception may be occured when we call <code>mapFolder
     *                            .getUID(message)</code>
     */
    public int addRows(Context context, String email, String label, IMAPFolder imapFolder, Message[] messages)
            throws MessagingException, IOException {
        if (messages != null) {
            ContentResolver contentResolver = context.getContentResolver();
            ContentValues[] contentValuesArray = new ContentValues[messages.length];

            for (int i = 0; i < messages.length; i++) {
                Message message = messages[i];
                ContentValues contentValues = prepareContentValues(email, label,
                        message, imapFolder.getUID(message));

                contentValuesArray[i] = contentValues;
            }

            return contentResolver.bulkInsert(getBaseContentUri(), contentValuesArray);
        } else return 0;
    }

    /**
     * This method delete cached messages.
     *
     * @param context     Interface to global information about an application environment.
     * @param email       The email that the message linked.
     * @param label       The folder label.
     * @param messagesUID The list of messages UID.
     * @return the number of deleted rows.
     */
    public int deleteMessagesByUID(Context context, String email, String label, Collection<Long> messagesUID) {
        ContentResolver contentResolver = context.getContentResolver();
        if (email != null && label != null && contentResolver != null) {
            List<String> selectionArgs = new LinkedList<>();
            selectionArgs.add(email);
            selectionArgs.add(label);

            for (Long uid : messagesUID) {
                selectionArgs.add(String.valueOf(uid));
            }

            return contentResolver.delete(getBaseContentUri(), COL_EMAIL + "= ? AND "
                            + COL_FOLDER + " = ? AND "
                            + COL_UID + " IN (" + prepareSelectionArgsString(messagesUID.toArray()) + ");",
                    selectionArgs.toArray(new String[0]));
        } else return -1;
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
            throws IOException, RemoteException, OperationApplicationException, MessagingException {
        ContentResolver contentResolver = context.getContentResolver();
        if (email != null && label != null && contentResolver != null && messages != null && messages.length > 0) {

            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            for (Message message : messages) {
                ops.add(ContentProviderOperation.newUpdate(getBaseContentUri())
                        .withValue(COL_FLAGS, message.getFlags().toString().toUpperCase())
                        .withSelection(COL_EMAIL + "= ? AND "
                                        + COL_FOLDER + " = ? AND "
                                        + COL_UID + " = ? ",
                                new String[]{email, label, String.valueOf(imapFolder.getUID(message))})
                        .withYieldAllowed(true)
                        .build());
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
        generalMessageDetails.setFrom(
                parseEmails(cursor.getString(cursor.getColumnIndex(COL_FROM_ADDRESSES))));
        generalMessageDetails.setTo(
                parseEmails(cursor.getString(cursor.getColumnIndex(COL_TO_ADDRESSES))));
        generalMessageDetails.setSubject(cursor.getString(cursor.getColumnIndex(COL_SUBJECT)));
        generalMessageDetails.setFlags(parseFlags(cursor.getString(cursor.getColumnIndex
                (COL_FLAGS))));
        generalMessageDetails.setRawMessageWithoutAttachments(
                cursor.getString(cursor.getColumnIndex(COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS)));
        generalMessageDetails.setMessageHasAttachment(cursor.getInt(cursor.getColumnIndex
                (COL_IS_MESSAGE_HAS_ATTACHMENTS)) == 1);

        return generalMessageDetails;
    }

    /**
     * Get all {@link Folder} objects from the database by an email.
     *
     * @param email The email of the {@link Folder}.
     * @return A  list of {@link Folder} objects.
     */
    public List<GeneralMessageDetails> getMessages(Context context, String email) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(getBaseContentUri(),
                null, COL_EMAIL + " = ?", new String[]{email}, null);

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
                null, MessageDaoSource.COL_EMAIL + "= ? AND "
                        + MessageDaoSource.COL_FOLDER + " = ? AND "
                        + MessageDaoSource.COL_UID + " = ? ",
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

        Cursor cursor = contentResolver.query(
                getBaseContentUri(),
                new String[]{"min(" + COL_UID + ")"},
                MessageDaoSource.COL_EMAIL + " = ? AND " + MessageDaoSource
                        .COL_FOLDER + " = ?",
                new String[]{email, label},
                null);

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

        Cursor cursor = contentResolver.query(
                getBaseContentUri(),
                new String[]{"max(" + COL_UID + ")"},
                MessageDaoSource.COL_EMAIL + " = ? AND " + MessageDaoSource
                        .COL_FOLDER + " = ?",
                new String[]{email, label},
                null);

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

        Cursor cursor = contentResolver.query(
                getBaseContentUri(),
                new String[]{COL_UID}, MessageDaoSource.COL_EMAIL + " = ? AND "
                        + MessageDaoSource.COL_FOLDER + " = ?",
                new String[]{email, label},
                null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                uidList.add(cursor.getString(cursor.getColumnIndex(COL_UID)));
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

        Cursor cursor = contentResolver.query(
                getBaseContentUri(),
                new String[]{COL_UID, COL_FLAGS},
                MessageDaoSource.COL_EMAIL + " = ? AND " + MessageDaoSource.COL_FOLDER + " = ?",
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
     * Get the count of messages in the database for some label.
     *
     * @param context Interface to global information about an application environment.
     * @param email   The user email.
     * @param label   The label name.
     * @return The count of messages for the current label.
     */
    public int getCountOfMessagesForLabel(Context context, String email, String label) {
        ContentResolver contentResolver = context.getContentResolver();

        Cursor cursor = contentResolver.query(
                getBaseContentUri(),
                new String[]{FlowCryptSQLiteOpenHelper.COLUMN_NAME_COUNT},
                MessageDaoSource.COL_EMAIL + " = ? AND " + MessageDaoSource
                        .COL_FOLDER + " = ?",
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
                    + COL_FOLDER + " = ? AND "
                    + COL_UID + " = ? ", new String[]{email, label, String.valueOf(uid)});
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

    private static String prepareArrayToSaving(String[] attributes) {
        if (attributes != null && attributes.length > 0) {
            StringBuilder result = new StringBuilder();
            for (String attribute : attributes) {
                result.append(attribute).append("\t");
            }

            return result.toString();
        } else {
            return null;
        }
    }

    private static String[] parseArray(String attributesAsString) {
        return parseArray(attributesAsString, "\t");
    }

    private static String[] parseArray(String attributesAsString, String regex) {
        if (attributesAsString != null && attributesAsString.length() > 0) {
            return attributesAsString.split(regex);
        } else {
            return null;
        }
    }

    private static String getStringEquivalentForFlag(Flags.Flag flag) {
        if (flag == Flags.Flag.ANSWERED) {
            return MessageFlag.ANSWERED;
        }

        if (flag == Flags.Flag.DELETED) {
            return MessageFlag.DELETED;
        }

        if (flag == Flags.Flag.DRAFT) {
            return MessageFlag.DRAFT;
        }

        if (flag == Flags.Flag.FLAGGED) {
            return MessageFlag.FLAGGED;
        }

        if (flag == Flags.Flag.RECENT) {
            return MessageFlag.RECENT;
        }

        if (flag == Flags.Flag.SEEN) {
            return MessageFlag.SEEN;
        }
        return null;
    }

    /**
     * Prepare the content values for insert to the database. This method must be called in the
     * non-UI thread.
     *
     * @param email   The email that the message linked.
     * @param label   The folder label.
     * @param message The message which will be added to the database.
     * @param uid     The message UID.
     * @return generated {@link ContentValues}
     * @throws MessagingException This exception may be occured when we call methods of thr
     *                            {@link Message} object</code>
     */
    @NonNull
    private ContentValues prepareContentValues(String email, String label, Message message, long
            uid) throws MessagingException, IOException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_EMAIL, email);
        contentValues.put(COL_FOLDER, label);
        contentValues.put(COL_UID, uid);
        contentValues.put(COL_RECEIVED_DATE, message.getReceivedDate().getTime());
        if (message.getSentDate() != null) {
            contentValues.put(COL_SENT_DATE, message.getSentDate().getTime());
        }
        contentValues.put(COL_FROM_ADDRESSES, prepareAddressesForSaving(message.getFrom()));
        contentValues.put(COL_TO_ADDRESSES,
                prepareAddressesForSaving(message.getRecipients(Message.RecipientType.TO)));
        contentValues.put(COL_SUBJECT, message.getSubject());
        contentValues.put(COL_FLAGS, message.getFlags().toString().toUpperCase());
        contentValues.put(COL_IS_MESSAGE_HAS_ATTACHMENTS, isMessageHasAttachment(message));
        return contentValues;
    }

    private String prepareFlagsToSave(Flags flags) {
        if (flags != null) {
            Flags.Flag[] systemFlags = flags.getSystemFlags();

            String[] stringsOfFlags = new String[systemFlags.length];

            for (int i = 0; i < systemFlags.length; i++) {
                stringsOfFlags[i] = getStringEquivalentForFlag(systemFlags[i]);
            }

            return prepareArrayToSaving(stringsOfFlags);
        }
        return null;
    }

    private String prepareAddressesForSaving(Address[] from) {
        if (from != null) {
            String[] emails = new String[from.length];

            for (int i = 0; i < from.length; i++) {
                InternetAddress internetAddress = (InternetAddress) from[i];
                emails[i] = internetAddress.getAddress();
            }

            return prepareArrayToSaving(emails);
        }
        return null;
    }

    private String[] parseFlags(String string) {
        return parseArray(string, "(?=\\\\)");
    }

    private String[] parseEmails(String string) {
        return parseArray(string);
    }

    /**
     * Check is {@link Part} has attachment.
     *
     * @param part The parent part.
     * @return <tt>boolean</tt> true if {@link Part} has attachment, false otherwise.
     * @throws MessagingException
     * @throws IOException
     */
    private boolean isMessageHasAttachment(Part part)
            throws MessagingException, IOException {
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
    }
}
