/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.gmail.GmailApiHelper;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.protocol.CustomFetchProfileItem;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.security.model.PrivateKeyInfo;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.SharedPreferencesHelper;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.sun.mail.imap.IMAPFolder;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.FetchProfile;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.UIDFolder;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

/**
 * @author Denis Bondarenko
 * Date: 29.09.2017
 * Time: 15:31
 * E-mail: DenBond7@gmail.com
 */

public class EmailUtil {
    public static final String PATTERN_FORWARDED_DATE = "EEE, MMM d, yyyy HH:mm:ss";
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
     * @param accountName       The account name;
     * @param armoredPrivateKey The  armored private key.
     * @param i                 The unique index of attachment.
     * @return {@link BodyPart} with private key as an attachment.
     * @throws Exception will occur when generate this {@link BodyPart}.
     */
    @NonNull
    public static MimeBodyPart generateAttachmentBodyPartWithPrivateKey(String accountName,
                                                                        String armoredPrivateKey,
                                                                        int i) throws Exception {
        MimeBodyPart attachmentsBodyPart = new MimeBodyPart();
        String attachmentName = SecurityUtils.generateNameForPrivateKey(accountName + (i >= 0 ? ("_" + i) : ""));

        DataSource dataSource = new ByteArrayDataSource(armoredPrivateKey, JavaEmailConstants.MIME_TYPE_TEXT_PLAIN);
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

            MimeBodyPart attachmentsBodyPart = generateAttachmentBodyPartWithPrivateKey(accountName, pgpKey.armor(), i);
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
        return GoogleAuthUtil.getToken(context, account, JavaEmailConstants.OAUTH2 + GmailScopes.MAIL_GOOGLE_COM);
    }

    /**
     * Check is debug IMAP and SMTP protocols enable.
     *
     * @param context Interface to global information about an application environment;
     * @return true if debug enable, false - otherwise.
     */
    public static boolean isDebugEnable(Context context) {
        Context appContext = context.getApplicationContext();
        return BuildConfig.DEBUG && SharedPreferencesHelper.getBoolean(PreferenceManager.getDefaultSharedPreferences
                (appContext), Constants.PREFERENCES_KEY_IS_MAIL_DEBUG_ENABLE, BuildConfig.IS_MAIL_DEBUG_ENABLE);
    }

    /**
     * Get a list of {@link KeyDetails} using the <b>Gmail API</b>
     *
     * @param context    context Interface to global information about an application environment;
     * @param accountDao An {@link AccountDao} object.
     * @param session    A {@link Session} object.
     * @return A list of {@link KeyDetails}
     * @throws MessagingException
     * @throws IOException
     */
    public static Collection<? extends KeyDetails> getPrivateKeyBackupsUsingGmailAPI(Context context,
                                                                                     AccountDao accountDao,
                                                                                     Session session)
            throws IOException, MessagingException {
        ArrayList<KeyDetails> privateKeyDetailsList = new ArrayList<>();
        String search = new Js(context, null).api_gmail_query_backups(accountDao.getEmail());
        Gmail gmailApiService = GmailApiHelper.generateGmailApiService(context, accountDao);

        ListMessagesResponse listMessagesResponse = gmailApiService
                .users()
                .messages()
                .list(GmailApiHelper.DEFAULT_USER_ID)
                .setQ(search)
                .execute();

        List<com.google.api.services.gmail.model.Message> messages = new ArrayList<>();

        //Try to load all backups
        while (listMessagesResponse.getMessages() != null) {
            messages.addAll(listMessagesResponse.getMessages());
            if (listMessagesResponse.getNextPageToken() != null) {
                String pageToken = listMessagesResponse.getNextPageToken();
                listMessagesResponse = gmailApiService
                        .users()
                        .messages()
                        .list(GmailApiHelper.DEFAULT_USER_ID)
                        .setQ(search)
                        .setPageToken(pageToken)
                        .execute();
            } else {
                break;
            }
        }

        for (com.google.api.services.gmail.model.Message originalMessage : messages) {
            com.google.api.services.gmail.model.Message message = gmailApiService
                    .users()
                    .messages()
                    .get(GmailApiHelper.DEFAULT_USER_ID, originalMessage.getId())
                    .setFormat(GmailApiHelper.MESSAGE_RESPONSE_FORMAT_RAW)
                    .execute();

            MimeMessage mimeMessage = new MimeMessage(session,
                    new ByteArrayInputStream(Base64.decodeBase64(message.getRaw())));

            String key = getKeyFromMessageIfItExists(mimeMessage);
            if (!TextUtils.isEmpty(key) && privateKeyNotExistsInList(privateKeyDetailsList, key)) {
                privateKeyDetailsList.add(new KeyDetails(key, KeyDetails.Type.EMAIL));
            }
        }

        return privateKeyDetailsList;
    }

