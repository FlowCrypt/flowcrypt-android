package com.flowcrypt.email.ui.loader;

import android.accounts.Account;
import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.sun.mail.gimap.GmailSSLStore;
import com.sun.mail.imap.IMAPFolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;

/**
 * This loader download information about first ten messages. This is a test variant.
 *
 * @author DenBond7
 *         Date: 28.04.2017
 *         Time: 9:37
 *         E-mail: DenBond7@gmail.com
 */

public class LoadGeneralMessagesDetailsAsyncTaskLoader extends
        AsyncTaskLoader<List<GeneralMessageDetails>> {
    private Account account;

    public LoadGeneralMessagesDetailsAsyncTaskLoader(Context context, Account account) {
        super(context);
        this.account = account;
        onContentChanged();
    }

    @Override
    public void onStartLoading() {
        if (takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    public List<GeneralMessageDetails> loadInBackground() {
        try {
            String token = GoogleAuthUtil.getToken(getContext(), account,
                    JavaEmailConstants.OAUTH2 + GmailConstants.SCOPE_MAIL_GOOGLE_COM);
            GmailSSLStore gmailSSLStore = OpenStoreHelper.openAndConnectToGimapsStore(token,
                    account.name);

            IMAPFolder imapFolder = (IMAPFolder) gmailSSLStore.getFolder(GmailConstants
                    .FOLDER_NAME_INBOX);
            imapFolder.open(Folder.READ_ONLY);

            List<javax.mail.Message> messages = new ArrayList<>(Arrays.asList(imapFolder.getMessages
                    (1, imapFolder
                            .getMessageCount() > 10 ? 10 : imapFolder.getMessageCount())));
            List<GeneralMessageDetails> generalMessageDetailsLinkedList = new LinkedList<>();

            for (Message message : messages) {
                generalMessageDetailsLinkedList.add(new GeneralMessageDetails(
                        ((InternetAddress) message.getFrom()[0]).getAddress(),
                        message.getSubject(),
                        message.getReceivedDate(),
                        imapFolder.getUID(message)));
            }

            imapFolder.close(false);
            gmailSSLStore.close();
            return generalMessageDetailsLinkedList;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }
}
