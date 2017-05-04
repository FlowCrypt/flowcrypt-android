package com.flowcrypt.email.ui.loader;

import android.accounts.Account;
import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.MessageInfo;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.sun.mail.gimap.GmailSSLStore;
import com.sun.mail.imap.IMAPFolder;

import org.jsoup.Jsoup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;

/**
 * This loader download some information about a message and return
 * {@link MessageInfo} object.
 *
 * @author DenBond7
 *         Date: 04.05.2017
 *         Time: 9:59
 *         E-mail: DenBond7@gmail.com
 */

public class LoadMessageInfoAsyncTaskLoader extends AsyncTaskLoader<MessageInfo> {

    private Account account;
    private GeneralMessageDetails generalMessageDetails;

    public LoadMessageInfoAsyncTaskLoader(Context context, Account account, GeneralMessageDetails
            generalMessageDetails) {
        super(context);
        this.account = account;
        this.generalMessageDetails = generalMessageDetails;
        onContentChanged();
    }

    @Override
    public void onStartLoading() {
        if (takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    public MessageInfo loadInBackground() {
        try {
            String token = GoogleAuthUtil.getToken(getContext(), account,
                    JavaEmailConstants.OAUTH2 + GmailConstants.SCOPE_MAIL_GOOGLE_COM);
            GmailSSLStore gmailSSLStore = OpenStoreHelper.openAndConnectToGimapsStore(token,
                    account.name);

            IMAPFolder imapFolder = (IMAPFolder) gmailSSLStore.getFolder(GmailConstants
                    .FOLDER_NAME_INBOX);
            imapFolder.open(Folder.READ_ONLY);

            Message message = imapFolder.getMessageByUID(generalMessageDetails.getUid());
            MessageInfo messageInfo = parseMessage(message);

            imapFolder.close(false);
            gmailSSLStore.close();
            return messageInfo;
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
     * Parse an original message and return {@link MessageInfo} object.
     *
     * @param message Original message which will be parsed.
     * @return <tt>MessageInfo</tt> Return a MessageInfo object.
     * @throws Exception The parsing process can be throws different exceptions.
     */
    private MessageInfo parseMessage(Message message) throws Exception {
        MessageInfo messageInfo = new MessageInfo();
        List<Address> addressList = Arrays.asList(message.getFrom());
        ArrayList<String> addresses = new ArrayList<>();
        for (Address address : addressList) {
            if (address instanceof InternetAddress) {
                addresses.add(((InternetAddress) address).getAddress());
            }
        }

        messageInfo.setFrom(addresses);
        messageInfo.setSubject(message.getSubject());
        messageInfo.setReceiveDate(message.getReceivedDate());
        messageInfo.setMessage(parseSimpleText(message));
        return messageInfo;
    }

    /**
     * Parse and return a simple not formatted text from the message.
     *
     * @param message Original message which will be parsed.
     * @return <tt>String</tt> Return a simple not formatted text.
     * @throws Exception The parsing process can be throws different exceptions.
     */
    private String parseSimpleText(Message message) throws Exception {
        String result = null;
        if (message.isMimeType(JavaEmailConstants.MIME_TYPE_TEXT_PLAIN)) {
            result = message.getContent().toString();
        } else if (message.isMimeType(JavaEmailConstants.MIME_TYPE_TEXT_HTML)) {
            String html = (String) message.getContent();
            result = Jsoup.parse(html).text();
        } else if (message.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            result = getTextFromMimeMultipart(mimeMultipart);
        }
        return result;
    }

    /**
     * Parse and return a simple not formatted text from the MimeMultipart object.
     *
     * @param mimeMultipart Original MimeMultipart object which will be parsed.
     * @return <tt>String</tt> Return a simple not formatted text.
     * @throws Exception The parsing process can be throws different exceptions.
     */
    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws Exception {
        String result = "";
        if (mimeMultipart != null) {
            for (int i = 0; i < mimeMultipart.getCount(); i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_TEXT_PLAIN)) {
                    result += bodyPart.getContent() + "\n";
                    break;

                } else if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_TEXT_HTML)) {
                    String html = (String) bodyPart.getContent();
                    result += Jsoup.parse(html).text() + "\n";
                    break;

                } else if (bodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
                    result += getTextFromMimeMultipart((MimeMultipart) bodyPart
                            .getContent());
                }
            }
        }
        return result;
    }
}