    /**
     * Get a private key from {@link Message}, if it exists in.
     *
     * @param message The original {@link Message} object.
     * @return <tt>String</tt> A private key.
     * @throws MessagingException
     * @throws IOException
     */
    public static String getKeyFromMessageIfItExists(Message message) throws MessagingException, IOException {
        if (message.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
            Multipart multiPart = (Multipart) message.getContent();
            int numberOfParts = multiPart.getCount();
            for (int partCount = 0; partCount < numberOfParts; partCount++) {
                BodyPart bodyPart = multiPart.getBodyPart(partCount);
                if (bodyPart instanceof MimeBodyPart) {
                    MimeBodyPart mimeBodyPart = (MimeBodyPart) bodyPart;
                    if (Part.ATTACHMENT.equalsIgnoreCase(mimeBodyPart.getDisposition())) {
                        return IOUtils.toString(mimeBodyPart.getInputStream(), StandardCharsets.UTF_8);
                    }
                }
            }
        }

        return null;
    }

    @NonNull
    public static BodyPart getBodyPartWithBackupText(Context context) throws MessagingException, IOException {
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setContent(IOUtils.toString(
                context.getAssets().open(HTML_EMAIL_INTRO_TEMPLATE_HTM),
                StandardCharsets.UTF_8), JavaEmailConstants.MIME_TYPE_TEXT_HTML);
        return messageBodyPart;
    }

