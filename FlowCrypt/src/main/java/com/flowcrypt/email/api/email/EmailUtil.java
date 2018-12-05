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
import android.text.TextUtils;
import android.util.Base64;
import android.util.LongSparseArray;
import android.util.SparseArray;

import com.flowcrypt.email.BuildConfig;
import com.flowcrypt.email.Constants;
import com.flowcrypt.email.R;
import com.flowcrypt.email.api.email.gmail.GmailApiHelper;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.GeneralMessageDetails;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.js.MessageBlock;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.SharedPreferencesHelper;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.util.CollectionUtils;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.sun.mail.gimap.GmailRawSearchTerm;
import com.sun.mail.iap.Argument;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.BODY;
import com.sun.mail.imap.protocol.FetchResponse;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.protocol.UID;
import com.sun.mail.imap.protocol.UIDSet;
import com.sun.mail.util.ASCIIUtility;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import javax.mail.MessageRemovedException;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.UIDFolder;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.BodyTerm;
import javax.mail.search.SearchTerm;
import javax.mail.util.ByteArrayDataSource;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

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
   * @param folder The {@link IMAPFolder} object.
   * @return true if current folder contains attribute
   * {@link JavaEmailConstants#FOLDER_ATTRIBUTE_NO_SELECT}, false otherwise.
   * @throws MessagingException
   */
  public static boolean isNoSelectAttributePresented(IMAPFolder folder) throws MessagingException {
    return Arrays.asList(folder.getAttributes()).contains(JavaEmailConstants.FOLDER_ATTRIBUTE_NO_SELECT);
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
      AttachmentInfo attInfo = new AttachmentInfo();
      attInfo.setUri(uri);
      attInfo.setType(GeneralUtil.getFileMimeTypeFromUri(context, uri));
      attInfo.setId(EmailUtil.generateContentId());

      Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
      if (cursor != null) {
        if (cursor.moveToFirst()) {
          attInfo.setName(cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)));
          attInfo.setEncodedSize(cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE)));
        }
        cursor.close();
      } else if (ContentResolver.SCHEME_FILE.equalsIgnoreCase(uri.getScheme())) {
        attInfo.setName(GeneralUtil.getFileNameFromUri(context, uri));
        attInfo.setEncodedSize(GeneralUtil.getFileSizeFromUri(context, uri));
      }

      return attInfo;
    } else return null;
  }

  /**
   * Generate {@link AttachmentInfo} using the sender public key.
   *
   * @param pubKey The sender public key
   * @return A generated {@link AttachmentInfo}.
   */
  @Nullable
  public static AttachmentInfo genAttachmentInfoFromPubKey(PgpKey pubKey) {
    if (pubKey != null) {
      String fileName = "0x" + pubKey.getLongid().toUpperCase() + ".asc";
      String pubKeyValue = pubKey.armor();

      if (!TextUtils.isEmpty(pubKeyValue)) {
        AttachmentInfo attachmentInfo = new AttachmentInfo();

        attachmentInfo.setName(fileName);
        attachmentInfo.setEncodedSize(pubKeyValue.length());
        attachmentInfo.setRawData(pubKeyValue);
        attachmentInfo.setType(Constants.MIME_TYPE_PGP_KEY);
        attachmentInfo.setEmail(pubKey.getPrimaryUserId().getEmail());
        attachmentInfo.setId(EmailUtil.generateContentId());

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
   * @param account      The given account;
   * @param armoredPrKey The armored private key.
   * @return {@link BodyPart} with private key as an attachment.
   * @throws Exception will occur when generate this {@link BodyPart}.
   */
  @NonNull
  public static MimeBodyPart genBodyPartWithPrivateKey(AccountDao account, String armoredPrKey) throws Exception {
    MimeBodyPart part = new MimeBodyPart();
    DataSource dataSource = new ByteArrayDataSource(armoredPrKey, JavaEmailConstants.MIME_TYPE_TEXT_PLAIN);
    part.setDataHandler(new DataHandler(dataSource));
    part.setFileName(SecurityUtils.genNameForPrivateKey(account.getEmail()));
    return part;
  }

  /**
   * Generate a message with the html pattern and the private key(s) as an attachment.
   *
   * @param context Interface to global information about an application environment;
   * @param account The given account;
   * @param session The current sess.
   * @param js      An instance of {@link Js}
   * @return Generated {@link Message} object.
   * @throws Exception will occur when generate this message.
   */
  @NonNull
  public static Message generateMessageWithAllPrivateKeys(Context context, AccountDao account,
                                                          Session session, Js js) throws Exception {
    String keys = SecurityUtils.genPrivateKeysBackup(context, js, account, true);

    Multipart multipart = new MimeMultipart();
    multipart.addBodyPart(getBodyPartWithBackupText(context));

    MimeBodyPart attsPart = genBodyPartWithPrivateKey(account, keys);
    attsPart.setContentID(EmailUtil.generateContentId());
    multipart.addBodyPart(attsPart);

    Message msg = generateMessageWithBackupTemplate(context, account, session);
    msg.setContent(multipart);
    return msg;
  }

  /**
   * Generate a message with the html pattern and the private key as an attachment.
   *
   * @param context Interface to global information about an application environment;
   * @param account The given account;
   * @param session The current sess.
   * @return Generated {@link Message} object.
   * @throws Exception will occur when generate this message.
   */
  @NonNull
  public static Message genMessageWithPrivateKeys(Context context, AccountDao account, Session session,
                                                  MimeBodyPart mimeBodyPartPrivateKey) throws Exception {
    Multipart multipart = new MimeMultipart();
    multipart.addBodyPart(getBodyPartWithBackupText(context));
    mimeBodyPartPrivateKey.setContentID(EmailUtil.generateContentId());
    multipart.addBodyPart(mimeBodyPartPrivateKey);

    Message msg = generateMessageWithBackupTemplate(context, account, session);
    msg.setContent(multipart);
    return msg;
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
  public static String getGmailAccountToken(Context context, @NonNull Account account) throws IOException,
      GoogleAuthException {
    return GoogleAuthUtil.getToken(context, account, JavaEmailConstants.OAUTH2 + GmailScopes.MAIL_GOOGLE_COM);
  }

  /**
   * Check is debug IMAP and SMTP protocols enable.
   *
   * @param context Interface to global information about an application environment;
   * @return true if debug enable, false - otherwise.
   */
  public static boolean isDebugEnabled(Context context) {
    Context appContext = context.getApplicationContext();
    return GeneralUtil.isDebugBuild() && SharedPreferencesHelper.getBoolean(PreferenceManager
        .getDefaultSharedPreferences
            (appContext), Constants.PREFERENCES_KEY_IS_MAIL_DEBUG_ENABLE, BuildConfig.IS_MAIL_DEBUG_ENABLE);
  }

  /**
   * Get a list of {@link KeyDetails} using the <b>Gmail API</b>
   *
   * @param context context Interface to global information about an application environment;
   * @param account An {@link AccountDao} object.
   * @param session A {@link Session} object.
   * @param js      An instance of {@link Js}
   * @return A list of {@link KeyDetails}
   * @throws MessagingException
   * @throws IOException
   */
  public static Collection<KeyDetails> getPrivateKeyBackupsViaGmailAPI(Context context, AccountDao account,
                                                                       Session session, Js js)
      throws IOException, MessagingException {
    ArrayList<KeyDetails> list = new ArrayList<>();
    String searchQuery = js.api_gmail_query_backups(account.getEmail());
    Gmail gmailApiService = GmailApiHelper.generateGmailApiService(context, account);

    ListMessagesResponse response = gmailApiService
        .users()
        .messages()
        .list(GmailApiHelper.DEFAULT_USER_ID)
        .setQ(searchQuery)
        .execute();

    List<com.google.api.services.gmail.model.Message> msgs = new ArrayList<>();

    //Try to load all backups
    while (response.getMessages() != null) {
      msgs.addAll(response.getMessages());
      if (response.getNextPageToken() != null) {
        String pageToken = response.getNextPageToken();
        response = gmailApiService
            .users()
            .messages()
            .list(GmailApiHelper.DEFAULT_USER_ID)
            .setQ(searchQuery)
            .setPageToken(pageToken)
            .execute();
      } else {
        break;
      }
    }

    for (com.google.api.services.gmail.model.Message origMsg : msgs) {
      com.google.api.services.gmail.model.Message message = gmailApiService
          .users()
          .messages()
          .get(GmailApiHelper.DEFAULT_USER_ID, origMsg.getId())
          .setFormat(GmailApiHelper.MESSAGE_RESPONSE_FORMAT_RAW)
          .execute();

      InputStream stream = new ByteArrayInputStream(Base64.decode(message.getRaw(), Base64.URL_SAFE));
      MimeMessage msg = new MimeMessage(session, stream);
      String backup = getKeyFromMimeMessage(msg);

      if (TextUtils.isEmpty(backup)) {
        continue;
      }

      MessageBlock[] blocks = js.crypto_armor_detect_blocks(backup);

      for (MessageBlock block : blocks) {
        if (MessageBlock.TYPE_PGP_PRIVATE_KEY.equalsIgnoreCase(block.getType())) {
          if (!TextUtils.isEmpty(block.getContent()) && !EmailUtil.isKeyExisted(list, block.getContent())) {
            list.add(new KeyDetails(block.getContent(), KeyDetails.Type.EMAIL));
          }
        }
      }
    }

    return list;
  }

  /**
   * Get a private key from {@link Message}, if it exists in.
   *
   * @param msg The original {@link Message} object.
   * @return <tt>String</tt> A private key.
   * @throws MessagingException
   * @throws IOException
   */
  public static String getKeyFromMimeMessage(Message msg) throws MessagingException, IOException {
    if (msg.isMimeType(JavaEmailConstants.MIME_TYPE_MULTIPART)) {
      Multipart multipart = (Multipart) msg.getContent();
      int partsCount = multipart.getCount();
      for (int partCount = 0; partCount < partsCount; partCount++) {
        BodyPart part = multipart.getBodyPart(partCount);
        if (part instanceof MimeBodyPart) {
          MimeBodyPart bodyPart = (MimeBodyPart) part;
          if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
            return IOUtils.toString(bodyPart.getInputStream(), StandardCharsets.UTF_8);
          }
        }
      }
    }

    return null;
  }

  /**
   * This method checks is the list contains a key.
   *
   * @param list The list of {@link KeyDetails} objects.
   * @param key  The private key armored string.
   * @return true if the key not exists in the list, otherwise false.
   */
  public static boolean isKeyExisted(ArrayList<KeyDetails> list, String key) {
    for (KeyDetails keyDetails : list) {
      if (key.equals(keyDetails.getValue())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Prepare the input HTML to show the user a viewport option.
   *
   * @return A generated HTML page which will be more comfortable for user.
   */
  @NonNull
  public static String genViewportHtml(String incomingHtml) {
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
  public static String genForwardedMessageDate(Date date) {
    if (date == null) {
      return "";
    }

    SimpleDateFormat format = new SimpleDateFormat(PATTERN_FORWARDED_DATE, Locale.US);
    return format.format(date);
  }

  /**
   * Generated a list of UID of the local messages which will be removed.
   *
   * @param localUIDs The list of UID of the local messages.
   * @param folder    The remote {@link IMAPFolder}.
   * @param msgs      The array of incoming messages.
   * @return A list of UID of the local messages which will be removed.
   */
  public static Collection<Long> genDeleteCandidates(Collection<Long> localUIDs,
                                                     IMAPFolder folder, javax.mail.Message[] msgs) {
    Collection<Long> uidListDeleteCandidates = new HashSet<>(localUIDs);
    Collection<Long> uidList = new HashSet<>();
    try {
      for (javax.mail.Message msg : msgs) {
        uidList.add(folder.getUID(msg));
      }
    } catch (MessagingException e) {
      e.printStackTrace();
      if (!(e instanceof MessageRemovedException)) {
        ExceptionUtil.handleError(e);
      }
    }

    uidListDeleteCandidates.removeAll(uidList);
    return uidListDeleteCandidates;
  }

  /**
   * Generate an array of {@link javax.mail.Message} which contains candidates for insert.
   *
   * @param localUIDs The list of UID of the local messages.
   * @param folder    The remote {@link IMAPFolder}.
   * @param msgs      The array of incoming messages.
   * @return The generated array.
   */
  public static javax.mail.Message[] genNewCandidates(Collection<Long> localUIDs,
                                                      IMAPFolder folder, javax.mail.Message[] msgs) {
    List<javax.mail.Message> newCandidates = new ArrayList<>();
    try {
      for (javax.mail.Message msg : msgs) {
        if (!localUIDs.contains(folder.getUID(msg))) {
          newCandidates.add(msg);
        }
      }
    } catch (MessagingException e) {
      e.printStackTrace();
      if (!(e instanceof MessageRemovedException)) {
        ExceptionUtil.handleError(e);
      }
    }
    return newCandidates.toArray(new javax.mail.Message[0]);
  }

  /**
   * Generate an array of the messages which will be updated.
   *
   * @param map    The map of UID and flags of the local messages.
   * @param folder The remote {@link IMAPFolder}.
   * @param msgs   The array of incoming messages.
   * @return An array of the messages which are candidates for updating iin the local database.
   */
  public static javax.mail.Message[] genUpdateCandidates(Map<Long, String> map, IMAPFolder folder,
                                                         javax.mail.Message[] msgs) {
    Collection<javax.mail.Message> updateCandidates = new ArrayList<>();
    try {
      for (javax.mail.Message msg : msgs) {
        String flags = map.get(folder.getUID(msg));
        if (flags == null) {
          flags = "";
        }

        if (!flags.equalsIgnoreCase(msg.getFlags().toString())) {
          updateCandidates.add(msg);
        }
      }
    } catch (MessagingException e) {
      e.printStackTrace();
      if (!(e instanceof MessageRemovedException)) {
        ExceptionUtil.handleError(e);
      }
    }
    return updateCandidates.toArray(new javax.mail.Message[0]);
  }

  /**
   * Get the personal name of the first address from an array. If the given name is null we will return the email
   * address.
   *
   * @param addresses An array of {@link InternetAddress}
   * @return The first address as a human readable string or email.
   */
  public static String getFirstAddressString(InternetAddress[] addresses) {
    if (addresses == null || addresses.length == 0) {
      return "";
    }

    if (TextUtils.isEmpty(addresses[0].getPersonal())) {
      return addresses[0].getAddress();
    } else {
      return addresses[0].getPersonal();
    }
  }

  /**
   * Get updated information about messages in the local database.
   *
   * @param folder          The folder which contains messages.
   * @param loadedMsgsCount The count of already loaded messages.
   * @param newMsgsCount    The count of new messages (offset value).
   * @return A list of messages which already exist in the local database.
   * @throws MessagingException for other failures.
   */
  public static Message[] getUpdatedMessages(IMAPFolder folder, int loadedMsgsCount, int newMsgsCount)
      throws MessagingException {
    int end = folder.getMessageCount() - newMsgsCount;
    int start = end - loadedMsgsCount + 1;

    if (end < 1) {
      return new Message[]{};
    } else {
      if (start < 1) {
        start = 1;
      }

      Message[] msgs = folder.getMessages(start, end);

      if (msgs.length > 0) {
        FetchProfile fetchProfile = new FetchProfile();
        fetchProfile.add(FetchProfile.Item.FLAGS);
        fetchProfile.add(UIDFolder.FetchProfileItem.UID);
        folder.fetch(msgs, fetchProfile);
      }
      return msgs;
    }
  }

  /**
   * Get updated information about messages in the local database using UIDs.
   *
   * @param folder The folder which contains messages.
   * @param first  The first UID in a range.
   * @param end    The last UID in a range.
   * @return A list of messages which already exist in the local database.
   * @throws MessagingException for other failures.
   */
  public static Message[] getUpdatedMessagesByUID(IMAPFolder folder, long first, long end)
      throws MessagingException {
    if (end <= first) {
      return new Message[]{};
    } else {
      Message[] msgs = folder.getMessagesByUID(first, end);

      if (msgs.length > 0) {
        FetchProfile fetchProfile = new FetchProfile();
        fetchProfile.add(FetchProfile.Item.FLAGS);
        fetchProfile.add(UIDFolder.FetchProfileItem.UID);
        folder.fetch(msgs, fetchProfile);
      }
      return msgs;
    }
  }

  /**
   * Load messages info.
   *
   * @param folder The folder which contains messages.
   * @param msgs   The array of {@link Message}.
   * @return New messages from a server which not exist in a local database.
   * @throws MessagingException for other failures.
   */
  public static Message[] fetchMessagesInfo(IMAPFolder folder, Message[] msgs) throws MessagingException {
    if (msgs.length > 0) {
      FetchProfile fetchProfile = new FetchProfile();
      fetchProfile.add(FetchProfile.Item.ENVELOPE);
      fetchProfile.add(FetchProfile.Item.FLAGS);
      fetchProfile.add(FetchProfile.Item.CONTENT_INFO);
      fetchProfile.add(UIDFolder.FetchProfileItem.UID);

      folder.fetch(msgs, fetchProfile);
    }

    return msgs;
  }

  /**
   * Check is input messages are encrypted.
   *
   * @param folder  The folder which contains messages which will be checked.
   * @param uidList The array of messages {@link UID} values.
   * @return {@link SparseArray} as results of the checking.
   */
  @SuppressWarnings("unchecked")
  @NonNull
  public static LongSparseArray<Boolean> getMessagesEncryptionState(IMAPFolder folder, List<Long> uidList)
      throws MessagingException {
    if (CollectionUtils.isEmpty(uidList)) {
      return new LongSparseArray<>();
    }
    long[] uidArray = new long[uidList.size()];

    for (int i = 0; i < uidList.size(); i++) {
      uidArray[i] = uidList.get(i);
    }

    final UIDSet[] uidSets = UIDSet.createUIDSets(uidArray);

    if (uidSets == null || uidSets.length == 0) {
      return new LongSparseArray<>();
    }

    return (LongSparseArray<Boolean>) folder.doCommand(new IMAPFolder.ProtocolCommand() {
      public Object doCommand(IMAPProtocol imapProtocol) throws ProtocolException {
        LongSparseArray<Boolean> booleanLongSparseArray = new LongSparseArray<>();

        Argument args = new Argument();
        Argument list = new Argument();
        list.writeString("UID");
        list.writeString("BODY.PEEK[TEXT]<0.2048>");
        args.writeArgument(list);

        Response[] responses = imapProtocol.command("UID FETCH " + UIDSet.toString(uidSets), args);
        Response serverResponse = responses[responses.length - 1];

        if (serverResponse.isOK()) {
          for (Response response : responses) {
            if (!(response instanceof FetchResponse)) {
              continue;
            }

            FetchResponse fetchResponse = (FetchResponse) response;

            UID uid = fetchResponse.getItem(UID.class);
            if (uid != null && uid.uid != 0) {
              BODY body = fetchResponse.getItem(BODY.class);
              if (body != null && body.getByteArrayInputStream() != null) {
                String rawMessage = ASCIIUtility.toString(body.getByteArrayInputStream());
                booleanLongSparseArray.put(uid.uid, rawMessage.contains("-----BEGIN PGP MESSAGE-----"));
              }
            }
          }
        }

        imapProtocol.notifyResponseHandlers(responses);
        imapProtocol.handleResult(serverResponse);

        return booleanLongSparseArray;
      }
    });
  }

  /**
   * Generate a {@link SearchTerm} for encrypted messages which depends on an input {@link AccountDao}.
   *
   * @param account An input {@link AccountDao}
   * @return A generated {@link SearchTerm}.
   */
  @NonNull
  public static SearchTerm genEncryptedMessagesSearchTerm(AccountDao account) {
    if (AccountDao.ACCOUNT_TYPE_GOOGLE.equalsIgnoreCase(account.getAccountType())) {
      return new GmailRawSearchTerm(
          "PGP OR GPG OR OpenPGP OR filename:asc OR filename:message OR filename:pgp OR filename:gpg");
    } else {
      return new BodyTerm("-----BEGIN PGP MESSAGE-----");
    }
  }

  /**
   * Generate a raw MIME message using {@link Js} tools.
   *
   * @param info    The given {@link OutgoingMessageInfo} which contains information about an outgoing
   *                message.
   * @param js      An instance of {@link Js}
   * @param pubKeys The public keys which will be used to generate an encrypted part.
   * @return The generated raw MIME message.
   */
  public static String genRawMessageWithoutAttachments(OutgoingMessageInfo info, Js js, String[] pubKeys) {
    String msgText = null;

    switch (info.getEncryptionType()) {
      case ENCRYPTED:
        msgText = js.crypto_message_encrypt(pubKeys, info.getMessage());
        break;

      case STANDARD:
        msgText = info.getMessage();
        break;
    }

    return js.mime_encode(msgText,
        info.getToPgpContacts(),
        info.getCcPgpContacts(),
        info.getBccPgpContacts(),
        info.getFromPgpContact(),
        info.getSubject(),
        null,
        js.mime_decode(info.getRawReplyMessage()));
  }

  /**
   * Generate a list of the all recipients.
   *
   * @return A list of the all recipients
   */
  public static PgpContact[] getAllRecipients(OutgoingMessageInfo info) {
    List<PgpContact> pgpContacts = new ArrayList<>();

    if (info.getToPgpContacts() != null) {
      pgpContacts.addAll(Arrays.asList(info.getToPgpContacts()));
    }

    if (info.getCcPgpContacts() != null) {
      pgpContacts.addAll(Arrays.asList(info.getCcPgpContacts()));
    }

    if (info.getBccPgpContacts() != null) {
      pgpContacts.addAll(Arrays.asList(info.getBccPgpContacts()));
    }

    return pgpContacts.toArray(new PgpContact[0]);
  }

  /**
   * Generate a list of the all recipients.
   *
   * @return A list of the all recipients
   */
  public static PgpContact[] getAllRecipients(Context context, GeneralMessageDetails details) {
    List<String> emails = new ArrayList<>();

    if (details.getTo() != null) {
      for (InternetAddress internetAddress : details.getTo()) {
        emails.add(internetAddress.getAddress());
      }
    }

    if (details.getCc() != null) {
      for (InternetAddress internetAddress : details.getCc()) {
        emails.add(internetAddress.getAddress());
      }
    }

    //todo-denbond7 need to add support BCC

    return new ContactsDaoSource().getPgpContacts(context, emails).toArray(new PgpContact[0]);
  }

  /**
   * Get next {@link UID} value for the outgoing message.
   *
   * @param context Interface to global information about an application environment.
   * @return The next {@link UID} value for the outgoing message.
   */
  public static long genOutboxUID(Context context) {
    long lastUid = SharedPreferencesHelper.getLong(PreferenceManager.getDefaultSharedPreferences(context),
        Constants.PREFERENCES_KEY_LAST_OUTBOX_UID, 0);

    lastUid++;

    SharedPreferencesHelper.setLong(PreferenceManager.getDefaultSharedPreferences(context),
        Constants.PREFERENCES_KEY_LAST_OUTBOX_UID, lastUid);

    return lastUid;
  }

  /**
   * Get information about the encryption state for the given messages.
   *
   * @param isEncryptedModeEnabled If true we show only encrypted messages
   * @param folder                 The folder which contains input messages
   * @param newMsgs                The input messages
   * @return An array which contains information about the encryption state of the given messages.
   * @throws MessagingException
   */
  public static LongSparseArray<Boolean> getMessagesEncryptionInfo(boolean isEncryptedModeEnabled, IMAPFolder folder,
                                                                   Message[] newMsgs) throws MessagingException {
    LongSparseArray<Boolean> array = new LongSparseArray<>();
    if (isEncryptedModeEnabled) {
      for (Message msg : newMsgs) {
        array.put(folder.getUID(msg), true);
      }
    } else {
      List<Long> uidList = new ArrayList<>();

      for (Message msg : newMsgs) {
        uidList.add(folder.getUID(msg));
      }

      array = EmailUtil.getMessagesEncryptionState(folder, uidList);
    }
    return array;
  }

  @NonNull
  private static BodyPart getBodyPartWithBackupText(Context context) throws MessagingException, IOException {
    BodyPart messageBodyPart = new MimeBodyPart();
    messageBodyPart.setContent(GeneralUtil.removeAllComments(IOUtils.toString(context.getAssets()
        .open(HTML_EMAIL_INTRO_TEMPLATE_HTM), StandardCharsets.UTF_8)), JavaEmailConstants.MIME_TYPE_TEXT_HTML);
    return messageBodyPart;
  }

  @NonNull
  private static Message generateMessageWithBackupTemplate(Context context, AccountDao account, Session session)
      throws MessagingException {
    Message msg = new MimeMessage(session);

    msg.setFrom(new InternetAddress(account.getEmail()));
    msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(account.getEmail()));
    msg.setSubject(context.getString(R.string.your_key_backup));
    return msg;
  }
}
