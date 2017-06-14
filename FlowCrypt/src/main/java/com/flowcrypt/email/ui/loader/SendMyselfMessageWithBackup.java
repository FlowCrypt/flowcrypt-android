/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (tom@cryptup.org). Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.loader;

import android.accounts.Account;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;

import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.protocol.PropertiesHelper;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.security.model.PrivateKeyInfo;
import com.google.android.gms.auth.GoogleAuthUtil;

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
 * This {@link Loader} describe a logic for sending an email to myself
 * with a private key as an attachment.
 *
 * @author DenBond7
 *         Date: 01.06.2017
 *         Time: 13:40
 *         E-mail: DenBond7@gmail.com
 */

public class SendMyselfMessageWithBackup extends AsyncTaskLoader<LoaderResult> {
    private static final String HTML_EMAIL_INTRO_TEMPLATE_HTM = "html/email_intro.template.htm";
    private Account account;

    public SendMyselfMessageWithBackup(Context context, Account account) {
        super(context);
        this.account = account;
        onContentChanged();
    }

    @Override
    public LoaderResult loadInBackground() {
        try {
            String token = GoogleAuthUtil.getToken(getContext(), account,
                    JavaEmailConstants.OAUTH2 + GmailConstants.SCOPE_MAIL_GOOGLE_COM);

            String username = account.name;
            Session session = Session.getInstance(
                    PropertiesHelper.generatePropertiesForGmailSmtp());

            Message message = generateMessage(session);

            Transport transport = session.getTransport(JavaEmailConstants.PROTOCOL_SMTP);
            transport.connect(GmailConstants.HOST_SMTP_GMAIL_COM,
                    GmailConstants.PORT_SMTP_GMAIL_COM, username, token);

            transport.sendMessage(message, message.getAllRecipients());

            return new LoaderResult(true, null);
        } catch (Exception e) {
            e.printStackTrace();
            return new LoaderResult(null, e);
        }
    }

    @Override
    public void onStopLoading() {
        cancelLoad();
    }

    @Override
    public void onStartLoading() {
        if (takeContentChanged()) {
            forceLoad();
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
    private Message generateMessage(Session session) throws Exception {
        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress(account.name));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(account.name));
        message.setSubject(getContext().getString(R.string.your_key_backup, getContext()
                .getString(R.string.app_name)));

        Multipart multipart = new MimeMultipart();

        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(IOUtils.toString(
                getContext().getAssets().open(HTML_EMAIL_INTRO_TEMPLATE_HTM),
                StandardCharsets.UTF_8), JavaEmailConstants.MIME_TYPE_TEXT_HTML);

        multipart.addBodyPart(messageBodyPart);

        List<PrivateKeyInfo> privateKeyInfoList =
                SecurityUtils.getPrivateKeysInfo(getContext());

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
        String attachmentName = SecurityUtils.generateNameForPrivateKey(account.name +
                "_" + i);
        DataSource dataSource = new ByteArrayDataSource(privateKeyInfo.getPgpKeyInfo()
                .getArmored(), JavaEmailConstants.MIME_TYPE_TEXT_PLAIN);
        attachmentsBodyPart.setDataHandler(new DataHandler(dataSource));
        attachmentsBodyPart.setFileName(attachmentName);
        return attachmentsBodyPart;
    }
}
