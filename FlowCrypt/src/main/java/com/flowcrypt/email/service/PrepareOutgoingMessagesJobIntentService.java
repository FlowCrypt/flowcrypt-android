/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.flowcrypt.email.Constants;
import com.flowcrypt.email.api.email.EmailUtil;
import com.flowcrypt.email.api.email.JavaEmailConstants;
import com.flowcrypt.email.api.email.model.AttachmentInfo;
import com.flowcrypt.email.api.email.model.MessageFlag;
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo;
import com.flowcrypt.email.api.email.protocol.OpenStoreHelper;
import com.flowcrypt.email.database.MessageState;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.flowcrypt.email.database.dao.source.AccountDaoSource;
import com.flowcrypt.email.database.dao.source.ContactsDaoSource;
import com.flowcrypt.email.database.dao.source.imap.AttachmentDaoSource;
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource;
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource;
import com.flowcrypt.email.jobscheduler.ForwardedAttachmentsDownloaderJobService;
import com.flowcrypt.email.jobscheduler.JobIdManager;
import com.flowcrypt.email.jobscheduler.MessagesSenderJobService;
import com.flowcrypt.email.js.PgpContact;
import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.model.MessageEncryptionType;
import com.flowcrypt.email.security.SecurityStorageConnector;
import com.flowcrypt.email.security.SecurityUtils;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.flowcrypt.email.util.exception.NoKeyAvailableException;
import com.google.android.gms.common.util.CollectionUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.content.FileProvider;

/**
 * This service creates a new outgoing message using the given {@link OutgoingMessageInfo}.
 *
 * @author DenBond7
 * Date: 22.05.2017
 * Time: 22:25
 * E-mail: DenBond7@gmail.com
 */

public class PrepareOutgoingMessagesJobIntentService extends JobIntentService {
  private static final String EXTRA_KEY_OUTGOING_MESSAGE_INFO = GeneralUtil.generateUniqueExtraKey
      ("EXTRA_KEY_OUTGOING_MESSAGE_INFO", PrepareOutgoingMessagesJobIntentService.class);
  private static final String TAG = PrepareOutgoingMessagesJobIntentService.class.getSimpleName();

  private MessageDaoSource msgDaoSource;
  private Js js;
  private Session sess;
  private AccountDao account;
  private File attsCacheDir;

  /**
   * Enqueue a new task for {@link PrepareOutgoingMessagesJobIntentService}.
   *
   * @param context         Interface to global information about an application environment.
   * @param outgoingMsgInfo {@link OutgoingMessageInfo} which contains information about an outgoing message.
   */
  public static void enqueueWork(Context context, OutgoingMessageInfo outgoingMsgInfo) {
    if (outgoingMsgInfo != null) {
      Intent intent = new Intent(context, PrepareOutgoingMessagesJobIntentService.class);
      intent.putExtra(EXTRA_KEY_OUTGOING_MESSAGE_INFO, outgoingMsgInfo);

      enqueueWork(context, PrepareOutgoingMessagesJobIntentService.class,
          JobIdManager.JOB_TYPE_PREPARE_OUT_GOING_MESSAGE, intent);
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "onCreate");
    msgDaoSource = new MessageDaoSource();
    account = new AccountDaoSource().getActiveAccountInformation(getApplicationContext());
    sess = OpenStoreHelper.getAccountSess(getApplicationContext(), account);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(TAG, "onDestroy");
  }

  @Override
  public boolean onStopCurrentWork() {
    Log.d(TAG, "onStopCurrentWork");
    return super.onStopCurrentWork();
  }

