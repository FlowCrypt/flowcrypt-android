package com.flowcrypt.email.ui.loader;

import android.accounts.Account;
import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.test.Js;
import com.flowcrypt.email.test.SampleStorageConnector;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.sun.mail.gimap.GmailFolder;
import com.sun.mail.gimap.GmailRawSearchTerm;
import com.sun.mail.gimap.GmailSSLStore;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeBodyPart;

/**
 * This loader find and return a user backup of private keys.
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
        List<String> keys = new ArrayList<>();
        try {
            String token = GoogleAuthUtil.getToken(getContext(), account,
                    JavaEmailConstants.OAUTH2 + GmailConstants.SCOPE_MAIL_GOOGLE_COM);
            GmailSSLStore gmailSSLStore = OpenStoreHelper.openAndConnectToGimapsStore(token,
                    account.name);
            GmailFolder gmailFolder = (GmailFolder) gmailSSLStore.getFolder(
                    GmailConstants.FOLDER_NAME_INBOX);
            gmailFolder.open(Folder.READ_ONLY);

            Message[] foundMessages = gmailFolder.search(
                    new GmailRawSearchTerm(new Js(getContext(),
                            new SampleStorageConnector(getContext()))
                            .api_gmail_query_backups(account.name)));

            for (Message message : foundMessages) {
                String key = getKeyFromMessageIfItExists(message);
                if (!TextUtils.isEmpty(key)) {
                    keys.add(key);
                }
            }

            gmailFolder.close(false);
            gmailSSLStore.close();
            return keys;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }

    /**
     * Get a private key from {@link Message}, if it exists in.
     *
     * @param message The original {@link Message} object.
     * @return <tt>String</tt> A private key.
     * @throws MessagingException
     * @throws IOException
     */
    private String getKeyFromMessageIfItExists(Message message) throws MessagingException,
            IOException {
        if (message.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
            Multipart multiPart = (Multipart) message.getContent();
            int numberOfParts = multiPart.getCount();
            for (int partCount = 0; partCount < numberOfParts; partCount++) {
                BodyPart bodyPart = multiPart.getBodyPart(partCount);
                if (bodyPart instanceof MimeBodyPart) {
                    MimeBodyPart mimeBodyPart = (MimeBodyPart) bodyPart;
                    if (Part.ATTACHMENT.equalsIgnoreCase(mimeBodyPart.getDisposition())) {
                        return IOUtils.toString(mimeBodyPart.getInputStream(),
                                StandardCharsets.UTF_8);
                    }
                }
            }
        }

        return null;
    }
}