    @NonNull
    public static Message generateMessageWithBackupTemplate(Context context, String accountName, Session session)
            throws MessagingException {
        Message message = new MimeMessage(session);

        message.setFrom(new InternetAddress(accountName));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(accountName));
        message.setSubject(context.getString(R.string.your_key_backup, context.getString(R.string.app_name)));
        return message;
    }

    /**
     * Check is the private key exists in the keys list.
     *
     * @param keyDetailsList The list of {@link KeyDetails} objects.
     * @param key            The private key armored string.
     * @return true if the key not exists in the list, otherwise false.
     */
    public static boolean privateKeyNotExistsInList(ArrayList<KeyDetails> keyDetailsList,
                                                    String key) {
        for (KeyDetails keyDetails : keyDetailsList) {
            if (key.equals(keyDetails.getValue())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Prepare the input HTML to show the user a viewport option.
     *
     * @return A generated HTML page which will be more comfortable for user.
     */
    @NonNull
    public static String prepareViewportHtml(String incomingHtml) {
        String body;
        if (Pattern.compile("<html.*?>", Pattern.DOTALL).matcher(incomingHtml).find()) {
            Pattern patternBody = Pattern.compile("<body.*?>(.*?)</body>", Pattern.DOTALL);
            Matcher matcherBody = patternBody.matcher(incomingHtml);
            if (matcherBody.find()) {
                body = matcherBody.group();
            } else {
                body = "<body>" + incomingHtml + "</body>";
            }
        } else {
            body = "<body>" + incomingHtml + "</body>";
        }

        return "<!DOCTYPE html><html><head><meta name=\"viewport\" content=\"width=device-width" +
                "\" /><style>img{display: inline !important ;height: auto !important; max-width:" +
                " 100% !important;}</style></head>" + body + "</html>";
    }

    /**
     * Prepare a formatted date string for a forwarded message. For example <code>Tue, Apr 3, 2018 at 3:07 PM.</code>
     *
     * @return A generated formatted date string.
     */
    @NonNull
    public static String prepareDateForForwardedMessage(Date date) {
        if (date == null) {
            return "";
        }

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(PATTERN_FORWARDED_DATE, Locale.US);
        return simpleDateFormat.format(date);
    }

    /**
     * Prepare a fetch command from the given {@link FetchProfile}
     *
     * @param fetchProfile    The given {@link FetchProfile}
     * @param isRev1          The protocol revision number
     * @param envelopeCommand The envelope command
     * @return A generated fetch command
     */
    public static StringBuilder prepareFetchCommand(FetchProfile fetchProfile, boolean isRev1, String envelopeCommand) {
        StringBuilder command = new StringBuilder();
        boolean first = true;

        if (fetchProfile.contains(FetchProfile.Item.ENVELOPE)) {
            command.append(envelopeCommand);
            first = false;
        }

        if (fetchProfile.contains(FetchProfile.Item.FLAGS)) {
            command.append(first ? "FLAGS" : " FLAGS");
            first = false;
        }

        if (fetchProfile.contains(FetchProfile.Item.CONTENT_INFO)) {
            command.append(first ? "BODYSTRUCTURE" : " BODYSTRUCTURE");
            first = false;
        }

        if (fetchProfile.contains(UIDFolder.FetchProfileItem.UID)) {
            command.append(first ? "UID" : " UID");
            first = false;
        }

        if (fetchProfile.contains(IMAPFolder.FetchProfileItem.HEADERS)) {
            if (isRev1)
                command.append(first ?
                        "BODY.PEEK[HEADER]" : " BODY.PEEK[HEADER]");
            else
                command.append(first ? "RFC822.HEADER" : " RFC822.HEADER");
            first = false;
        }

        if (fetchProfile.contains(IMAPFolder.FetchProfileItem.MESSAGE)) {
            if (isRev1)
                command.append(first ? "BODY.PEEK[]" : " BODY.PEEK[]");
            else
                command.append(first ? "RFC822" : " RFC822");
            first = false;
        }

        if (fetchProfile.contains(FetchProfile.Item.SIZE) ||
                fetchProfile.contains(IMAPFolder.FetchProfileItem.SIZE)) {
            command.append(first ? "RFC822.SIZE" : " RFC822.SIZE");
            first = false;
        }

        if (fetchProfile.contains(IMAPFolder.FetchProfileItem.INTERNALDATE)) {
            command.append(first ? "INTERNALDATE" : " INTERNALDATE");
            first = false;
        }

        for (FetchProfile.Item item : fetchProfile.getItems()) {
            if (item instanceof CustomFetchProfileItem) {
                CustomFetchProfileItem customFetchProfileItem = (CustomFetchProfileItem) item;
                if (!first) {
                    command.append(" ");
                }

                command.append(customFetchProfileItem.getValue());
            }
        }

        return command;
    }

    /**
     * Generated a list of UID of the local messages which will be removed.
     *
     * @param messagesUIDInLocalDatabase The list of UID of the local messages.
     * @param imapFolder                 The remote {@link IMAPFolder}.
     * @param messages                   The array of incoming messages.
     * @return A list of UID of the local messages which will be removed.
     */
    public static Collection<Long> generateDeleteCandidates(Collection<Long> messagesUIDInLocalDatabase,
                                                            IMAPFolder imapFolder, javax.mail.Message[] messages) {
        Collection<Long> uidListDeleteCandidates = new HashSet<>(messagesUIDInLocalDatabase);
        Collection<Long> uidList = new HashSet<>();
        try {
            for (javax.mail.Message message : messages) {
                uidList.add(imapFolder.getUID(message));
            }
        } catch (MessagingException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }

        uidListDeleteCandidates.removeAll(uidList);
        return uidListDeleteCandidates;
    }

    /**
     * Generate an array of {@link javax.mail.Message} which contains candidates for insert.
     *
     * @param messagesUIDInLocalDatabase The list of UID of the local messages.
     * @param imapFolder                 The remote {@link IMAPFolder}.
     * @param messages                   The array of incoming messages.
     * @return The generated array.
     */
    public static javax.mail.Message[] generateNewCandidates(Collection<Long> messagesUIDInLocalDatabase,
                                                             IMAPFolder imapFolder, javax.mail.Message[] messages) {
        List<javax.mail.Message> newCandidates = new ArrayList<>();
        try {
            for (javax.mail.Message message : messages) {
                if (!messagesUIDInLocalDatabase.contains(imapFolder.getUID(message))) {
                    newCandidates.add(message);
                }
            }
        } catch (MessagingException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
        return newCandidates.toArray(new javax.mail.Message[0]);
    }

    /**
     * Generate an array of the messages which will be updated.
     *
     * @param messagesUIDWithFlagsInLocalDatabase The map of UID and flags of the local messages.
     * @param imapFolder                          The remote {@link IMAPFolder}.
     * @param messages                            The array of incoming messages.
     * @return An array of the messages which are candidates for updating iin the local database.
     */
    public static javax.mail.Message[] generateUpdateCandidates(
            Map<Long, String> messagesUIDWithFlagsInLocalDatabase,
            IMAPFolder imapFolder, javax.mail.Message[] messages) {
        Collection<javax.mail.Message> updateCandidates = new ArrayList<>();
        try {
            for (javax.mail.Message message : messages) {
                String flags = messagesUIDWithFlagsInLocalDatabase.get(imapFolder.getUID(message));
                if (flags == null) {
                    flags = "";
                }

                if (!flags.equalsIgnoreCase(message.getFlags().toString())) {
                    updateCandidates.add(message);
                }
            }
        } catch (MessagingException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
        return updateCandidates.toArray(new javax.mail.Message[0]);
    }

    /**
     * Get the personal name of the first address from an array. If the given name is null we will return the email
     * address.
     *
     * @param internetAddresses An array of {@link InternetAddress}
     * @return The first address as a human readable string or email.
     */
    public static String getFirstAddressString(InternetAddress[] internetAddresses) {
        if (internetAddresses == null || internetAddresses.length == 0) {
            return "";
        }

        if (TextUtils.isEmpty(internetAddresses[0].getPersonal())) {
            return internetAddresses[0].getAddress();
        } else {
            return internetAddresses[0].getPersonal();
        }
    }
}
