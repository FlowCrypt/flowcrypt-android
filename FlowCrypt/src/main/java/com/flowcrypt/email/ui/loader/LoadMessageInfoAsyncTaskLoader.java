/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/tree/master/src/LICENSE
 * Contributors: DenBond7
 */

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
import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.api.email.model.MessageInfo;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.test.Js;
import com.flowcrypt.email.test.MimeAddress;
import com.flowcrypt.email.test.MimeMessage;
import com.flowcrypt.email.test.PgpDecrypted;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.sun.mail.gimap.GmailSSLStore;
import com.sun.mail.imap.IMAPBodyPart;
import com.sun.mail.imap.IMAPFolder;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;

/**
 * This loader download some information about a message and return
 * {@link MessageInfo} object.
 *
 * @author DenBond7
 *         Date: 04.05.2017
 *         Time: 9:59
 *         E-mail: DenBond7@gmail.com
 */

public class LoadMessageInfoAsyncTaskLoader extends AsyncTaskLoader<LoaderResult> {

    private static final String PARAMETER_NAME_BOUNDARY = "boundary";

    private Account account;
    private GeneralMessageDetails generalMessageDetails;
    private String folderName;

    public LoadMessageInfoAsyncTaskLoader(Context context, Account account, GeneralMessageDetails
            generalMessageDetails, String folderName) {
        super(context);
        this.account = account;
        this.generalMessageDetails = generalMessageDetails;
        this.folderName = folderName;
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

            IMAPFolder imapFolder = (IMAPFolder) gmailSSLStore.getFolder(folderName);
            imapFolder.open(Folder.READ_ONLY);

            Message message = imapFolder.getMessageByUID(generalMessageDetails.getUid());

            String rawMessage =
                    getRawMessageWithoutAttachments((javax.mail.internet.MimeMessage) message);

            IncomingMessageInfo messageInfo = parseRawMessage(rawMessage);

            imapFolder.close(false);
            gmailSSLStore.close();

            return new LoaderResult(messageInfo, null);
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
     * Get the raw MIME message without attachments from the IMAP server for some
     * {@link MimeMessage}.
     *
     * @param message The original message for what will be received the raw MIME message.
     * @return The string which contains the raw MIME message.
     * @throws Exception
     */
    private String getRawMessageWithoutAttachments(javax.mail.internet.MimeMessage message)
            throws Exception {

        String rawMessage = "";

        if (isMessageHasAttachment(message)
                && message.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
            ArrayList headers = Collections.list(message.getAllHeaderLines());
            Multipart multiPart = (Multipart) message.getContent();
            rawMessage += TextUtils.join("\n", headers) + "\n\n";
            rawMessage += "--" + new ContentType(message.getContentType())
                    .getParameter(PARAMETER_NAME_BOUNDARY) + "\n";
            rawMessage += getRawMultipart(multiPart);
            rawMessage += "--" + new ContentType(message.getContentType())
                    .getParameter(PARAMETER_NAME_BOUNDARY) + "--";
        } else {
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                message.writeTo(output);
                rawMessage = output.toString();
            } catch (IOException | MessagingException e) {
                e.printStackTrace();
            }
        }

        return rawMessage;
    }

    /**
     * Check is message has attachments.
     *
     * @param message The original {@link Message}
     * @return true if the message has attachments, false otherwise.
     * @throws MessagingException
     * @throws IOException
     */
    private boolean isMessageHasAttachment(Message message) throws MessagingException, IOException {
        if (message.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
            Multipart mp = (Multipart) message.getContent();
            if (mp.getCount() > 1)
                return true;
        }
        return false;
    }

    /**
     * Generate a raw multipart information for the {@link Multipart} object.
     *
     * @param multipart The input {@link Multipart} object
     * @return A string which contains the raw information about the {@link Multipart} object.
     * @throws IOException
     * @throws MessagingException
     */
    private String getRawMultipart(Multipart multipart) throws IOException,
            MessagingException {
        String rawMultipart = "";

        int numberOfParts = multipart.getCount();
        for (int partCount = 0; partCount < numberOfParts; partCount++) {
            BodyPart bodyPart = multipart.getBodyPart(partCount);
            if (bodyPart instanceof MimeBodyPart) {
                MimeBodyPart mimeBodyPart = (MimeBodyPart) bodyPart;
                if (mimeBodyPart.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
                    rawMultipart += "Content-Type: " + bodyPart.getContentType() + "\n\n";
                    rawMultipart += getRawMultipart((Multipart) mimeBodyPart.getContent());
                    rawMultipart += "--" + new ContentType(bodyPart.getContentType())
                            .getParameter(PARAMETER_NAME_BOUNDARY) + "--" + "\n\n";
                } else {
                    if (!Part.ATTACHMENT.equalsIgnoreCase(mimeBodyPart.getDisposition())) {
                        rawMultipart += "--" + new ContentType(multipart.getContentType())
                                .getParameter(PARAMETER_NAME_BOUNDARY) + "\n";
                        rawMultipart += IOUtils.toString(((IMAPBodyPart) bodyPart).getMimeStream(),
                                StandardCharsets.UTF_8);
                    }
                }
            }
        }

        rawMultipart += "\n";

        return rawMultipart;
    }

    /**
     * Parse an original message and return {@link MessageInfo} object.
     *
     * @param rawMessage Original message which will be parsed.
     * @return <tt>MessageInfo</tt> Return a MessageInfo object.
     * @throws Exception The parsing process can be throws different exceptions.
     */
    private IncomingMessageInfo parseRawMessage(String rawMessage) throws Exception {
        IncomingMessageInfo messageInfo = new IncomingMessageInfo();
        if (rawMessage != null) {
            Js js = new Js(getContext(), new SecurityStorageConnector(getContext()));
            MimeMessage mimeMessage = js.mime_decode(rawMessage);
            ArrayList<String> addresses = new ArrayList<>();

            for (MimeAddress mimeAddress : mimeMessage.getAddressHeader("from")) {
                addresses.add(mimeAddress.getAddress());
            }

            messageInfo.setFrom(addresses);
            messageInfo.setSubject(mimeMessage.getStringHeader("subject"));
            messageInfo.setReceiveDate(new Date(mimeMessage.getTimeHeader("date")));
            messageInfo.setMessage(decryptMessageIfNeed(js, mimeMessage));
            messageInfo.setOriginalRawMessageWithoutAttachments(rawMessage);
        } else {
            return null;
        }
        return messageInfo;
    }

    /**
     * Decrypt a message if it encrypted. At now will be decrypted only a simple text.
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
