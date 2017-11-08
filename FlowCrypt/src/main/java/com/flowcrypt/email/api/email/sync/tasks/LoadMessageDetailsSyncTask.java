/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.os.Messenger;

import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.sun.mail.iap.Argument;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.BODY;
import com.sun.mail.imap.protocol.FetchResponse;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.util.ASCIIUtility;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Store;

/**
 * This task load a detail information of the some message. At now this task creates and executes
 * command like this "UID FETCH xxxxxxxxxxxxx (RFC822.SIZE BODY[]<0.204800>)". We request first
 * 200kb of a message, and if the message is smaller, we'll get the whole MIME message. If
 * larger, we'll get only the first part of it.
 *
 * @author DenBond7
 *         Date: 26.06.2017
 *         Time: 17:41
 *         E-mail: DenBond7@gmail.com
 */

public class LoadMessageDetailsSyncTask extends BaseSyncTask {
    private long uid;
    private String folderName;

    /**
     * The base constructor.
     *
     * @param ownerKey    The name of the reply to {@link Messenger}.
     * @param requestCode The unique request code for the reply to {@link Messenger}.
     * @param folderName  The folder name where a message exists.
     * @param uid         The {@link com.sun.mail.imap.protocol.UID} of {@link Message).
     */
    public LoadMessageDetailsSyncTask(String ownerKey, int requestCode, String folderName, long
            uid) {
        super(ownerKey, requestCode);
        this.folderName = folderName;
        this.uid = uid;
    }

    @Override
    public void runIMAPAction(AccountDao accountDao, Store store, SyncListener syncListener) throws Exception {
        IMAPFolder imapFolder = (IMAPFolder) store.getFolder(folderName);
        imapFolder.open(Folder.READ_WRITE);

        if (syncListener != null) {
            String rawMessage = (String) imapFolder.doCommand(new IMAPFolder.ProtocolCommand() {
                public Object doCommand(IMAPProtocol imapProtocol)
                        throws ProtocolException {
                    String rawMessage = null;

                    Argument args = new Argument();
                    Argument list = new Argument();
                    list.writeString("RFC822.SIZE");
                    list.writeString("BODY[]<0.204800>");
                    args.writeArgument(list);


                    Response[] responses = imapProtocol.command("UID FETCH " + uid, args);
                    Response serverStatusResponse = responses[responses.length - 1];

                    if (serverStatusResponse.isOK()) {
                        for (Response response : responses) {
                            if (!(response instanceof FetchResponse))
                                continue;

                            FetchResponse fetchResponse = (FetchResponse) response;
                            BODY body = fetchResponse.getItem(BODY.class);
                            if (body != null && body.getByteArrayInputStream() != null) {
                                rawMessage = ASCIIUtility.toString(body.getByteArrayInputStream());
                            }
                        }
                    }

                    imapProtocol.notifyResponseHandlers(responses);
                    imapProtocol.handleResult(serverStatusResponse);

                    return rawMessage;
                }
            });

            Message message = imapFolder.getMessageByUID(uid);
            if (message != null) {
                message.setFlag(Flags.Flag.SEEN, true);
            }

            syncListener.onMessageDetailsReceived(accountDao, imapFolder, uid, rawMessage, ownerKey, requestCode);
        }

        imapFolder.close(false);
    }
}
