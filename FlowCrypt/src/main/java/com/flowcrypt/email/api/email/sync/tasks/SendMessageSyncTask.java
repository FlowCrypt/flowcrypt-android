/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.content.Context;
import android.net.Uri;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.FoldersManager;
import com.flowcrypt.email.api.email.gmail.GmailApiHelper;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.api.email.protocol.ImapProtocolUtil;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.PgpKeyInfo;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.util.FileAndDirectoryUtils;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.sun.mail.imap.IMAPFolder;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * This task does job of sending a message.
 *
 * @author DenBond7
 * Date: 29.06.2017
 * Time: 11:38
 * E-mail: DenBond7@gmail.com
 */

public class SendMessageSyncTask extends BaseSyncTask {
    private static final String TAG = SendMessageSyncTask.class.getSimpleName();

    private OutgoingMessageInfo outgoingMessageInfo;

    /**
     * The base constructor.
     *
     * @param ownerKey            The name of the reply to {@link Messenger}.
     * @param requestCode         The unique request code for the reply to {@link Messenger}.
     * @param outgoingMessageInfo The {@link OutgoingMessageInfo} which contains information about an outgoing
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
    public void runSMTPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener)
            throws Exception {
        super.runSMTPAction(accountDao, session, store, syncListener);
        boolean isMessageSent;
        if (syncListener != null) {
            Context context = syncListener.getContext();

            File pgpCacheDirectory = new File(context.getCacheDir(), Constants.PGP_ATTACHMENTS_CACHE_DIR);
            if (pgpCacheDirectory.exists()) {
                FileAndDirectoryUtils.cleanDirectory(pgpCacheDirectory);
            } else if (!pgpCacheDirectory.mkdirs()) {
                Log.d(TAG, "Create cache directory " + pgpCacheDirectory.getName() + " filed!");
            }

            updateContactsLastUseDateTime(context);

            IMAPFolder folderOfForwardedMessage = null;
            Message forwardedMessage = null;

            if (outgoingMessageInfo.isForwarded() && !outgoingMessageInfo.getForwardedAttachmentInfoList().isEmpty()) {
                folderOfForwardedMessage = (IMAPFolder) store.getFolder(new ImapLabelsDaoSource()
                        .getFolderByAlias(context,
                                outgoingMessageInfo.getForwardedAttachmentInfoList().get(0).getEmail(),
                                outgoingMessageInfo.getForwardedAttachmentInfoList().get(0).getFolder())
                        .getServerFullFolderName());
                folderOfForwardedMessage.open(Folder.READ_ONLY);
                forwardedMessage = folderOfForwardedMessage.getMessageByUID(
                        outgoingMessageInfo.getForwardedAttachmentInfoList().get(0).getUid());
            }

            MimeMessage mimeMessage = createMimeMessage(session, context, accountDao, pgpCacheDirectory,
                    folderOfForwardedMessage, forwardedMessage);

            switch (accountDao.getAccountType()) {
                case AccountDao.ACCOUNT_TYPE_GOOGLE:
                    if (accountDao.getEmail().equalsIgnoreCase(outgoingMessageInfo.getFromPgpContact().getEmail())) {
                        Transport transport = prepareTransportForSmtp(syncListener.getContext(), session, accountDao);
                        transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
                        isMessageSent = true;
                    } else {
                        Gmail gmailApiService = GmailApiHelper.generateGmailApiService(context, accountDao);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        mimeMessage.writeTo(byteArrayOutputStream);

                        String threadId = null;

                        if (!TextUtils.isEmpty(outgoingMessageInfo.getRawReplyMessage())) {
                            MimeMessage originalMimeMessage = new MimeMessage(session,
                                    new ByteArrayInputStream(outgoingMessageInfo.getRawReplyMessage().getBytes()));

                            threadId = getGmailMessageThreadID(gmailApiService, originalMimeMessage.getMessageID());
                        }

                        com.google.api.services.gmail.model.Message sentMessage
                                = new com.google.api.services.gmail.model.Message();
                        sentMessage.setRaw(Base64.encodeBase64URLSafeString(byteArrayOutputStream.toByteArray()));

                        if (!TextUtils.isEmpty(threadId)) {
                            sentMessage.setThreadId(threadId);
                        }

                        sentMessage = gmailApiService
                                .users()
                                .messages()
                                .send(GmailApiHelper.DEFAULT_USER_ID, sentMessage)
                                .execute();
                        isMessageSent = sentMessage.getId() != null;
                    }

                    //Gmail automatically save a copy of the sent message.
                    break;

                default:
                    Transport transport = prepareTransportForSmtp(syncListener.getContext(), session, accountDao);
                    transport.sendMessage(mimeMessage, mimeMessage.getAllRecipients());
                    isMessageSent = true;

                    saveCopyOfSentMessage(accountDao, store, syncListener.getContext(), mimeMessage);
            }

            FileAndDirectoryUtils.cleanDirectory(pgpCacheDirectory);
            if (folderOfForwardedMessage != null && folderOfForwardedMessage.isOpen()) {
                folderOfForwardedMessage.close(false);
            }
            syncListener.onMessageSent(accountDao, ownerKey, requestCode, isMessageSent);
        }
    }

    /**
     * Save a copy of the sent message to the account SENT folder.
     *
     * @param accountDao  The object which contains information about an email account.
     * @param store       The connected and opened {@link Store} object.
     * @param context     Interface to global information about an application environment.
     * @param mimeMessage The original {@link MimeMessage} which will be saved to the SENT folder.
     * @throws MessagingException Errors can be happened when we try to save a copy of sent message.
     */
    private void saveCopyOfSentMessage(AccountDao accountDao, Store store, Context context, MimeMessage
            mimeMessage) throws MessagingException {
        FoldersManager foldersManager = FoldersManager.fromDatabase(context, accountDao.getEmail());
        com.flowcrypt.email.api.email.Folder sentFolder = foldersManager.getFolderSent();

        if (sentFolder != null) {
            IMAPFolder sentImapFolder = (IMAPFolder) store.getFolder(sentFolder.getServerFullFolderName());

            if (sentImapFolder == null || !sentImapFolder.exists()) {
                throw new IllegalArgumentException("The sent folder doesn't exists. Can't create a copy of " +
                        "the sent message!");
            }

            sentImapFolder.open(Folder.READ_WRITE);
            mimeMessage.setFlag(Flags.Flag.SEEN, true);
            sentImapFolder.appendMessages(new Message[]{mimeMessage});
            sentImapFolder.close(false);
        } else {
            throw new IllegalArgumentException("The SENT folder is not defined");
        }
    }

