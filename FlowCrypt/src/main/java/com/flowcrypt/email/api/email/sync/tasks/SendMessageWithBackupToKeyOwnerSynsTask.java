/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync.tasks;

import android.content.Context;
import android.os.Messenger;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.security.model.PrivateKeyInfo;

import org.apache.commons.io.IOUtils;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

/**
 * This task send a message with backup to the key owner.
 *
 * @author DenBond7
 *         Date: 05.07.2017
 *         Time: 14:08
 *         E-mail: DenBond7@gmail.com
 */

public class SendMessageWithBackupToKeyOwnerSynsTask extends BaseSyncTask {
    private static final String HTML_EMAIL_INTRO_TEMPLATE_HTM = "html/email_intro.template.htm";
    private String accountName;

    /**
     * The base constructor.
     *
     * @param ownerKey    The name of the reply to {@link Messenger}.
     * @param requestCode The unique request code for the reply to {@link Messenger}.
     * @param accountName The account name..
     */
    public SendMessageWithBackupToKeyOwnerSynsTask(String ownerKey, int requestCode,
                                                   String accountName) {
        super(ownerKey, requestCode);
        this.accountName = accountName;
    }

    @Override
    public boolean isUseSMTP() {
        return true;
    }

    @Override
    public void run(Session session, String userName, String password, SyncListener syncListener)
            throws Exception {
        super.run(session, userName, password, syncListener);

        if (syncListener != null && !TextUtils.isEmpty(accountName)) {
            Message message = generateMessage(syncListener.getContext(), session);

            Transport transport = session.getTransport(JavaEmailConstants.PROTOCOL_SMTP);
            transport.connect(GmailConstants.HOST_SMTP_GMAIL_COM,
                    GmailConstants.PORT_SMTP_GMAIL_COM, userName, password);

            transport.sendMessage(message, message.getAllRecipients());

            syncListener.onMessageWithBackupToKeyOwnerSent(ownerKey, requestCode, true);
        }
    }

    /**
     * Generate a message with the html pattern and the private key as an attachment.
     *
     * @param session The current session.
     * @return Generated {@link Message} object.
     * @throws Exception will occur when generate this message.
     */
    @NonNull
    private Message generateMessage(Context context, Session session) throws Exception {
        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress(accountName));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(accountName));
        message.setSubject(context.getString(R.string.your_key_backup, context
                .getString(R.string.app_name)));

        Multipart multipart = new MimeMultipart();

        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(IOUtils.toString(
                context.getAssets().open(HTML_EMAIL_INTRO_TEMPLATE_HTM),
                StandardCharsets.UTF_8), JavaEmailConstants.MIME_TYPE_TEXT_HTML);

        multipart.addBodyPart(messageBodyPart);

        List<PrivateKeyInfo> privateKeyInfoList = SecurityUtils.getPrivateKeysInfo(context);

        for (int i = 0; i < privateKeyInfoList.size(); i++) {
            BodyPart attachmentsBodyPart = generateAttachmentBodyPartWithPrivateKey
                    (privateKeyInfoList, i);
            multipart.addBodyPart(attachmentsBodyPart);
        }

        message.setContent(multipart);
        return message;
    }

    /**
     * Generate a {@link BodyPart} with private key as an attachment.
     *
     * @param privateKeyInfoList The list of the private key info.
     * @param i                  The position in list.
     * @return {@link BodyPart} with private key as an attachment.
     * @throws Exception will occur when generate this {@link BodyPart}.
     */
    @NonNull
    private BodyPart generateAttachmentBodyPartWithPrivateKey(
            List<PrivateKeyInfo> privateKeyInfoList, int i)
            throws Exception {
        PrivateKeyInfo privateKeyInfo = privateKeyInfoList.get(i);
        BodyPart attachmentsBodyPart = new MimeBodyPart();
        String attachmentName = SecurityUtils.generateNameForPrivateKey(accountName +
                "_" + i);
        DataSource dataSource = new ByteArrayDataSource(privateKeyInfo.getPgpKeyInfo()
                .getArmored(), JavaEmailConstants.MIME_TYPE_TEXT_PLAIN);
        attachmentsBodyPart.setDataHandler(new DataHandler(dataSource));
        attachmentsBodyPart.setFileName(attachmentName);
        return attachmentsBodyPart;
    }
}
