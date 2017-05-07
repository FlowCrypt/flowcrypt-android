package com.flowcrypt.email.ui.loader;

import android.accounts.Account;
import android.content.Context;
import android.os.Build;
import android.support.v4.content.AsyncTaskLoader;
import android.text.Html;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.MessageInfo;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.test.Js;
import com.flowcrypt.email.test.MimeAddress;
import com.flowcrypt.email.test.MimeMessage;
import com.flowcrypt.email.test.PgpDecrypted;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.sun.mail.gimap.GmailSSLStore;
import com.sun.mail.imap.IMAPFolder;

import org.jsoup.Jsoup;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
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
        String rawMIMEMessage = null;

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            message.writeTo(output);
            rawMIMEMessage = output.toString();
        } catch (IOException | MessagingException e) {
            e.printStackTrace();
        }

        if (rawMIMEMessage != null) {
            Js js = new Js(getContext(), new SecurityStorageConnector(getContext()));
            MimeMessage mimeMessage = js.mime_decode(rawMIMEMessage);
            ArrayList<String> addresses = new ArrayList<>();

            for (MimeAddress mimeAddress : mimeMessage.getAddressHeader("from")) {
                addresses.add(mimeAddress.getAddress());
            }

            messageInfo.setFrom(addresses);
            messageInfo.setSubject(mimeMessage.getStringHeader("subject"));
            messageInfo.setReceiveDate(new Date(mimeMessage.getTimeHeader("date")));
            messageInfo.setMessage(decryptMessageIfNeed(js, mimeMessage));
        } else {
            return null;
        }
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

    /**
     * Decrypt a message if it decrypted;
     *
     * @param js          The Js object which used to decrypt a message text.
     * @param mimeMessage The MimeMessage object.
     * @return <tt>String</tt> Return a decrypted or original text.
     */
    @SuppressWarnings("deprecation")
    private String decryptMessageIfNeed(Js js, MimeMessage mimeMessage) {
        if (TextUtils.isEmpty(mimeMessage.getText())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return Html.fromHtml(mimeMessage.getHtml(), Html.FROM_HTML_MODE_LEGACY).toString();
            } else {
                return Html.fromHtml(mimeMessage.getHtml()).toString();
            }
        } else {
            String decryptedText = js.crypto_armor_clip(mimeMessage.getText());
            if (decryptedText != null) {
                PgpDecrypted pgpDecrypted = js.crypto_message_decrypt(decryptedText);
                try {
                    return pgpDecrypted != null ? pgpDecrypted.getContent() : "";
                } catch (Exception e) {
                    e.printStackTrace();
                    return mimeMessage.getText();
                }
            } else {
                return mimeMessage.getText();
            }
        }
    }
}
