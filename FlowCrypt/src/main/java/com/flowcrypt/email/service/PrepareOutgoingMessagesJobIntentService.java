/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.MessageFlag;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.database.MessageState;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.jobscheduler.ForwardedAttachmentsDownloaderJobService;
import com.flowcrypt.email.jobscheduler.JobIdManager;
import com.flowcrypt.email.jobscheduler.MessagesSenderJobService;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.gms.common.util.CollectionUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

/**
 * This service creates a new outgoing message using the given {@link OutgoingMessageInfo}.
 *
 * @author DenBond7
 *         Date: 22.05.2017
 *         Time: 22:25
 *         E-mail: DenBond7@gmail.com
 */

public class PrepareOutgoingMessagesJobIntentService extends JobIntentService {
    private static final String EXTRA_KEY_OUTGOING_MESSAGE_INFO = GeneralUtil.generateUniqueExtraKey
            ("EXTRA_KEY_OUTGOING_MESSAGE_INFO", PrepareOutgoingMessagesJobIntentService.class);
    private static final String TAG = PrepareOutgoingMessagesJobIntentService.class.getSimpleName();

    private MessageDaoSource messageDaoSource;
    private Js js;
    private Session session;
    private AccountDao accountDao;
    private File attachmentsCacheDirectory;

    /**
     * Enqueue a new task for {@link PrepareOutgoingMessagesJobIntentService}.
     *
     * @param context             Interface to global information about an application environment.
     * @param outgoingMessageInfo {@link OutgoingMessageInfo} which contains information about an outgoing message.
     */
    public static void enqueueWork(Context context, OutgoingMessageInfo outgoingMessageInfo) {
        if (outgoingMessageInfo != null) {
            Intent intent = new Intent(context, PrepareOutgoingMessagesJobIntentService.class);
            intent.putExtra(EXTRA_KEY_OUTGOING_MESSAGE_INFO, outgoingMessageInfo);

            enqueueWork(context, PrepareOutgoingMessagesJobIntentService.class, JobIdManager
                    .JOB_TYPE_PREPARE_OUT_GOING_MESSAGE, intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        messageDaoSource = new MessageDaoSource();
        accountDao = new AccountDaoSource().getActiveAccountInformation(getApplicationContext());
        session = OpenStoreHelper.getSessionForAccountDao(getApplicationContext(), accountDao);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public boolean onStopCurrentWork() {
        Log.d(TAG, "onStopCurrentWork");
        return super.onStopCurrentWork();
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d(TAG, "onHandleWork");
        if (intent.hasExtra(EXTRA_KEY_OUTGOING_MESSAGE_INFO)) {
            OutgoingMessageInfo outgoingMessageInfo = intent.getParcelableExtra(EXTRA_KEY_OUTGOING_MESSAGE_INFO);

            Log.d(TAG, "Received a new job: " + outgoingMessageInfo);

            setupIfNeed();

            updateContactsLastUseDateTime(outgoingMessageInfo);

            Uri newMessageUri = null;
            long generatedUID = -1;

            try {
                String[] pubKeys = outgoingMessageInfo.getMessageEncryptionType() == MessageEncryptionType.ENCRYPTED ?
                        SecurityUtils.getRecipientsPubKeys(getApplicationContext(), js, EmailUtil.getAllRecipients
                                (outgoingMessageInfo), accountDao, outgoingMessageInfo.getFromPgpContact().getEmail())
                        : null;

                String rawMessage = EmailUtil.generateRawMessageWithoutAttachments(outgoingMessageInfo, js, pubKeys);
                generatedUID = EmailUtil.generateOutboxUID(getApplicationContext());

                MimeMessage mimeMessage = new MimeMessage(session, IOUtils.toInputStream(rawMessage,
                        StandardCharsets.UTF_8));

                File messageAttachmentCacheDirectory = new File(attachmentsCacheDirectory,
                        UUID.randomUUID().toString());

                ContentValues contentValues = prepareContentValues(outgoingMessageInfo, generatedUID, mimeMessage,
                        rawMessage, messageAttachmentCacheDirectory);

                newMessageUri = messageDaoSource.addRow(getApplicationContext(), contentValues);

                if (newMessageUri != null) {
                    new ImapLabelsDaoSource().updateLabelMessageCount(getApplicationContext(), accountDao.getEmail(),
                            JavaEmailConstants.FOLDER_OUTBOX, messageDaoSource.getOutboxMessages
                                    (getApplicationContext(),
                                            accountDao.getEmail()).size());

                    if (!CollectionUtils.isEmpty(outgoingMessageInfo.getAttachmentInfoArrayList())
                            || !CollectionUtils.isEmpty(outgoingMessageInfo.getForwardedAttachmentInfoList())) {
                        if (!messageAttachmentCacheDirectory.exists()) {
                            if (!messageAttachmentCacheDirectory.mkdir()) {
                                Log.e(TAG, "Create cache directory " + attachmentsCacheDirectory.getName() + " filed!");
                                messageDaoSource.updateMessageState(getApplicationContext(), accountDao.getEmail(),
                                        JavaEmailConstants.FOLDER_OUTBOX, generatedUID, MessageState
                                                .ERROR_CACHE_PROBLEM);
                                return;
                            }
                        }

                        addAttachmentsToCache(outgoingMessageInfo, generatedUID, pubKeys,
                                messageAttachmentCacheDirectory);
                    }

                    if (CollectionUtils.isEmpty(outgoingMessageInfo.getForwardedAttachmentInfoList())) {
                        messageDaoSource.updateMessageState(getApplicationContext(), accountDao.getEmail(),
                                JavaEmailConstants.FOLDER_OUTBOX, generatedUID, MessageState.QUEUED);
                        MessagesSenderJobService.schedule(getApplicationContext());
                    } else {
                        ForwardedAttachmentsDownloaderJobService.schedule(getApplicationContext());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                ExceptionUtil.handleError(e);

                if (newMessageUri != null) {
                    messageDaoSource.updateMessageState(getApplicationContext(), accountDao.getEmail(),
                            JavaEmailConstants.FOLDER_OUTBOX, generatedUID, MessageState.ERROR_DURING_CREATION);
                }
            }

            if (newMessageUri != null) {
                new ImapLabelsDaoSource().updateLabelMessageCount(this, accountDao.getEmail(),
                        JavaEmailConstants.FOLDER_OUTBOX, new MessageDaoSource().getOutboxMessages(this,
                                accountDao.getEmail()).size());
            }
        }
    }

    @NonNull
    private ContentValues prepareContentValues(OutgoingMessageInfo outgoingMessageInfo,
                                               long generatedUID, MimeMessage mimeMessage,
                                               String rawMessage, File attachmentsCacheDirectory)
            throws MessagingException {
        ContentValues contentValues = MessageDaoSource.prepareContentValues(accountDao.getEmail(),
                JavaEmailConstants.FOLDER_OUTBOX, mimeMessage, generatedUID, false);

        contentValues.put(MessageDaoSource.COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS, rawMessage);
        contentValues.put(MessageDaoSource.COL_FLAGS, MessageFlag.SEEN);
        contentValues.put(MessageDaoSource.COL_IS_MESSAGE_HAS_ATTACHMENTS,
                !CollectionUtils.isEmpty(outgoingMessageInfo.getAttachmentInfoArrayList())
                        || !CollectionUtils.isEmpty(outgoingMessageInfo.getForwardedAttachmentInfoList()));
        contentValues.put(MessageDaoSource.COL_IS_ENCRYPTED,
                outgoingMessageInfo.getMessageEncryptionType() == MessageEncryptionType.ENCRYPTED);
        contentValues.put(MessageDaoSource.COL_STATE, outgoingMessageInfo.isForwarded()
                ? MessageState.NEW_FORWARDED.getValue() : MessageState.NEW.getValue());
        contentValues.put(MessageDaoSource.COL_ATTACHMENTS_DIRECTORY, attachmentsCacheDirectory.getName());

        return contentValues;
    }

    private void addAttachmentsToCache(OutgoingMessageInfo outgoingMessageInfo, long generatedUID, String[] pubKeys,
                                       File messageAttachmentCacheDirectory) {
        AttachmentDaoSource attachmentDaoSource = new AttachmentDaoSource();
        List<AttachmentInfo> cachedAttachments = new ArrayList<>();

        if (!CollectionUtils.isEmpty(outgoingMessageInfo.getAttachmentInfoArrayList())) {
            if (outgoingMessageInfo.getMessageEncryptionType() == MessageEncryptionType.ENCRYPTED) {
                for (AttachmentInfo attachmentInfo : outgoingMessageInfo.getAttachmentInfoArrayList()) {
                    try {
                        Uri uriOfOriginalFile = attachmentInfo.getUri();

                        InputStream inputStream = getContentResolver().openInputStream(uriOfOriginalFile);
                        if (inputStream != null) {
                            File encryptedTempFile = new File(messageAttachmentCacheDirectory,
                                    attachmentInfo.getName() + Constants.PGP_FILE_EXT);
                            byte[] encryptedBytes = js.crypto_message_encrypt(pubKeys, IOUtils.toByteArray
                                    (inputStream), attachmentInfo.getName());
                            FileUtils.writeByteArrayToFile(encryptedTempFile, encryptedBytes);
                            attachmentInfo.setUri(FileProvider.getUriForFile(getApplicationContext(),
                                    Constants.FILE_PROVIDER_AUTHORITY, encryptedTempFile));
                            attachmentInfo.setName(encryptedTempFile.getName());
                            cachedAttachments.add(attachmentInfo);

                            if (Constants.FILE_PROVIDER_AUTHORITY.equalsIgnoreCase(uriOfOriginalFile.getAuthority())) {
                                getContentResolver().delete(uriOfOriginalFile, null, null);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                for (AttachmentInfo attachmentInfo : outgoingMessageInfo.getAttachmentInfoArrayList()) {
                    try {
                        Uri uriOfOriginalFile = attachmentInfo.getUri();

                        InputStream inputStream = getContentResolver().openInputStream(uriOfOriginalFile);
                        if (inputStream != null) {
                            File cachedAttachment = new File(messageAttachmentCacheDirectory, attachmentInfo.getName());
                            FileUtils.copyInputStreamToFile(inputStream, cachedAttachment);
                            attachmentInfo.setUri(FileProvider.getUriForFile(getApplicationContext(),
                                    Constants.FILE_PROVIDER_AUTHORITY, cachedAttachment));
                            cachedAttachments.add(attachmentInfo);

                            if (Constants.FILE_PROVIDER_AUTHORITY.equalsIgnoreCase(uriOfOriginalFile.getAuthority())) {
                                getContentResolver().delete(uriOfOriginalFile, null, null);
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        ExceptionUtil.handleError(e);
                    }
                }
            }
        }

        if (!CollectionUtils.isEmpty(outgoingMessageInfo.getForwardedAttachmentInfoList())) {
            if (outgoingMessageInfo.getMessageEncryptionType() == MessageEncryptionType.ENCRYPTED) {
                for (AttachmentInfo attachmentInfo : outgoingMessageInfo.getForwardedAttachmentInfoList()) {
                    AttachmentInfo attachmentInfoEncrypted =
                            new AttachmentInfo(JavaEmailConstants.FOLDER_OUTBOX, attachmentInfo);
                    attachmentInfoEncrypted.setName(attachmentInfoEncrypted.getName() + Constants.PGP_FILE_EXT);
                    cachedAttachments.add(attachmentInfoEncrypted);
                }
            } else {
                for (AttachmentInfo attachmentInfo : outgoingMessageInfo.getForwardedAttachmentInfoList()) {
                    cachedAttachments.add(new AttachmentInfo(JavaEmailConstants.FOLDER_OUTBOX, attachmentInfo));
                }
            }
        }

        attachmentDaoSource.addRows(getApplicationContext(), accountDao.getEmail(), JavaEmailConstants.FOLDER_OUTBOX,
                generatedUID, cachedAttachments);
    }

    private void setupIfNeed() {
        if (js == null) {
            try {
                js = new Js(getApplicationContext(), new SecurityStorageConnector(getApplicationContext()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (attachmentsCacheDirectory == null) {
            attachmentsCacheDirectory = new File(getCacheDir(), Constants.ATTACHMENTS_CACHE_DIR);
            if (!attachmentsCacheDirectory.exists()) {
                if (!attachmentsCacheDirectory.mkdirs()) {
                    throw new IllegalStateException("Create cache directory " + attachmentsCacheDirectory.getName() +
                            " filed!");
                }
            }
        }
    }

    /**
     * Update the {@link ContactsDaoSource#COL_LAST_USE} field in the {@link ContactsDaoSource#TABLE_NAME_CONTACTS}.
     *
     * @param outgoingMessageInfo - {@link OutgoingMessageInfo} which contains information about an outgoing message.
     */
    private void updateContactsLastUseDateTime(OutgoingMessageInfo outgoingMessageInfo) {
        ContactsDaoSource contactsDaoSource = new ContactsDaoSource();

        for (PgpContact pgpContact : EmailUtil.getAllRecipients(outgoingMessageInfo)) {
            int updateResult = contactsDaoSource.updateLastUseOfPgpContact(getApplicationContext(), pgpContact);
            if (updateResult == -1) {
                contactsDaoSource.addRow(getApplicationContext(), pgpContact);
            }
        }
    }
}
