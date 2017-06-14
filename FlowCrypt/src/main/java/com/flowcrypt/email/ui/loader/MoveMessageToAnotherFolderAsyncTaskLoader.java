/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/tree/master/src/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.accounts.Account;
import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.model.results.LoaderResult;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.sun.mail.gimap.GmailSSLStore;
import com.sun.mail.imap.IMAPFolder;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

/**
 * This loader move a message from one folder to another folder.
 *
 * @author DenBond7
 *         Date: 03.06.2017
 *         Time: 15:25
 *         E-mail: DenBond7@gmail.com
 */

public class MoveMessageToAnotherFolderAsyncTaskLoader extends
        AsyncTaskLoader<LoaderResult> {

    private Account account;
    private String sourcesFolder;
    private String destinationFolder;
    private GeneralMessageDetails generalMessageDetails;

    public MoveMessageToAnotherFolderAsyncTaskLoader(Context context,
                                                     Account account,
                                                     GeneralMessageDetails generalMessageDetails,
                                                     String sourcesFolder,
                                                     String destinationFolder) {
        super(context);
        this.account = account;
        this.generalMessageDetails = generalMessageDetails;
        this.sourcesFolder = sourcesFolder;
        this.destinationFolder = destinationFolder;

        onContentChanged();
    }

    @Override
    public void onStartLoading() {
        if (takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    public LoaderResult loadInBackground() {
        GmailSSLStore gmailSSLStore = null;
        IMAPFolder sourceImapFolder = null;
        IMAPFolder destinationImapFolder = null;
        try {
            String token = GoogleAuthUtil.getToken(getContext(), account,
                    JavaEmailConstants.OAUTH2 + GmailConstants.SCOPE_MAIL_GOOGLE_COM);
            gmailSSLStore = OpenStoreHelper.openAndConnectToGimapsStore(token,
                    account.name);

            sourceImapFolder = (IMAPFolder) gmailSSLStore.getFolder(sourcesFolder);
            if (sourceImapFolder == null || !sourceImapFolder.exists()) {
                return new LoaderResult(null, new IllegalArgumentException("The invalid source " +
                        "folder: " + "\"" + sourcesFolder + "\""));
            }
            sourceImapFolder.open(Folder.READ_WRITE);

            Message message = sourceImapFolder.getMessageByUID(generalMessageDetails.getUid());

            if (message != null) {
                destinationImapFolder = (IMAPFolder) gmailSSLStore.getFolder(destinationFolder);
                if (destinationImapFolder == null || !destinationImapFolder.exists()) {
                    return new LoaderResult(null, new IllegalArgumentException("The invalid " +
                            "destination folder: " + "\"" + destinationImapFolder + "\""));
                }
                destinationImapFolder.open(Folder.READ_WRITE);

                message.setFlag(Flags.Flag.SEEN, true);
                Message[] messages = new Message[]{message};

                sourceImapFolder.moveMessages(messages, destinationImapFolder);
            } else {
                return new LoaderResult(null, new IllegalArgumentException("The message with " +
                        "UID " + generalMessageDetails.getUid() + " not found on the server"));
            }

            return new LoaderResult(true, null);
        } catch (Exception e) {
            e.printStackTrace();
            return new LoaderResult(null, e);
        } finally {
            releaseResources(gmailSSLStore, sourceImapFolder, destinationImapFolder);
        }
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }

    /**
     * Release all used resources.
     *
     * @param gmailSSLStore         The {@link GmailSSLStore} object which used as the connection
     *                              point.
     * @param sourceImapFolder      The source {@link IMAPFolder} where the original messages exist.
     * @param destinationImapFolder The destination {@link IMAPFolder} where the original
     *                              messages moved.
     */
    private void releaseResources(GmailSSLStore gmailSSLStore, IMAPFolder sourceImapFolder,
                                  IMAPFolder destinationImapFolder) {
        try {
            if (sourceImapFolder != null && sourceImapFolder.isOpen()) {
                sourceImapFolder.close(false);
            }

            if (destinationImapFolder != null && destinationImapFolder.isOpen()) {
                destinationImapFolder.close(false);
            }

            if (gmailSSLStore != null && gmailSSLStore.isConnected()) {
                gmailSSLStore.close();
            }
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
