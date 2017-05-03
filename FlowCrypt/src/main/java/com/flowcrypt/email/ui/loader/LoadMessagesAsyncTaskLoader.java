package com.flowcrypt.email.ui.loader;

import android.accounts.Account;
import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.Message;
import com.google.android.gms.auth.GoogleAuthUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;

/**
 * This loader download information about first ten messages. This is a test variant.
 *
 * @author DenBond7
 *         Date: 28.04.2017
 *         Time: 9:37
 *         E-mail: DenBond7@gmail.com
 */

public class LoadMessagesAsyncTaskLoader extends AsyncTaskLoader<List<Message>> {
    private Account account;

    public LoadMessagesAsyncTaskLoader(Context context, Account account) {
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
    public List<Message> loadInBackground() {
        Properties props = new Properties();
        props.put(JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_SSL_ENABLE, "true");
        props.put(JavaEmailConstants.PROPERTY_NAME_MAIL_IMAP_AUTH_MECHANISMS,
                JavaEmailConstants.MECHANISMS_TYPE_XOAUTH2);
        Session session = Session.getInstance(props);
        session.setDebug(BuildConfig.DEBUG);
        try {

            String token = GoogleAuthUtil.getToken(getContext(), account,
                    JavaEmailConstants.OAUTH2 + GmailConstants.SCOPE_MAIL_GOOGLE_COM);
            Store store = session.getStore(JavaEmailConstants.PROTOCOL_IMAP);
            store.connect(JavaEmailConstants.GMAIL_IMAP_SERVER, account.name, token);
            Folder folder = store.getFolder(GmailConstants.FOLDER_NAME_INBOX);
            folder.open(Folder.READ_ONLY);
            List<javax.mail.Message> messages = new ArrayList<>(Arrays.asList(folder.getMessages
                    (1, folder
                    .getMessageCount() > 10 ? 10 : folder.getMessageCount())));
            List<Message> simpleMessageModels = new LinkedList<>();

            for (javax.mail.Message message : messages) {
                simpleMessageModels.add(new Message(
                        ((InternetAddress) message.getFrom()[0]).getAddress(),
                        message.getSubject(), message.getReceivedDate()));
            }

            folder.close(false);
            store.close();
            return simpleMessageModels;
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
