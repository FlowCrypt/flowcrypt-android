package com.flowcrypt.email.ui.loader;

import android.accounts.Account;
import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.test.Js;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.sun.mail.gimap.GmailFolder;
import com.sun.mail.gimap.GmailRawSearchTerm;
import com.sun.mail.gimap.GmailSSLStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;

/**
 * This loader find and save user a backup of private keys to the application directory.
 *
 * @author DenBond7
 *         Date: 30.04.2017.
 *         Time: 22:28.
 *         E-mail: DenBond7@gmail.com
 */
public class LoadPrivateKeyAsyncTaskLoader extends AsyncTaskLoader<List<String>> {

    /**
     * An user account.
     */
    private Account account;

    public LoadPrivateKeyAsyncTaskLoader(Context context, Account account) {
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
    public List<String> loadInBackground() {
        List<String> filesPaths = new ArrayList<>();
        Properties properties = new Properties();
        properties.put(GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_SSL_ENABLE, "true");
        properties.put(GmailConstants.PROPERTY_NAME_MAIL_GIMAPS_AUTH_MECHANISMS, JavaEmailConstants
                .MECHANISMS_TYPE_XOAUTH2);
        Session session = Session.getInstance(properties);
        session.setDebug(BuildConfig.DEBUG);
        try {
            String token = GoogleAuthUtil.getToken(getContext(), account,
                    JavaEmailConstants.OAUTH2 + GmailConstants.SCOPE_MAIL_GOOGLE_COM);
            GmailSSLStore gmailSSLStore = (GmailSSLStore) session.getStore(JavaEmailConstants
                    .PROTOCOL_GIMAPS);
            gmailSSLStore.connect(JavaEmailConstants.GMAIL_IMAP_SERVER, account.name, token);
            GmailFolder gmailFolder = (GmailFolder) gmailSSLStore.getFolder(
                    GmailConstants.FOLDER_NAME_INBOX);
            gmailFolder.open(Folder.READ_ONLY);

            Message[] foundMessages = gmailFolder.search(
                    new GmailRawSearchTerm(
                            new Js(getContext()).api_gmail_query_backups(account.name)));

            for (Message message : foundMessages) {
                if (message.getContentType().contains(JavaEmailConstants.CONTENT_TYPE_MULTIPART)) {
                    Multipart multiPart = (Multipart) message.getContent();
                    int numberOfParts = multiPart.getCount();
                    for (int partCount = 0; partCount < numberOfParts; partCount++) {
                        BodyPart bodyPart = multiPart.getBodyPart(partCount);
                        if (bodyPart instanceof MimeBodyPart) {
                            MimeBodyPart mimeBodyPart = (MimeBodyPart) bodyPart;
                            if (Part.ATTACHMENT.equalsIgnoreCase(mimeBodyPart.getDisposition())) {
                                String filePath = getContext().getFilesDir().getPath()
                                        + File.separator + mimeBodyPart.getFileName();
                                mimeBodyPart.saveFile(filePath);
                                filesPaths.add(filePath);
                            }
                        }
                    }
                }
            }

            gmailFolder.close(false);
            gmailSSLStore.close();
            return filesPaths;
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
