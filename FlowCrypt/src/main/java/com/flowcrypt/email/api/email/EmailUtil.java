/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
import android.text.TextUtils;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.gmail.GmailConstants;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.security.model.PrivateKeyInfo;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.SharedPreferencesHelper;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.sun.mail.imap.IMAPFolder;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

/**
 * @author Denis Bondarenko
 *         Date: 29.09.2017
 *         Time: 15:31
 *         E-mail: DenBond7@gmail.com
 */

public class EmailUtil {
    private static final String HTML_EMAIL_INTRO_TEMPLATE_HTM = "html/email_intro.template.htm";

    /**
     * Generate an unique content id.
     *
     * @return A generated unique content id.
     */
    public static String generateContentId() {
        return "<" + UUID.randomUUID().toString() + "@flowcrypt" + ">";
    }

    /**
     * Check if current folder has {@link JavaEmailConstants#FOLDER_ATTRIBUTE_NO_SELECT}. If the
     * folder contains it attribute we will not show this folder in the list.
     *
     * @param imapFolder The {@link IMAPFolder} object.
     * @return true if current folder contains attribute
     * {@link JavaEmailConstants#FOLDER_ATTRIBUTE_NO_SELECT}, false otherwise.
     * @throws MessagingException
     */
    public static boolean isFolderHasNoSelectAttribute(IMAPFolder imapFolder) throws MessagingException {
        List<String> attributes = Arrays.asList(imapFolder.getAttributes());
        return attributes.contains(JavaEmailConstants.FOLDER_ATTRIBUTE_NO_SELECT);
    }

    /**
     * Get a domain of some email.
     *
     * @return The domain of some email.
     */
    public static String getDomain(String email) {
        if (TextUtils.isEmpty(email)) {
            return "";
        } else if (email.contains("@")) {
            return email.substring(email.indexOf('@') + 1, email.length());
        } else {
            return "";
        }
    }

