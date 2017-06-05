package com.flowcrypt.email.ui.loader;

import android.accounts.Account;
import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.model.EmailAndNamePair;
import com.flowcrypt.email.model.results.LoadEmailsResult;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.service.EmailAndNameUpdaterService;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.sun.mail.gimap.GmailSSLStore;
import com.sun.mail.imap.IMAPFolder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

/**
 * This loader download information about available messages of a folder. If the folder has an
 * attribute {@link LoadGeneralMessagesDetailsAsyncTaskLoader#ATTRIBUTE_VALUE_SENT} then we
 * update information about email/name pairs or will create it.
 *
 * @author DenBond7
 *         Date: 28.04.2017
 *         Time: 9:37
 *         E-mail: DenBond7@gmail.com
 */

public class LoadGeneralMessagesDetailsAsyncTaskLoader extends
        AsyncTaskLoader<LoaderResult> {
    /**
     * This attribute exists in the sent folder;
     */
    private static final String ATTRIBUTE_VALUE_SENT = "\\Sent";
    private static final int COUNT_OF_LOADED_EMAILS_BY_STEP = 10;

    private Account account;
    private String folder;
    private int endLoadPosition;

    public LoadGeneralMessagesDetailsAsyncTaskLoader(Context context, Account account,
                                                     String folder) {
        this(context, account, folder, Integer.MAX_VALUE);
    }

    public LoadGeneralMessagesDetailsAsyncTaskLoader(Context context, Account account, String
            folder, int endLoadPosition) {
        super(context);
        this.account = account;
        this.folder = folder;
        this.endLoadPosition = endLoadPosition;
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
        try {
            String token = GoogleAuthUtil.getToken(getContext(), account,
                    JavaEmailConstants.OAUTH2 + GmailConstants.SCOPE_MAIL_GOOGLE_COM);
            GmailSSLStore gmailSSLStore = OpenStoreHelper.openAndConnectToGimapsStore(token,
                    account.name);

            IMAPFolder imapFolder = (IMAPFolder) gmailSSLStore.getFolder(folder);
            imapFolder.open(Folder.READ_ONLY);

            boolean isSentFolder = Arrays.asList(imapFolder.getAttributes())
                    .contains(ATTRIBUTE_VALUE_SENT);

            int countOfMessages = imapFolder.getMessageCount();

            List<javax.mail.Message> messages;

            if (this.endLoadPosition == Integer.MAX_VALUE) {
                endLoadPosition = countOfMessages;
            }

            int beginLoadPosition = this.endLoadPosition - COUNT_OF_LOADED_EMAILS_BY_STEP;

            if (beginLoadPosition < 1) {
                beginLoadPosition = 1;
            }

            messages = new ArrayList<>(Arrays.asList(imapFolder.getMessages
                    (beginLoadPosition, endLoadPosition)));

            List<GeneralMessageDetails> generalMessageDetailsLinkedList = new LinkedList<>();
            ArrayList<EmailAndNamePair> emailAndNamePairs = new ArrayList<>();

            for (int i = messages.size() - 1; i > 0; i--) {
                Message message = messages.get(i);
                generalMessageDetailsLinkedList.add(new GeneralMessageDetails(
                        ((InternetAddress) message.getFrom()[0]).getAddress(),
                        message.getSubject(),
                        message.getReceivedDate(),
                        imapFolder.getUID(message)));

                if (isSentFolder) {
                    emailAndNamePairs.addAll(getEmailAndNamePairsFromMessage(message));
                }
            }

            imapFolder.close(false);
            gmailSSLStore.close();

            if (isSentFolder) {
                getContext().startService(EmailAndNameUpdaterService.getStartIntent(getContext(),
                        emailAndNamePairs));
            }

            return new LoaderResult(new LoadEmailsResult(beginLoadPosition,
                    generalMessageDetailsLinkedList), null);
        } catch (Exception e) {
            e.printStackTrace();
            return new LoaderResult(null, e);
        }
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }

    /**
     * Generate a list of {@link EmailAndNamePair} objects from the input message.
     * This information will be retrieved from "to" and "cc" headers.
     *
     * @param message The input {@link Message}.
     * @return <tt>{@link List}</tt> of EmailAndNamePair objects, which contains an information
     * about
     * emails and names.
     * @throws MessagingException when retrieve an information about recipients.
     */
    private List<EmailAndNamePair> getEmailAndNamePairsFromMessage(Message message) throws
            MessagingException {
        List<EmailAndNamePair> emailAndNamePairs = new ArrayList<>();

        Address[] addressesTo = message.getRecipients(Message.RecipientType.TO);
        if (addressesTo != null) {
            for (Address address : addressesTo) {
                InternetAddress internetAddress = (InternetAddress) address;
                emailAndNamePairs.add(new EmailAndNamePair(
                        internetAddress.getAddress(),
                        internetAddress.getPersonal()));
            }
        }

        Address[] addressesCC = message.getRecipients(Message.RecipientType.CC);
        if (addressesCC != null) {
            for (Address address : addressesCC) {
                InternetAddress internetAddress = (InternetAddress) address;
                emailAndNamePairs.add(new EmailAndNamePair(
                        internetAddress.getAddress(),
                        internetAddress.getPersonal()));
            }
        }

        return emailAndNamePairs;
    }
}
