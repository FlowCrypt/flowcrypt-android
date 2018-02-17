/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.content.Context;
import android.os.Messenger;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.SearchBackupsUtil;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.model.KeyDetails;
import com.google.android.gms.auth.GoogleAuthException;
import com.sun.mail.imap.IMAPFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

/**
 * This task load the private keys from the email INBOX folder.
 *
 * @author DenBond7
 *         Date: 05.07.2017
 *         Time: 10:27
 *         E-mail: DenBond7@gmail.com
 */

public class LoadPrivateKeysFromEmailBackupSyncTask extends BaseSyncTask {
    /**
     * The base constructor.
     *
     * @param ownerKey    The name of the reply to {@link Messenger}.
     * @param requestCode The unique request code for the reply to {@link Messenger}.
     */
    public LoadPrivateKeysFromEmailBackupSyncTask(String ownerKey, int requestCode) {
        super(ownerKey, requestCode);
    }

    @Override
    public void runIMAPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener)
            throws Exception {
        super.runIMAPAction(accountDao, session, store, syncListener);

        if (syncListener != null) {
            ArrayList<KeyDetails> keyDetailsList = new ArrayList<>();
            List<String> keys = new ArrayList<>();

            switch (accountDao.getAccountType()) {
                case AccountDao.ACCOUNT_TYPE_GOOGLE:
                    keyDetailsList.addAll(EmailUtil.getPrivateKeyBackupsUsingGmailAPI(syncListener.getContext(),
                            accountDao, session));
                    break;

                default:
                    keyDetailsList.addAll(getPrivateKeyBackupsUsingJavaMailAPI(syncListener.getContext(),
                            accountDao, session));
                    break;
            }

            for (KeyDetails keyDetails : keyDetailsList) {
                keys.add(keyDetails.getValue());
            }

            syncListener.onPrivateKeyFound(accountDao, keys, ownerKey, requestCode);
        }
    }

    /**
     * Get a list of {@link KeyDetails} using the standard <b>JavaMail API</b>
     *
     * @param session A {@link Session} object.
     * @return A list of {@link KeyDetails}
     * @throws MessagingException
     * @throws IOException
     * @throws GoogleAuthException
     */
    private Collection<? extends KeyDetails> getPrivateKeyBackupsUsingJavaMailAPI(Context context,
                                                                                  AccountDao accountDao,
                                                                                  Session session)
            throws MessagingException, IOException, GoogleAuthException {
        ArrayList<KeyDetails> privateKeyDetailsList = new ArrayList<>();
        Store store = null;
        try {
            store = OpenStoreHelper.openAndConnectToStore(context, accountDao, session);
            Folder[] folders = store.getDefaultFolder().list("*");

            for (Folder folder : folders) {
                if (!EmailUtil.isFolderHasNoSelectAttribute((IMAPFolder) folder)) {
                    folder.open(Folder.READ_ONLY);

                    Message[] foundMessages = folder.search(
                            SearchBackupsUtil.generateSearchTerms(accountDao.getEmail()));

                    for (Message message : foundMessages) {
                        String key = EmailUtil.getKeyFromMessageIfItExists(message);
                        if (!TextUtils.isEmpty(key)
                                && EmailUtil.privateKeyNotExistsInList(privateKeyDetailsList, key)) {
                            privateKeyDetailsList.add(new KeyDetails(key, KeyDetails.Type.EMAIL));
                        }
                    }

                    folder.close(false);
                }
            }

            store.close();
        } catch (MessagingException | IOException | GoogleAuthException e) {
            e.printStackTrace();
            if (store != null) {
                store.close();
            }
            throw e;
        }
        return privateKeyDetailsList;
    }
}
