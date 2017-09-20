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
import com.flowcrypt.email.api.email.sync.SyncListener;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.security.SecurityStorageConnector;
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
    public void runSMTPAction(AccountDao accountDao, Session session, SyncListener syncListener) throws Exception {
        super.runSMTPAction(accountDao, session, syncListener);

        if (syncListener != null && !TextUtils.isEmpty(accountName)) {
            Message message = generateMessage(syncListener.getContext(), session);

            Transport transport = prepareTransportForSmtp(syncListener.getContext(), session, accountDao);

            transport.sendMessage(message, message.getAllRecipients());

            syncListener.onMessageWithBackupToKeyOwnerSent(accountDao, ownerKey, requestCode, true);
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
                    (context, privateKeyInfoList, i);
            multipart.addBodyPart(attachmentsBodyPart);
        }

        message.setContent(multipart);
        return message;
    }

    /**
     * Generate a {@link BodyPart} with private key as an attachment.
     *
     * @param context            Interface to global information about an application environment;
     * @param privateKeyInfoList The list of the private key info.
     * @param i                  The position in list.
     * @return {@link BodyPart} with private key as an attachment.
     * @throws Exception will occur when generate this {@link BodyPart}.
     */
    @NonNull
    private BodyPart generateAttachmentBodyPartWithPrivateKey(
            Context context, List<PrivateKeyInfo> privateKeyInfoList, int i)
            throws Exception {
        Js js = new Js(context, new SecurityStorageConnector(context));

        PrivateKeyInfo privateKeyInfo = privateKeyInfoList.get(i);
        BodyPart attachmentsBodyPart = new MimeBodyPart();
        String attachmentName = SecurityUtils.generateNameForPrivateKey(accountName +
                "_" + i);

        String decryptedKeyFromDatabase = privateKeyInfo.getPgpKeyInfo().getArmored();
        PgpKey pgpKey = js.crypto_key_read(decryptedKeyFromDatabase);
        pgpKey.encrypt(privateKeyInfo.getPassphrase());

        DataSource dataSource = new ByteArrayDataSource(pgpKey.armor(), JavaEmailConstants
                .MIME_TYPE_TEXT_PLAIN);
        attachmentsBodyPart.setDataHandler(new DataHandler(dataSource));
        attachmentsBodyPart.setFileName(attachmentName);
        return attachmentsBodyPart;
    }
}
