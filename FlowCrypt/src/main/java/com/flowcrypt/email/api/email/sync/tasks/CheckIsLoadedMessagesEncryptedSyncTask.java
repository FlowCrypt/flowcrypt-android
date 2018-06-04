/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.os.Messenger;
import android.support.annotation.NonNull;
import android.util.LongSparseArray;
import android.util.SparseArray;

import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.sun.mail.iap.Argument;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.BODY;
import com.sun.mail.imap.protocol.FetchResponse;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.protocol.MessageSet;
import com.sun.mail.imap.protocol.UID;
import com.sun.mail.util.ASCIIUtility;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

/**
 * This task identifies encrypted messages and updates information about messages in the local database.
 *
 * @author Denis Bondarenko
 * Date: 02.06.2018
 * Time: 14:30
 * E-mail: DenBond7@gmail.com
 */
public class CheckIsLoadedMessagesEncryptedSyncTask extends BaseSyncTask {
    private MessageSet[] messageSets;
    private com.flowcrypt.email.api.email.Folder localFolder;

    /**
     * The base constructor.
     *
     * @param ownerKey    The name of the reply to {@link Messenger}.
     * @param requestCode The unique request code for the reply to {@link Messenger}.
     * @param messageSets The array of {@link MessageSet} which contains information which messages will be checked
     *                    for decryption.
     * @param localFolder The local implementation of the remote folder
     */
    public CheckIsLoadedMessagesEncryptedSyncTask(String ownerKey, int requestCode, MessageSet[] messageSets,
                                                  com.flowcrypt.email.api.email.Folder localFolder) {
        super(ownerKey, requestCode);
        this.messageSets = messageSets;
        this.localFolder = localFolder;
    }

    @Override
    public void runIMAPAction(AccountDao accountDao, Session session, Store store, SyncListener syncListener)
            throws Exception {
        super.runIMAPAction(accountDao, session, store, syncListener);

        if (messageSets == null || messageSets.length == 0) {
            return;
        }

        IMAPFolder imapFolder = (IMAPFolder) store.getFolder(localFolder.getServerFullFolderName());
        imapFolder.open(Folder.READ_ONLY);

        LongSparseArray<Boolean> booleanLongSparseArray = getInfoAreMessagesEncrypted(imapFolder, messageSets);

        if (booleanLongSparseArray.size() > 0) {
            MessageDaoSource messageDaoSource = new MessageDaoSource();
            messageDaoSource.updateMessagesEncryptionStateByUID(syncListener.getContext(), accountDao.getEmail(),
                    localFolder.getFolderAlias(), booleanLongSparseArray);
        }

        imapFolder.close(false);
    }

    /**
     * Check is input messages are encrypted.
     *
     * @param imapFolder The localFolder which contains messages which will be checked.
     * @param messageSet The array of {@link MessageSet} which contains information which messages will be checked
     *                   for decryption.
     * @return {@link SparseArray} as results of the checking.
     */
    @SuppressWarnings("unchecked")
    @NonNull
    private LongSparseArray<Boolean> getInfoAreMessagesEncrypted(IMAPFolder imapFolder, final MessageSet[] messageSet)
            throws MessagingException {

        return (LongSparseArray<Boolean>) imapFolder.doCommand(new IMAPFolder.ProtocolCommand() {
            public Object doCommand(IMAPProtocol imapProtocol) throws ProtocolException {
                LongSparseArray<Boolean> booleanLongSparseArray = new LongSparseArray<>();

                Argument args = new Argument();
                Argument list = new Argument();
                list.writeString("UID");
                list.writeString("BODY.PEEK[TEXT]<0.2048>");
                args.writeArgument(list);

                Response[] responses = imapProtocol.command(
                        ("FETCH ") + MessageSet.toString(messageSet), args);
                Response serverStatusResponse = responses[responses.length - 1];

                if (serverStatusResponse.isOK()) {
                    for (Response response : responses) {
                        if (!(response instanceof FetchResponse))
                            continue;

                        FetchResponse fetchResponse = (FetchResponse) response;

                        UID uid = fetchResponse.getItem(UID.class);
                        if (uid != null && uid.uid != 0) {
                            BODY body = fetchResponse.getItem(BODY.class);
                            if (body != null && body.getByteArrayInputStream() != null) {
                                String rawMessage = ASCIIUtility.toString(body.getByteArrayInputStream());
                                booleanLongSparseArray.put(uid.uid,
                                        rawMessage.contains("-----BEGIN PGP MESSAGE-----"));
                            }
                        }
                    }
                }

                imapProtocol.notifyResponseHandlers(responses);
                imapProtocol.handleResult(serverStatusResponse);

                return booleanLongSparseArray;
            }
        });
    }
}
