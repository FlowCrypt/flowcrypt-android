package com.flowcrypt.email.ui.loader;

import android.accounts.Account;
import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.model.results.ActionResult;
import com.flowcrypt.email.model.results.LoadEmailsResult;
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
        AsyncTaskLoader<ActionResult<LoadEmailsResult>> {
    private static final int COUNT_OF_LOADED_EMAILS_BY_STEP = 10;

    private Account account;
    private String folder;
    private int beginLoadPosition;

    public LoadGeneralMessagesDetailsAsyncTaskLoader(Context context, Account account,
                                                     String folder) {
        this(context, account, folder, 1);
    }

    public LoadGeneralMessagesDetailsAsyncTaskLoader(Context context, Account account, String
            folder, int beginLoadPosition) {
        super(context);
        this.account = account;
        this.folder = folder;
        this.beginLoadPosition = beginLoadPosition;
        onContentChanged();
    }

    @Override
    public void onStartLoading() {
        if (takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    public ActionResult<LoadEmailsResult> loadInBackground() {
        try {
            String token = GoogleAuthUtil.getToken(getContext(), account,
                    JavaEmailConstants.OAUTH2 + GmailConstants.SCOPE_MAIL_GOOGLE_COM);
            GmailSSLStore gmailSSLStore = OpenStoreHelper.openAndConnectToGimapsStore(token,
                    account.name);

            IMAPFolder imapFolder = (IMAPFolder) gmailSSLStore.getFolder(folder);
            imapFolder.open(Folder.READ_ONLY);

            int countOfMessages = imapFolder.getMessageCount();

            List<javax.mail.Message> messages;

            int endLoadPosition = beginLoadPosition + COUNT_OF_LOADED_EMAILS_BY_STEP;

            if (endLoadPosition > countOfMessages) {
                endLoadPosition = countOfMessages;
            }

            messages = new ArrayList<>(Arrays.asList(imapFolder.getMessages
                    (beginLoadPosition, endLoadPosition)));


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

            return new ActionResult<>(new LoadEmailsResult(endLoadPosition,
                    generalMessageDetailsLinkedList), null);
        } catch (Exception e) {
            e.printStackTrace();
            return new ActionResult<>(null, e);
        }
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }
}
