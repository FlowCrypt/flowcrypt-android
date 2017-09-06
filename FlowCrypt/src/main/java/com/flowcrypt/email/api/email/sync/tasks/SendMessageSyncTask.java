/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.content.Context;
import android.os.Messenger;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.Attachment;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.security.SecurityStorageConnector;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

/**
 * This task does job of sending a message.
 *
 * @author DenBond7
 *         Date: 29.06.2017
 *         Time: 11:38
 *         E-mail: DenBond7@gmail.com
 */

public class SendMessageSyncTask extends BaseSyncTask {
    private OutgoingMessageInfo outgoingMessageInfo;

    /**
     * The base constructor.
     *
     * @param ownerKey            The name of the reply to {@link Messenger}.
     * @param requestCode         The unique request code for the reply to {@link Messenger}.
     * @param outgoingMessageInfo The {@link OutgoingMessageInfo} which contains an information about an outgoing
     *                            message.
     */
    public SendMessageSyncTask(String ownerKey, int requestCode, OutgoingMessageInfo outgoingMessageInfo) {
        super(ownerKey, requestCode);
        this.outgoingMessageInfo = outgoingMessageInfo;
    }

    @Override
    public boolean isUseSMTP() {
        return true;
    }

    @Override
    public void run(Session session, String userName, String password, SyncListener syncListener)
            throws Exception {
        super.run(session, userName, password, syncListener);

        if (syncListener != null) {
            Context context = syncListener.getContext();

            updateContactsLastUseDateTime(context);

            String rawMessage = generateRawMimeMessage(context);

            MimeMessage mimeMessage = new MimeMessage(session,
                    IOUtils.toInputStream(rawMessage, StandardCharsets.UTF_8));

            Transport transport = session.getTransport(JavaEmailConstants.PROTOCOL_SMTP);
            transport.connect(GmailConstants.HOST_SMTP_GMAIL_COM,
                    GmailConstants.PORT_SMTP_GMAIL_COM, userName, password);

            transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
            syncListener.onEncryptedMessageSent(ownerKey, requestCode, true);
        }
    }

    /**
     * Update the {@link ContactsDaoSource#COL_LAST_USE} field in the {@link ContactsDaoSource#TABLE_NAME_CONTACTS}.
     *
     * @param context - Interface to global information about an application environment.
     */
    private void updateContactsLastUseDateTime(Context context) {
        ContactsDaoSource contactsDaoSource = new ContactsDaoSource();

        for (PgpContact pgpContact : outgoingMessageInfo.getToPgpContacts()) {
            contactsDaoSource.updateLastUseOfPgpContact(context, pgpContact);
        }
    }

    /**
     * @param context - Interface to global information about an application environment.
     * @return The raw MIME message as {@link String}.
     * @throws IOException Errors can be happened while we generate a MIME message
     *                     ({@link java.io.FileNotFoundException} for attachments ans etc.)
     */
    private String generateRawMimeMessage(Context context) throws IOException {
        Js js = new Js(context, new SecurityStorageConnector(context));
        String[] pubKeys = getPubKeys(js, context);

        String messageText = null;
        ArrayList<Attachment> attachments = new ArrayList<>();
        ArrayList<AttachmentInfo> attachmentInfoArrayList = outgoingMessageInfo.getAttachmentInfoArrayList();

        switch (outgoingMessageInfo.getMessageEncryptionType()) {
            case ENCRYPTED:
                messageText = js.crypto_message_encrypt(pubKeys, outgoingMessageInfo.getMessage());
                for (int i = 0; i < attachmentInfoArrayList.size(); i++) {
                    AttachmentInfo attachmentInfo = attachmentInfoArrayList.get(i);
                    InputStream inputStream = context.getContentResolver().openInputStream(attachmentInfo.getUri());
                    if (inputStream != null) {
                        attachments.add(js.file_attachment(
                                js.crypto_message_encrypt(pubKeys, IOUtils.toByteArray(inputStream),
                                        attachmentInfo.getName()),
                                attachmentInfo.getName() + ".pgp",
                                attachmentInfo.getType()));
                    }
                }
                break;

            case STANDARD:
                messageText = outgoingMessageInfo.getMessage();
                for (int i = 0; i < attachmentInfoArrayList.size(); i++) {
                    AttachmentInfo attachmentInfo = attachmentInfoArrayList.get(i);
                    InputStream inputStream = context.getContentResolver().openInputStream(attachmentInfo.getUri());

                    if (inputStream != null) {
                        byte[] content = IOUtils.toByteArray(inputStream);
                        attachments.add(js.file_attachment(content, attachmentInfo.getName(),
                                attachmentInfo.getType()));
                    }
                }
                break;
        }

        return js.mime_encode(messageText,
                outgoingMessageInfo.getToPgpContacts(),
                outgoingMessageInfo.getFromPgpContact(),
                outgoingMessageInfo.getSubject(),
                attachments.toArray(new Attachment[0]),
                js.mime_decode(outgoingMessageInfo.getRawReplyMessage()));
    }

    /**
     * Get public keys for recipients + keys of the sender;
     *
     * @param js      - {@link Js} util class.
     * @param context - Interface to global information about an application environment.
     * @return <tt>String[]</tt> An array of public keys.
     */
    private String[] getPubKeys(Js js, Context context) {
        ArrayList<String> publicKeys = new ArrayList<>();
        for (PgpContact pgpContact : outgoingMessageInfo.getToPgpContacts()) {
            if (!TextUtils.isEmpty(pgpContact.getPubkey())) {
                publicKeys.add(pgpContact.getPubkey());
            }
        }

        publicKeys.addAll(generateOwnPublicKeys(js, context));

        return publicKeys.toArray(new String[0]);
    }

    /**
     * Get public keys of the sender;
     *
     * @param js      - {@link Js} util class.
     * @param context - Interface to global information about an application environment.
     * @return <tt>String[]</tt> An array of the sender public keys.
     */
    private ArrayList<String> generateOwnPublicKeys(Js js, Context context) {
        ArrayList<String> publicKeys = new ArrayList<>();

        SecurityStorageConnector securityStorageConnector = new SecurityStorageConnector(context);
        PgpKeyInfo[] pgpKeyInfoArray = securityStorageConnector.getAllPgpPrivateKeys();

        for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoArray) {
            PgpKey pgpKey = js.crypto_key_read(pgpKeyInfo.getArmored());
            publicKeys.add(pgpKey.toPublic().armor());
        }

        return publicKeys;
    }
}
