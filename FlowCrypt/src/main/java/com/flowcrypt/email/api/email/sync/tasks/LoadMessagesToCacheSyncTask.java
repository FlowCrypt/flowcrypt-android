/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.util.Log;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.protocol.CustomFetchProfileItem;
import com.flowcrypt.email.api.email.protocol.FlowCryptIMAPFolder;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.sun.mail.imap.IMAPFolder;

import javax.mail.FetchProfile;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;

/**
 * This task loads the older messages via some step.
 *
 * @author DenBond7
 *         Date: 23.06.2017
 *         Time: 11:26
 *         E-mail: DenBond7@gmail.com
 */

public class LoadMessagesToCacheSyncTask extends BaseSyncTask {
    private static final int COUNT_OF_LOADED_EMAILS_BY_STEP = 20;
    private static final String TAG = LoadMessagesToCacheSyncTask.class.getSimpleName();
    private String folderName;
    private int countOfAlreadyLoadedMessages;

    public LoadMessagesToCacheSyncTask(String ownerKey, int requestCode, String folderName,
                                       int countOfAlreadyLoadedMessages) {
        super(ownerKey, requestCode);
        this.folderName = folderName;
        this.countOfAlreadyLoadedMessages = countOfAlreadyLoadedMessages;
    }

    @Override
    public void runIMAPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener) throws
            Exception {
        IMAPFolder imapFolder = (IMAPFolder) store.getFolder(folderName);
        if (syncListener != null) {
            syncListener.onActionProgress(accountDao, ownerKey, requestCode, R.id.progress_id_opening_store);
        }
        imapFolder.open(Folder.READ_ONLY);

        if (countOfAlreadyLoadedMessages < 0) {
            countOfAlreadyLoadedMessages = 0;
        }

        int messagesCount = imapFolder.getMessageCount();
        int end = messagesCount - countOfAlreadyLoadedMessages;
        int start = end - COUNT_OF_LOADED_EMAILS_BY_STEP + 1;

        Log.d(TAG, "Run LoadMessagesToCacheSyncTask with parameters:"
                + " folderName = " + folderName
                + " | countOfAlreadyLoadedMessages = " + countOfAlreadyLoadedMessages
                + " | messagesCount = " + messagesCount
                + " | start = " + start
                + " | end = " + end);

        if (syncListener != null) {
            new ImapLabelsDaoSource().updateLabelMessageCount(syncListener.getContext(),
                    imapFolder.getFullName(), messagesCount);

            syncListener.onActionProgress(accountDao, ownerKey, requestCode, R.id.progress_id_getting_list_of_emails);
            if (end < 1) {
                syncListener.onMessagesReceived(accountDao, imapFolder, new Message[]{}, ownerKey, requestCode);
            } else {
                if (start < 1) {
                    start = 1;
                }

                Message[] messages = imapFolder.getMessages(start, end);

                FetchProfile fetchProfile = new FetchProfile();
                fetchProfile.add(FetchProfile.Item.ENVELOPE);
                fetchProfile.add(FetchProfile.Item.FLAGS);
                fetchProfile.add(FetchProfile.Item.CONTENT_INFO);
                fetchProfile.add(UIDFolder.FetchProfileItem.UID);
                fetchProfile.add(CustomFetchProfileItem.BODY_FISRT_CHARACTERS);

                FlowCryptIMAPFolder flowCryptIMAPFolder = (FlowCryptIMAPFolder) imapFolder;
                flowCryptIMAPFolder.fetchGeneralInfo(messages, fetchProfile);

                syncListener.onMessagesReceived(accountDao, imapFolder, messages, ownerKey, requestCode);
            }
        }

        imapFolder.close(false);
    }
}