  @Override
  protected void onHandleWork(@NonNull Intent intent) {
    Log.d(TAG, "onHandleWork");
    if (intent.hasExtra(EXTRA_KEY_OUTGOING_MESSAGE_INFO)) {
      OutgoingMessageInfo outgoingMsgInfo = intent.getParcelableExtra(EXTRA_KEY_OUTGOING_MESSAGE_INFO);
      long uid = outgoingMsgInfo.getUid();
      String email = account.getEmail();
      String label = JavaEmailConstants.FOLDER_OUTBOX;

      if (msgDaoSource.getMsg(this, email, label, uid) != null) {
        //todo-DenBond7 need to think about resolving a situation, when a message was created but the
        // attachments were not added.
        return;
      }

      Log.d(TAG, "Received a new job: " + outgoingMsgInfo);
      Uri newMsgUri = null;

      try {
        setupIfNeeded();
        updateContactsLastUseDateTime(outgoingMsgInfo);

        String[] pubKeys = null;
        if (outgoingMsgInfo.getEncryptionType() == MessageEncryptionType.ENCRYPTED) {
          PgpContact[] pgpContacts = EmailUtil.getAllRecipients(outgoingMsgInfo);
          String senderEmail = outgoingMsgInfo.getFromPgpContact().getEmail();
          pubKeys = SecurityUtils.getRecipientsPubKeys(this, js, pgpContacts, account, senderEmail);
        }

        String rawMsg = EmailUtil.genRawMsgWithoutAtts(outgoingMsgInfo, js, pubKeys);
        MimeMessage mimeMsg = new MimeMessage(sess, IOUtils.toInputStream(rawMsg, StandardCharsets.UTF_8));

        File msgAttsCacheDir = new File(attsCacheDir, UUID.randomUUID().toString());

        ContentValues contentValues = prepareContentValues(outgoingMsgInfo, uid, mimeMsg, rawMsg, msgAttsCacheDir);
        newMsgUri = msgDaoSource.addRow(this, contentValues);

        if (newMsgUri != null) {
          int msgsCount = msgDaoSource.getOutboxMsgs(this, email).size();
          new ImapLabelsDaoSource().updateLabelMsgsCount(this, email, label, msgsCount);

          boolean hasAtts = !CollectionUtils.isEmpty(outgoingMsgInfo.getAtts())
              || !CollectionUtils.isEmpty(outgoingMsgInfo.getForwardedAtts());

          if (hasAtts) {
            if (!msgAttsCacheDir.exists()) {
              if (!msgAttsCacheDir.mkdir()) {
                Log.e(TAG, "Create cache directory " + attsCacheDir.getName() + " filed!");
                msgDaoSource.updateMsgState(this, email, label, uid, MessageState.ERROR_CACHE_PROBLEM);
                return;
              }
            }

            addAttsToCache(outgoingMsgInfo, uid, pubKeys, msgAttsCacheDir);
          }

          if (CollectionUtils.isEmpty(outgoingMsgInfo.getForwardedAtts())) {
            msgDaoSource.updateMsgState(this, email, label, uid, MessageState.QUEUED);
            MessagesSenderJobService.schedule(getApplicationContext());
          } else {
            ForwardedAttachmentsDownloaderJobService.schedule(getApplicationContext());
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        ExceptionUtil.handleError(e);

        if (newMsgUri == null) {
          ContentValues contentValues = MessageDaoSource.prepareContentValues(email, label, uid, outgoingMsgInfo);
          newMsgUri = msgDaoSource.addRow(this, contentValues);
        }

        if (e instanceof NoKeyAvailableException) {
          NoKeyAvailableException exception = (NoKeyAvailableException) e;
          String errorMsg = TextUtils.isEmpty(exception.getAlias()) ? exception.getEmail() : exception.getAlias();

          ContentValues contentValues = new ContentValues();
          contentValues.put(MessageDaoSource.COL_STATE, MessageState.ERROR_PRIVATE_KEY_NOT_FOUND.getValue());
          contentValues.put(MessageDaoSource.COL_ERROR_MSG, errorMsg);

          msgDaoSource.updateMsg(this, email, label, uid, contentValues);
        } else {
          msgDaoSource.updateMsgState(this, email, label, uid, MessageState.ERROR_DURING_CREATION);
        }
      }

      if (newMsgUri != null) {
        int newMsgsCount = msgDaoSource.getOutboxMsgs(this, email).size();
        new ImapLabelsDaoSource().updateLabelMsgsCount(this, email, label, newMsgsCount);
      }
    }
  }

  @NonNull
  private ContentValues prepareContentValues(OutgoingMessageInfo msgInfo, long generatedUID, MimeMessage mimeMsg,
                                             String rawMsg, File attsCacheDir)
      throws MessagingException {
    ContentValues contentValues = MessageDaoSource.prepareContentValues(account.getEmail(),
        JavaEmailConstants.FOLDER_OUTBOX, mimeMsg, generatedUID, false);
    boolean hasAtts = !CollectionUtils.isEmpty(msgInfo.getAtts())
        || !CollectionUtils.isEmpty(msgInfo.getForwardedAtts());
    boolean isEncrypted = msgInfo.getEncryptionType() == MessageEncryptionType.ENCRYPTED;
    int msgStateValue = msgInfo.isForwarded() ? MessageState.NEW_FORWARDED.getValue() : MessageState.NEW.getValue();

    contentValues.put(MessageDaoSource.COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS, rawMsg);
    contentValues.put(MessageDaoSource.COL_FLAGS, MessageFlag.SEEN);
    contentValues.put(MessageDaoSource.COL_IS_MESSAGE_HAS_ATTACHMENTS, hasAtts);
    contentValues.put(MessageDaoSource.COL_IS_ENCRYPTED, isEncrypted);
    contentValues.put(MessageDaoSource.COL_STATE, msgStateValue);
    contentValues.put(MessageDaoSource.COL_ATTACHMENTS_DIRECTORY, attsCacheDir.getName());

    return contentValues;
  }

  private void addAttsToCache(OutgoingMessageInfo msgInfo, long uid, String[] pubKeys, File attsCacheDir) {
    AttachmentDaoSource attDaoSource = new AttachmentDaoSource();
    List<AttachmentInfo> cachedAtts = new ArrayList<>();

    if (!CollectionUtils.isEmpty(msgInfo.getAtts())) {
      for (AttachmentInfo att : msgInfo.getAtts()) {
        try {
          Uri origFileUri = att.getUri();
          InputStream inputStream = null;
          if (origFileUri != null) {
            inputStream = getContentResolver().openInputStream(origFileUri);
          } else if (!TextUtils.isEmpty(att.getRawData())) {
            inputStream = new ByteArrayInputStream(att.getRawData().getBytes());
          }

          if (inputStream == null) {
            continue;
          }

          if (msgInfo.getEncryptionType() == MessageEncryptionType.ENCRYPTED) {
            File encryptedTempFile = new File(attsCacheDir, att.getName() + Constants.PGP_FILE_EXT);
            byte[] originalBytes = IOUtils.toByteArray(inputStream);
            byte[] encryptedBytes = js.crypto_message_encrypt(pubKeys, originalBytes, att.getName());
            FileUtils.writeByteArrayToFile(encryptedTempFile, encryptedBytes);
            Uri uri = FileProvider.getUriForFile(this, Constants.FILE_PROVIDER_AUTHORITY, encryptedTempFile);
            att.setUri(uri);
            att.setName(encryptedTempFile.getName());
          } else {
            File cachedAtt = new File(attsCacheDir, att.getName());
            FileUtils.copyInputStreamToFile(inputStream, cachedAtt);
            Uri uri = FileProvider.getUriForFile(this, Constants.FILE_PROVIDER_AUTHORITY, cachedAtt);
            att.setUri(uri);
          }

          cachedAtts.add(att);
          if (origFileUri != null) {
            if (Constants.FILE_PROVIDER_AUTHORITY.equalsIgnoreCase(origFileUri.getAuthority())) {
              getContentResolver().delete(origFileUri, null, null);
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
          ExceptionUtil.handleError(e);
        }
      }
    }

    if (!CollectionUtils.isEmpty(msgInfo.getForwardedAtts())) {
      for (AttachmentInfo att : msgInfo.getForwardedAtts()) {
        if (msgInfo.getEncryptionType() == MessageEncryptionType.ENCRYPTED) {
          AttachmentInfo encryptedAtt = new AttachmentInfo(JavaEmailConstants.FOLDER_OUTBOX, att);
          encryptedAtt.setName(encryptedAtt.getName() + Constants.PGP_FILE_EXT);
          cachedAtts.add(encryptedAtt);
        } else {
          cachedAtts.add(new AttachmentInfo(JavaEmailConstants.FOLDER_OUTBOX, att));
        }
      }
    }

    attDaoSource.addRows(this, account.getEmail(), JavaEmailConstants.FOLDER_OUTBOX, uid, cachedAtts);
  }

  private void setupIfNeeded() {
    if (js == null) {
      try {
        js = new Js(getApplicationContext(), new SecurityStorageConnector(getApplicationContext()));
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    if (attsCacheDir == null) {
      attsCacheDir = new File(getCacheDir(), Constants.ATTACHMENTS_CACHE_DIR);
      if (!attsCacheDir.exists()) {
        if (!attsCacheDir.mkdirs()) {
          throw new IllegalStateException("Create cache directory " + attsCacheDir.getName() + " filed!");
        }
      }
    }
  }

  /**
   * Update the {@link ContactsDaoSource#COL_LAST_USE} field in the {@link ContactsDaoSource#TABLE_NAME_CONTACTS}.
   *
   * @param msgInfo - {@link OutgoingMessageInfo} which contains information about an outgoing message.
   */
  private void updateContactsLastUseDateTime(OutgoingMessageInfo msgInfo) {
    ContactsDaoSource contactsDaoSource = new ContactsDaoSource();

    for (PgpContact pgpContact : EmailUtil.getAllRecipients(msgInfo)) {
      int updateResult = contactsDaoSource.updateLastUseOfPgpContact(this, pgpContact);
      if (updateResult == -1) {
        contactsDaoSource.addRow(this, pgpContact);
      }
    }
  }
}
