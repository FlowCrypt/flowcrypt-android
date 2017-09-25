/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.os.Messenger;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.sun.mail.gimap.GmailRawSearchTerm;
import com.sun.mail.imap.IMAPFolder;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;
import javax.mail.search.SearchTerm;
import javax.mail.search.SubjectTerm;

/**
 * This task load the private keys from the email INBOX folder.
 *
 * @author DenBond7
 *         Date: 05.07.2017
 *         Time: 10:27
 *         E-mail: DenBond7@gmail.com
 */

public class LoadPrivateKeysFromEmailBackupSyncTask extends BaseSyncTask {
    private String searchTermString;

    /**
     * The base constructor.
     *
     * @param searchTermString The search phrase.
     * @param ownerKey         The name of the reply to {@link Messenger}.
     * @param requestCode      The unique request code for the reply to {@link Messenger}.
     */
    public LoadPrivateKeysFromEmailBackupSyncTask(String searchTermString,
                                                  String ownerKey, int requestCode) {
        super(ownerKey, requestCode);
        this.searchTermString = searchTermString;
    }

    @Override
    public void runIMAPAction(AccountDao accountDao, Store store, SyncListener syncListener) throws Exception {
        super.runIMAPAction(accountDao, store, syncListener);

        if (syncListener != null) {
            IMAPFolder imapFolder = (IMAPFolder) store.getFolder(JavaEmailConstants.FOLDER_INBOX);
            imapFolder.open(Folder.READ_ONLY);

            List<String> keys = new ArrayList<>();

            SearchTerm searchTerm;

            switch (accountDao.getAccountType()) {
                case AccountDao.ACCOUNT_TYPE_GOOGLE:
                    searchTerm = new GmailRawSearchTerm(searchTermString);
                    break;

                default:
                    searchTerm = new SubjectTerm("Your FlowCrypt Backup");
                    //TODO-denbond7 Need to fix search backups for different providers
                    break;
            }

            Message[] foundMessages = imapFolder.search(searchTerm);

            for (Message message : foundMessages) {
                String key = getKeyFromMessageIfItExists(message);
                if (!TextUtils.isEmpty(key) && !keys.contains(key)) {
                    keys.add(key);
                }
            }

            syncListener.onPrivateKeyFound(accountDao, keys, ownerKey, requestCode);

            imapFolder.close(false);
        }
    }

    /**
     * Get a private key from {@link Message}, if it exists in.
     *
     * @param message The original {@link Message} object.
     * @return <tt>String</tt> A private key.
     * @throws MessagingException
     * @throws IOException
     */
    private String getKeyFromMessageIfItExists(Message message) throws MessagingException,
            IOException {
        if (message.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
            Multipart multiPart = (Multipart) message.getContent();
            int numberOfParts = multiPart.getCount();
            for (int partCount = 0; partCount < numberOfParts; partCount++) {
                BodyPart bodyPart = multiPart.getBodyPart(partCount);
                if (bodyPart instanceof MimeBodyPart) {
                    MimeBodyPart mimeBodyPart = (MimeBodyPart) bodyPart;
                    if (Part.ATTACHMENT.equalsIgnoreCase(mimeBodyPart.getDisposition())) {
                        return IOUtils.toString(mimeBodyPart.getInputStream(),
                                StandardCharsets.UTF_8);
                    }
                }
            }
        }

        return null;
    }
}