    /**
     * Generate {@link AttachmentInfo} from the requested information from the file uri.
     *
     * @param uri The file {@link Uri}
     * @return Generated {@link AttachmentInfo}.
     */
    public static AttachmentInfo getAttachmentInfoFromUri(Context context, Uri uri) {
        if (context != null && uri != null) {
            AttachmentInfo attachmentInfo = new AttachmentInfo();
            attachmentInfo.setUri(uri);
            attachmentInfo.setType(GeneralUtil.getFileMimeTypeFromUri(context, uri));

            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    attachmentInfo.setName(cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)));
                    attachmentInfo.setEncodedSize(cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE)));
                }
                cursor.close();
            } else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
                attachmentInfo.setName(GeneralUtil.getFileNameFromUri(context, uri));
                attachmentInfo.setEncodedSize(GeneralUtil.getFileSizeFromUri(context, uri));
            }

            return attachmentInfo;
        } else return null;
    }

    /**
     * Generate {@link AttachmentInfo} using the sender public key.
     *
     * @param publicKey The sender public key
     * @return A generated {@link AttachmentInfo}.
     */
    @Nullable
    public static AttachmentInfo generateAttachmentInfoFromPublicKey(PgpKey publicKey) {
        if (publicKey != null) {
            String fileName = "0x" + publicKey.getLongid().toUpperCase() + ".asc";
            String publicKeyValue = publicKey.armor();

            if (!TextUtils.isEmpty(publicKeyValue)) {
                AttachmentInfo attachmentInfo = new AttachmentInfo();

                attachmentInfo.setName(fileName);
                attachmentInfo.setEncodedSize(publicKeyValue.length());
                attachmentInfo.setRawData(publicKeyValue);
                attachmentInfo.setType(Constants.MIME_TYPE_PGP_KEY);
                attachmentInfo.setEmail(publicKey.getPrimaryUserId().getEmail());

                return attachmentInfo;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Generate a {@link BodyPart} with a private key as an attachment.
     *
     * @param accountName The account name;
     * @param pgpKey      The private key.
     * @param i           The unique index of attachment.
     * @return {@link BodyPart} with private key as an attachment.
     * @throws Exception will occur when generate this {@link BodyPart}.
     */
    @NonNull
    public static MimeBodyPart generateAttachmentBodyPartWithPrivateKey(String accountName,
                                                                        PgpKey pgpKey,
                                                                        int i) throws Exception {
        MimeBodyPart attachmentsBodyPart = new MimeBodyPart();
        String attachmentName = SecurityUtils.generateNameForPrivateKey(accountName + (i >= 0 ? ("_" + i) : ""));

        DataSource dataSource = new ByteArrayDataSource(pgpKey.armor(), JavaEmailConstants.MIME_TYPE_TEXT_PLAIN);
        attachmentsBodyPart.setDataHandler(new DataHandler(dataSource));
        attachmentsBodyPart.setFileName(attachmentName);
        return attachmentsBodyPart;
    }

    /**
     * Generate a message with the html pattern and the private key(s) as an attachment.
     *
     * @param context     Interface to global information about an application environment;
     * @param accountName The account name;
     * @param session     The current session.
     * @return Generated {@link Message} object.
     * @throws Exception will occur when generate this message.
     */
    @NonNull
    public static Message generateMessageWithAllPrivateKeysBackups(Context context, String accountName, Session session)
            throws Exception {
        Message message = generateMessageWithBackupTemplate(context, accountName, session);

        Multipart multipart = new MimeMultipart();
        BodyPart messageBodyPart = getBodyPartWithBackupText(context);
        multipart.addBodyPart(messageBodyPart);

        List<PrivateKeyInfo> privateKeyInfoList = SecurityUtils.getPrivateKeysInfo(context);
        Js js = new Js(context, new SecurityStorageConnector(context));

        for (int i = 0; i < privateKeyInfoList.size(); i++) {
            PrivateKeyInfo privateKeyInfo = privateKeyInfoList.get(i);

            String decryptedKeyFromDatabase = privateKeyInfo.getPgpKeyInfo().getPrivate();
            PgpKey pgpKey = js.crypto_key_read(decryptedKeyFromDatabase);
            pgpKey.encrypt(privateKeyInfo.getPassphrase());

            MimeBodyPart attachmentsBodyPart = generateAttachmentBodyPartWithPrivateKey(accountName, pgpKey, i);
            attachmentsBodyPart.setContentID(EmailUtil.generateContentId());
            multipart.addBodyPart(attachmentsBodyPart);
        }

        message.setContent(multipart);
        return message;
    }

    /**
     * Generate a message with the html pattern and the private key as an attachment.
     *
     * @param context     Interface to global information about an application environment;
     * @param accountName The account name;
     * @param session     The current session.
     * @return Generated {@link Message} object.
     * @throws Exception will occur when generate this message.
     */
    @NonNull
    public static Message generateMessageWithPrivateKeysBackup(Context context, String accountName, Session session,
                                                               MimeBodyPart mimeBodyPartPrivateKey)
            throws Exception {
        Message message = generateMessageWithBackupTemplate(context, accountName, session);
        Multipart multipart = new MimeMultipart();
        BodyPart messageBodyPart = getBodyPartWithBackupText(context);
        multipart.addBodyPart(messageBodyPart);
        mimeBodyPartPrivateKey.setContentID(EmailUtil.generateContentId());
        multipart.addBodyPart(mimeBodyPartPrivateKey);
        message.setContent(multipart);
        return message;
    }

    /**
     * Get a valid OAuth2 token for some {@link Account}. Must be called on the non-UI thread.
     *
     * @return A new valid OAuth2 token;
     * @throws IOException         Signaling a transient error (typically network related). It is left to clients to
     *                             implement a backoff/abandonment strategy appropriate to their latency requirements.
     * @throws GoogleAuthException Signaling an unrecoverable authentication error. These errors will typically
     *                             result from client errors (e.g. providing an invalid scope).
     */
    public static String getTokenForGmailAccount(Context context, @NonNull Account account) throws IOException,
            GoogleAuthException {
        return GoogleAuthUtil.getToken(context, account, JavaEmailConstants.OAUTH2 + GmailConstants
                .SCOPE_MAIL_GOOGLE_COM);
    }

    /**
     * Check is debug IMAP and SMTP protocols enable.
     *
     * @param context Interface to global information about an application environment;
     * @return true if debug enable, false - otherwise.
     */
    public static boolean isDebugEnable(Context context) {
        Context appContext = context.getApplicationContext();

        return SharedPreferencesHelper.getBoolean(PreferenceManager.getDefaultSharedPreferences(appContext),
                Constants.PREFERENCES_KEY_IS_JAVA_MAIL_DEBUG_ENABLE, false);
    }

    @NonNull
    private static BodyPart getBodyPartWithBackupText(Context context) throws MessagingException, IOException {
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(IOUtils.toString(
                context.getAssets().open(HTML_EMAIL_INTRO_TEMPLATE_HTM),
                StandardCharsets.UTF_8), JavaEmailConstants.MIME_TYPE_TEXT_HTML);
        return messageBodyPart;
    }

    @NonNull
    private static Message generateMessageWithBackupTemplate(Context context, String accountName, Session session)
            throws MessagingException {
        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress(accountName));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(accountName));
        message.setSubject(context.getString(R.string.your_key_backup, context.getString(R.string.app_name)));
        return message;
    }
}
