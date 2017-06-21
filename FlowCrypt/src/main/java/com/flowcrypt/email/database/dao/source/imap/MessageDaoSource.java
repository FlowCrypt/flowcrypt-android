/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao.source.imap;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.flowcrypt.email.api.email.Folder;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.MessageFlag;
import com.flowcrypt.email.database.dao.source.BaseDaoSource;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
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
    public static final String LABEL_MARKER = "  ";
    public static final String TABLE_NAME_MESSAGES = "messages";

    public static final String COL_EMAIL = "email";
    public static final String COL_LABELS = "labels";
    public static final String COL_UID = "uid";
    public static final String COL_RECEIVED_DATE = "received_date";
    public static final String COL_SENT_DATE = "sent_date";
    public static final String COL_FROM_ADDRESSES = "from_address";
    public static final String COL_TO_ADDRESSES = "to_address";
    public static final String COL_SUBJECT = "subject";
    public static final String COL_FLAGS = "flags";

    public static final String IMAP_MESSAGES_INFO_TABLE_SQL_CREATE = "CREATE TABLE IF NOT EXISTS " +
            TABLE_NAME_MESSAGES + " (" +
            BaseColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_EMAIL + " VARCHAR(100) NOT NULL, " +
            COL_LABELS + " TEXT NOT NULL, " +
            COL_UID + " INTEGER NOT NULL, " +
            COL_RECEIVED_DATE + " INTEGER DEFAULT NULL, " +
            COL_SENT_DATE + " INTEGER DEFAULT NULL, " +
            COL_FROM_ADDRESSES + " TEXT DEFAULT NULL, " +
            COL_TO_ADDRESSES + " TEXT DEFAULT NULL, " +
            COL_SUBJECT + " TEXT DEFAULT NULL, " +
            COL_FLAGS + " TEXT DEFAULT NULL " + ");";

    public static final String CREATE_INDEX_EMAIL_IN_MESSAGES =
            "CREATE INDEX IF NOT EXISTS " + COL_EMAIL + "_in_" + TABLE_NAME_MESSAGES +
                    " ON " + TABLE_NAME_MESSAGES + " (" + COL_EMAIL + ")";

    public static final String CREATE_INDEX_UID_IN_MESSAGES =
            "CREATE INDEX IF NOT EXISTS " + COL_UID + "_in_" + TABLE_NAME_MESSAGES +
                    " ON " + TABLE_NAME_MESSAGES + " (" + COL_UID + ")";

    @Override
    public String getTableName() {
        return TABLE_NAME_MESSAGES;
    }

    /**
     * Add a new message details to the database. Must be called on non-UI thread.
     *
     * @param context Interface to global information about an application environment.
     * @param email   The email that the message linked.
     * @param label   The folder label.
     * @param uid     The message UID.
     * @param message The message which will be added to the database.
     * @return A {@link Uri} of the created row.
     */
    public Uri addRow(Context context, String email, String label, long uid, Message message)
            throws MessagingException {
        ContentResolver contentResolver = context.getContentResolver();
        if (message != null && label != null && contentResolver != null) {
            ContentValues contentValues = new ContentValues();
            contentValues.put(COL_EMAIL, email);
            contentValues.put(COL_LABELS, label + LABEL_MARKER);
            contentValues.put(COL_UID, uid);
            contentValues.put(COL_RECEIVED_DATE, message.getReceivedDate().getTime());
            contentValues.put(COL_SENT_DATE, message.getSentDate().getTime());
            contentValues.put(COL_FROM_ADDRESSES, prepareAddressesForSaving(message.getFrom()));
            contentValues.put(COL_TO_ADDRESSES, prepareAddressesForSaving(message.getReplyTo()));
            contentValues.put(COL_SUBJECT, message.getSubject());
            contentValues.put(COL_FLAGS, prepareFlagsToSave(message.getFlags()));
            return contentResolver.insert(getBaseContentUri(), contentValues);
        } else return null;
    }

    /**
     * Generate a {@link Folder} object from the current cursor position.
     *
     * @param cursor The {@link Cursor} which contains information about {@link Folder}.
     * @return A generated {@link Folder}.
     */
    public GeneralMessageDetails getMessageInfo(Cursor cursor) {
        GeneralMessageDetails generalMessageDetails = new GeneralMessageDetails();

        generalMessageDetails.setEmail(cursor.getString(cursor.getColumnIndex(COL_LABELS)));
        generalMessageDetails.setLabels(
                parseLabels(cursor.getString(cursor.getColumnIndex(COL_LABELS))));
        generalMessageDetails.setUid(cursor.getLong(cursor.getColumnIndex(COL_UID)));
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

    private static String prepareArrayToSaving(String[] attributes) {
        if (attributes != null && attributes.length > 0) {
            String result = "";
            for (String attribute : attributes) {
                result += attribute + "\t";
            }

            return result;
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
        return parseArray(string);
    }

    private String[] parseEmails(String string) {
        return parseArray(string);
    }

    private String[] parseLabels(String string) {
        return parseArray(string, LABEL_MARKER);
    }
}
