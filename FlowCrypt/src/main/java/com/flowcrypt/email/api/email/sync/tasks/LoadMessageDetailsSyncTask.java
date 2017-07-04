/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.os.Messenger;
import android.text.TextUtils;

import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.js.MimeMessage;
import com.sun.mail.gimap.GmailSSLStore;
import com.sun.mail.imap.IMAPBodyPart;
import com.sun.mail.imap.IMAPFolder;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeBodyPart;

/**
 * This task load a detail information of the some message.
 *
 * @author DenBond7
 *         Date: 26.06.2017
 *         Time: 17:41
 *         E-mail: DenBond7@gmail.com
 */

public class LoadMessageDetailsSyncTask extends BaseSyncTask {
    private static final String PARAMETER_NAME_BOUNDARY = "boundary";

    private long uid;
    private String folderName;

    /**
     * The base constructor.
     *
     * @param ownerKey    The name of the reply to {@link Messenger}.
     * @param requestCode The unique request code for the reply to {@link Messenger}.
     * @param folderName  The folder name where a message exists.
     * @param uid         The {@link com.sun.mail.imap.protocol.UID} of {@link Message).
     */
    public LoadMessageDetailsSyncTask(String ownerKey, int requestCode, String folderName, long
            uid) {
        super(ownerKey, requestCode);
        this.folderName = folderName;
        this.uid = uid;
    }

    @Override
    public void run(GmailSSLStore gmailSSLStore, SyncListener syncListener) throws Exception {
        IMAPFolder imapFolder = (IMAPFolder) gmailSSLStore.getFolder(folderName);
        imapFolder.open(Folder.READ_WRITE);

        if (syncListener != null) {
            Message message = imapFolder.getMessageByUID(uid);
            String rawMessage =
                    getRawMessageWithoutAttachments((javax.mail.internet.MimeMessage) message);
            syncListener.onMessageDetailsReceived(imapFolder, message, rawMessage, ownerKey,
                    requestCode);
        }

        imapFolder.close(false);
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
}