    /**
     * Create {@link MimeMessage} using different parameters.
     *
     * @param session                  Will be used to create {@link MimeMessage}
     * @param context                  Interface to global information about an application environment.
     * @param accountDao               The {@link AccountDao} which contains information about account.
     * @param pgpCacheDirectory        The cache directory which contains temp files.  @return {@link MimeMessage}
     * @param folderOfForwardedMessage The folder where located our forwarded message
     * @param forwardedMessage         The original forwarded message.
     * @throws IOException
     * @throws MessagingException
     */
    @NonNull
    private MimeMessage createMimeMessage(Session session, Context context, AccountDao accountDao,
                                          File pgpCacheDirectory, IMAPFolder folderOfForwardedMessage,
                                          Message forwardedMessage) throws IOException, MessagingException {
        Js js = new Js(context, new SecurityStorageConnector(context));
        String[] pubKeys = outgoingMessageInfo.getMessageEncryptionType() == MessageEncryptionType.ENCRYPTED ?
                getPubKeys(context, js, accountDao) : null;

        String rawMessage = generateRawMessageWithoutAttachments(js, pubKeys);

        MimeMessage mimeMessage = new MimeMessage(session,
                IOUtils.toInputStream(rawMessage, StandardCharsets.UTF_8));

        if (mimeMessage.getContent() instanceof MimeMultipart) {
            MimeMultipart mimeMultipart = (MimeMultipart) mimeMessage.getContent();

            for (AttachmentInfo attachmentInfo : outgoingMessageInfo.getAttachmentInfoArrayList()) {
                BodyPart attachmentBodyPart = generateBodyPartWithAttachment(context, pgpCacheDirectory,
                        js, pubKeys, attachmentInfo);
                mimeMultipart.addBodyPart(attachmentBodyPart);
            }

            for (AttachmentInfo attachmentInfo : outgoingMessageInfo.getForwardedAttachmentInfoList()) {
                BodyPart forwardedAttachmentBodyPart = generateBodyPartWithForwardedAttachment(pgpCacheDirectory, js,
                        pubKeys, attachmentInfo, folderOfForwardedMessage, forwardedMessage);
                if (forwardedAttachmentBodyPart != null) {
                    mimeMultipart.addBodyPart(forwardedAttachmentBodyPart);
                }
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
     * @return Generated {@link MimeBodyPart} with an attachment.
     * @throws IOException
     * @throws MessagingException
     */
    @NonNull
    private BodyPart generateBodyPartWithAttachment(Context context, File pgpCacheDirectory, Js js,
                                                    String[] pubKeys, AttachmentInfo attachmentInfo)
            throws IOException, MessagingException {
        MimeBodyPart attachmentsBodyPart = new MimeBodyPart();
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
        attachmentsBodyPart.setContentID(EmailUtil.generateContentId());

        return attachmentsBodyPart;
    }

    /**
     * Generate a {@link BodyPart} with forwarded attachment.
     *
     * @param pgpCacheDirectory        The cache directory which contains temp files.
     * @param js                       The {@link Js} tools.
     * @param pubKeys                  The public keys which will be used for generate an encrypted attachments.
     * @param attachmentInfo           The {@link AttachmentInfo} object, which contains general information about
     *                                 attachment.
     * @param folderOfForwardedMessage The folder where located our forwarded message
     * @param forwardedMessage         The original forwarded message.
     * @return Generated {@link MimeBodyPart} with an attachment.
     * @throws IOException
     * @throws MessagingException
     */
    @NonNull
    private BodyPart generateBodyPartWithForwardedAttachment(File pgpCacheDirectory, Js js,
                                                             String[] pubKeys, AttachmentInfo attachmentInfo,
                                                             IMAPFolder folderOfForwardedMessage,
                                                             Message forwardedMessage)
            throws IOException, MessagingException {
        switch (outgoingMessageInfo.getMessageEncryptionType()) {
            case ENCRYPTED:
                BodyPart originalAttachment = ImapProtocolUtil.getAttachmentPartById(folderOfForwardedMessage,
                        forwardedMessage.getMessageNumber(), forwardedMessage, attachmentInfo.getId());

                MimeBodyPart mimeBodyPart = new MimeBodyPart();
                InputStream inputStream = originalAttachment.getInputStream();
                if (inputStream != null) {
                    File encryptedTempFile = generateTempFile(pgpCacheDirectory, attachmentInfo.getName());
                    byte[] encryptedBytes = js.crypto_message_encrypt(pubKeys, IOUtils.toByteArray
                            (inputStream), attachmentInfo.getName());
                    FileUtils.writeByteArrayToFile(encryptedTempFile, encryptedBytes);
                    mimeBodyPart.setDataHandler(new DataHandler(new FileDataSource(encryptedTempFile)));
                    mimeBodyPart.setFileName(encryptedTempFile.getName());
                    mimeBodyPart.setContentID(EmailUtil.generateContentId());
                }
                return mimeBodyPart;

            case STANDARD:
                return ImapProtocolUtil.getAttachmentPartById(folderOfForwardedMessage,
                        forwardedMessage.getMessageNumber(), forwardedMessage, attachmentInfo.getId());
        }

        return null;
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
                outgoingMessageInfo.getCcPgpContacts(),
                outgoingMessageInfo.getBccPgpContacts(),
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
     */
    private File generateTempFile(File parentDirectory, String fileName) {
        return new File(parentDirectory, fileName + ".pgp");
    }

    /**
     * Update the {@link ContactsDaoSource#COL_LAST_USE} field in the {@link ContactsDaoSource#TABLE_NAME_CONTACTS}.
     *
     * @param context - Interface to global information about an application environment.
     */
    private void updateContactsLastUseDateTime(Context context) {
        ContactsDaoSource contactsDaoSource = new ContactsDaoSource();

        for (PgpContact pgpContact : getAllRecipients()) {
            int updateResult = contactsDaoSource.updateLastUseOfPgpContact(context, pgpContact);
            if (updateResult == -1) {
                contactsDaoSource.addRow(context, pgpContact);
            }
        }
    }

    /**
     * Generate a list of the all recipients.
     *
     * @return A list of the all recipients
     */
    private PgpContact[] getAllRecipients() {
        List<PgpContact> pgpContacts = new ArrayList<>();

        if (outgoingMessageInfo.getToPgpContacts() != null) {
            pgpContacts.addAll(Arrays.asList(outgoingMessageInfo.getToPgpContacts()));
        }

        if (outgoingMessageInfo.getCcPgpContacts() != null) {
            pgpContacts.addAll(Arrays.asList(outgoingMessageInfo.getCcPgpContacts()));
        }

        if (outgoingMessageInfo.getBccPgpContacts() != null) {
            pgpContacts.addAll(Arrays.asList(outgoingMessageInfo.getBccPgpContacts()));
        }

        return pgpContacts.toArray(new PgpContact[0]);
    }

    /**
     * Get public keys for recipients + keys of the sender;
     *
     * @param context    Interface to global information about an application environment.
     * @param accountDao The {@link AccountDao} which contains information about account.
     * @param js         - {@link Js} util class.
     * @return <tt>String[]</tt> An array of public keys.
     */
    private String[] getPubKeys(Context context, Js js, AccountDao accountDao) {
        ArrayList<String> publicKeys = new ArrayList<>();
        for (PgpContact pgpContact : getAllRecipients()) {
            if (!TextUtils.isEmpty(pgpContact.getPubkey())) {
                publicKeys.add(pgpContact.getPubkey());
            }
        }

        publicKeys.add(getAccountPublicKey(context, js, accountDao));

        return publicKeys.toArray(new String[0]);
    }

    /**
     * Get a public key of the sender;
     *
     * @param context    Interface to global information about an application environment.
     * @param js         {@link Js} util class.
     * @param accountDao The {@link AccountDao} which contains information about account.
     * @return <tt>String</tt> The sender public key.
     */
    private String getAccountPublicKey(Context context, Js js, AccountDao accountDao) {
        PgpContact pgpContact = new ContactsDaoSource().getPgpContact(context, accountDao.getEmail());

        if (pgpContact != null && !TextUtils.isEmpty(pgpContact.getPubkey())) {
            return pgpContact.getPubkey();
        }

        PgpKeyInfo[] pgpKeyInfoArray = new SecurityStorageConnector(context).getAllPgpPrivateKeys();
        for (PgpKeyInfo pgpKeyInfo : pgpKeyInfoArray) {
            PgpKey pgpKey = js.crypto_key_read(pgpKeyInfo.getPrivate());
            if (pgpKey != null) {
                PgpKey publicKey = pgpKey.toPublic();
                if (publicKey != null) {
                    PgpContact primaryUserId = pgpKey.getPrimaryUserId();
                    if (primaryUserId != null) {
                        if (!TextUtils.isEmpty(publicKey.armor())) {
                            primaryUserId.setPubkey(publicKey.armor());
                            new ContactsDaoSource().addRow(context, primaryUserId);
                            return primaryUserId.getPubkey();
                        }
                        break;
                    }
                }
            }
        }

        throw new IllegalArgumentException("The sender doesn't have a public key");
    }

    /**
     * Retrive a Gmail message thread id.
     *
     * @param service          A {@link Gmail} reference.
     * @param rfc822msgidValue An rfc822 Message-Id value of the input message.
     * @return The input message thread id.
     * @throws IOException
     */
    private String getGmailMessageThreadID(Gmail service, String rfc822msgidValue) throws IOException {
        ListMessagesResponse response = service.users().messages().list(GmailApiHelper.DEFAULT_USER_ID).setQ(
                "rfc822msgid:" + rfc822msgidValue).execute();

        if (response.getMessages() != null && response.getMessages().size() == 1) {
            List<com.google.api.services.gmail.model.Message> messages = response.getMessages();
            return messages.get(0).getThreadId();
        }

        return null;
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
            InputStream inputStream = attachmentInfo.getUri() == null ? (attachmentInfo.getRawData() != null ?
                    IOUtils.toInputStream(attachmentInfo.getRawData(), StandardCharsets.UTF_8) : null) :
                    context.getContentResolver().openInputStream(attachmentInfo.getUri());

            return inputStream == null ? null : new BufferedInputStream(inputStream);
        }

        @Override
        public OutputStream getOutputStream() {
            return null;
        }

        /**
         * If a content type is unknown we return "application/octet-stream".
         * http://www.rfc-editor.org/rfc/rfc2046.txt (section 4.5.1.  Octet-Stream Subtype)
         */
        @Override
        public String getContentType() {
            return TextUtils.isEmpty(attachmentInfo.getType()) ? "application/octet-stream" : attachmentInfo.getType();
        }

        @Override
        public String getName() {
            return attachmentInfo.getName();
        }
    }
}
