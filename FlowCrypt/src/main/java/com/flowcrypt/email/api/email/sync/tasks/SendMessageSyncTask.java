/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.content.Context;
import android.net.Uri;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.security.SecurityStorageConnector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * This task does job of sending a message.
 *
 * @author DenBond7
 *         Date: 29.06.2017
 *         Time: 11:38
 *         E-mail: DenBond7@gmail.com
 */

public class SendMessageSyncTask extends BaseSyncTask {
    private static final String TAG = SendMessageSyncTask.class.getSimpleName();
    private static final String PGP_CACHE_DIR = "PGP";

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

            File pgpCacheDirectory = new File(context.getCacheDir(), PGP_CACHE_DIR);
            if (pgpCacheDirectory.exists()) {
                FileUtils.cleanDirectory(pgpCacheDirectory);
            } else if (!pgpCacheDirectory.mkdir()) {
                Log.d(TAG, "Create cache directory " + pgpCacheDirectory.getName() + " filed!");
            }

            updateContactsLastUseDateTime(context);

            MimeMessage mimeMessage = createMimeMessage(session, context, pgpCacheDirectory);

            Transport transport = session.getTransport(JavaEmailConstants.PROTOCOL_SMTP);
            transport.connect(GmailConstants.GMAIL_SMTP_SERVER, GmailConstants.GMAIL_SMTP_PORT,
                    userName, password);

            transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());

            FileUtils.cleanDirectory(pgpCacheDirectory);

            syncListener.onEncryptedMessageSent(ownerKey, requestCode, true);
        }
    }

    /**
     * Create {@link MimeMessage} using different parameters.
     *
     * @param session           Will be used to create {@link MimeMessage}
     * @param context           Interface to global information about an application environment.
     * @param pgpCacheDirectory The cache directory which contains temp files.
     * @return {@link MimeMessage}
     * @throws IOException
     * @throws MessagingException
     */
    @NonNull
    private MimeMessage createMimeMessage(Session session, Context context, File pgpCacheDirectory)
            throws IOException, MessagingException {
        Js js = new Js(context, new SecurityStorageConnector(context));
        String[] pubKeys = getPubKeys(js, context);

        String rawMessage = generateRawMessageWithoutAttachments(js, pubKeys);

        MimeMessage mimeMessage = new MimeMessage(session,
                IOUtils.toInputStream(rawMessage, StandardCharsets.UTF_8));

        if (mimeMessage.getContent() instanceof MimeMultipart) {
            MimeMultipart mimeMultipart = (MimeMultipart) mimeMessage.getContent();

            for (AttachmentInfo attachmentInfo : outgoingMessageInfo.getAttachmentInfoArrayList()) {
                mimeMultipart.addBodyPart(generateBodyPartWithAttachment(context, pgpCacheDirectory,
                        js, pubKeys, attachmentInfo));
            }

            mimeMessage.setContent(mimeMultipart);
            mimeMessage.saveChanges();
        }

        return mimeMessage;
    }

    /**
     * Generate a {@link BodyPart} with attachment.
     *
     * @param context           Interface to global information about an application environment.
     * @param pgpCacheDirectory The cache directory which contains temp files.
     * @param js                The {@link Js} tools.
     * @param pubKeys           The public keys which will be used for generate an encrypted attachments.
     * @param attachmentInfo    The {@link AttachmentInfo} object, which contains general information about attachment.
     * @return Generated {@link BodyPart} with an attachment.
     * @throws IOException
     * @throws MessagingException
     */
    @NonNull
    private BodyPart generateBodyPartWithAttachment(Context context, File pgpCacheDirectory, Js js,
                                                    String[] pubKeys, AttachmentInfo attachmentInfo)
            throws IOException, MessagingException {
        BodyPart attachmentsBodyPart = new MimeBodyPart();
        switch (outgoingMessageInfo.getMessageEncryptionType()) {
            case ENCRYPTED:
                InputStream inputStream =
                        context.getContentResolver().openInputStream(attachmentInfo.getUri());
                if (inputStream != null) {
                    File encryptedTempFile = generateTempFile(pgpCacheDirectory, attachmentInfo.getName());
                    byte[] encryptedBytes = js.crypto_message_encrypt(pubKeys, IOUtils.toByteArray
                            (inputStream), attachmentInfo.getName());
                    FileUtils.writeByteArrayToFile(encryptedTempFile, encryptedBytes);
                    attachmentsBodyPart.setDataHandler(new DataHandler(new FileDataSource(encryptedTempFile)));
                    attachmentsBodyPart.setFileName(encryptedTempFile.getName());
                }
                break;

            case STANDARD:
                attachmentsBodyPart.setDataHandler(new DataHandler(new AttachmentInfoDataSource(context,
                        attachmentInfo)));
                attachmentsBodyPart.setFileName(attachmentInfo.getName());
                break;
        }

        return attachmentsBodyPart;
    }

    /**
     * Generate a raw MIME message using {@link Js} tools.
     *
     * @param js      The {@link Js} tools.
     * @param pubKeys The public keys which will be used for generate an encrypted attachments.
     * @return The generated raw MIME message.
     */
    private String generateRawMessageWithoutAttachments(Js js, String[] pubKeys) {
        String messageText = null;

        switch (outgoingMessageInfo.getMessageEncryptionType()) {
            case ENCRYPTED:
                messageText = js.crypto_message_encrypt(pubKeys, outgoingMessageInfo.getMessage());
                break;

            case STANDARD:
                messageText = outgoingMessageInfo.getMessage();
                break;
        }

        return js.mime_encode(messageText,
                outgoingMessageInfo.getToPgpContacts(),
                outgoingMessageInfo.getFromPgpContact(),
                outgoingMessageInfo.getSubject(),
                null,
                js.mime_decode(outgoingMessageInfo.getRawReplyMessage()));
    }

    /**
     * Generate a temp file for IO operations.
     *
     * @param parentDirectory The parent directory where a new file will be created.
     * @param fileName        The name of an the created file
     * @return Generated {@link File}
     * @throws IOException
     */
    private File generateTempFile(File parentDirectory, String fileName) throws IOException {
        return new File(parentDirectory, fileName + ".pgp");
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

    /**
     * The {@link DataSource} realization for a file which received from {@link Uri}
     */
    private class AttachmentInfoDataSource implements DataSource {
        private AttachmentInfo attachmentInfo;
        private Context context;

        AttachmentInfoDataSource(Context context, AttachmentInfo attachmentInfo) {
            this.attachmentInfo = attachmentInfo;
            this.context = context;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            InputStream inputStream = attachmentInfo.getUri() == null ? null : context.getContentResolver()
                    .openInputStream(attachmentInfo.getUri());

            return inputStream == null ? null : new BufferedInputStream(inputStream);
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return null;
        }

        @Override
        public String getContentType() {
            return attachmentInfo.getType();
        }

        @Override
        public String getName() {
            return attachmentInfo.getName();
        }
    }
}
