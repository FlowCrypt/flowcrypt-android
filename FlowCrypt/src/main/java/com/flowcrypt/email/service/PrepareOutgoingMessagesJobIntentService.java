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
  private Session session;
  private AccountDao account;
  private File attsCacheDir;

  /**
   * Enqueue a new task for {@link PrepareOutgoingMessagesJobIntentService}.
   *
   * @param context             Interface to global information about an application environment.
   * @param outgoingMessageInfo {@link OutgoingMessageInfo} which contains information about an outgoing message.
   */
  public static void enqueueWork(Context context, OutgoingMessageInfo outgoingMessageInfo) {
    if (outgoingMessageInfo != null) {
      Intent intent = new Intent(context, PrepareOutgoingMessagesJobIntentService.class);
      intent.putExtra(EXTRA_KEY_OUTGOING_MESSAGE_INFO, outgoingMessageInfo);

      enqueueWork(context, PrepareOutgoingMessagesJobIntentService.class, JobIdManager
          .JOB_TYPE_PREPARE_OUT_GOING_MESSAGE, intent);
    }
  }

  @Override
  public void onCreate() {
    super.onCreate();
    Log.d(TAG, "onCreate");
    msgDaoSource = new MessageDaoSource();
    account = new AccountDaoSource().getActiveAccountInformation(getApplicationContext());
    session = OpenStoreHelper.getSessionForAccountDao(getApplicationContext(), account);
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

      if (msgDaoSource.getMessage(getApplicationContext(),
          account.getEmail(), JavaEmailConstants.FOLDER_OUTBOX, uid) != null) {
        //todo-DenBond7 need to think about resolving a situation, when a message was created but the
        // attachments were not added.
        return;
      }

      Log.d(TAG, "Received a new job: " + outgoingMsgInfo);
      Uri newMessageUri = null;

      try {
        setupIfNeed();
        updateContactsLastUseDateTime(outgoingMsgInfo);

        String[] pubKeys = outgoingMsgInfo.getEncryptionType() == MessageEncryptionType.ENCRYPTED ?
            SecurityUtils.getRecipientsPubKeys(getApplicationContext(), js, EmailUtil.getAllRecipients
                (outgoingMsgInfo), account, outgoingMsgInfo.getFromPgpContact().getEmail()) : null;

        String rawMsg = EmailUtil.genRawMessageWithoutAttachments(outgoingMsgInfo, js, pubKeys);
        MimeMessage mimeMessage = new MimeMessage(session,
            IOUtils.toInputStream(rawMsg, StandardCharsets.UTF_8));

        File msgAttsCacheDir = new File(attsCacheDir, UUID.randomUUID().toString());

        ContentValues contentValues = prepareContentValues(outgoingMsgInfo, uid, mimeMessage,
            rawMsg, msgAttsCacheDir);

        newMessageUri = msgDaoSource.addRow(getApplicationContext(), contentValues);

        if (newMessageUri != null) {
          new ImapLabelsDaoSource().updateLabelMessagesCount(getApplicationContext(), account.getEmail(),
              JavaEmailConstants.FOLDER_OUTBOX, msgDaoSource.getOutboxMessages(getApplicationContext(),
                  account.getEmail()).size());

          if (!CollectionUtils.isEmpty(outgoingMsgInfo.getAttachments())
              || !CollectionUtils.isEmpty(outgoingMsgInfo.getForwardedAttachments())) {
            if (!msgAttsCacheDir.exists()) {
              if (!msgAttsCacheDir.mkdir()) {
                Log.e(TAG, "Create cache directory " + attsCacheDir.getName() + " filed!");
                msgDaoSource.updateMessageState(getApplicationContext(), account.getEmail(),
                    JavaEmailConstants.FOLDER_OUTBOX, uid, MessageState.ERROR_CACHE_PROBLEM);
                return;
              }
            }

            addAttachmentsToCache(outgoingMsgInfo, uid, pubKeys, msgAttsCacheDir);
          }

          if (CollectionUtils.isEmpty(outgoingMsgInfo.getForwardedAttachments())) {
            msgDaoSource.updateMessageState(getApplicationContext(), account.getEmail(),
                JavaEmailConstants.FOLDER_OUTBOX, uid, MessageState.QUEUED);
            MessagesSenderJobService.schedule(getApplicationContext());
          } else {
            ForwardedAttachmentsDownloaderJobService.schedule(getApplicationContext());
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        ExceptionUtil.handleError(e);

        if (newMessageUri == null) {
          ContentValues contentValues = MessageDaoSource.prepareContentValues(account.getEmail(),
              JavaEmailConstants.FOLDER_OUTBOX, uid, outgoingMsgInfo);

          newMessageUri = msgDaoSource.addRow(getApplicationContext(), contentValues);
        }

        if (e instanceof NoKeyAvailableException) {
          NoKeyAvailableException exception = (NoKeyAvailableException) e;
          String errorMsg = TextUtils.isEmpty(exception.getAlias()) ? exception.getEmail() : exception.getAlias();

          ContentValues contentValues = new ContentValues();
          contentValues.put(MessageDaoSource.COL_STATE, MessageState.ERROR_PRIVATE_KEY_NOT_FOUND.getValue());
          contentValues.put(MessageDaoSource.COL_ERROR_MSG, errorMsg);

          msgDaoSource.updateMessage(getApplicationContext(), account.getEmail(),
              JavaEmailConstants.FOLDER_OUTBOX, uid, contentValues);
        } else {
          msgDaoSource.updateMessageState(getApplicationContext(), account.getEmail(),
              JavaEmailConstants.FOLDER_OUTBOX, uid, MessageState.ERROR_DURING_CREATION);
        }
      }

      if (newMessageUri != null) {
        new ImapLabelsDaoSource().updateLabelMessagesCount(this, account.getEmail(),
            JavaEmailConstants.FOLDER_OUTBOX, msgDaoSource.getOutboxMessages(this, account.getEmail()).size());
      }
    }
  }

  @NonNull
  private ContentValues prepareContentValues(OutgoingMessageInfo outgoingMessageInfo,
                                             long generatedUID, MimeMessage mimeMessage,
                                             String rawMessage, File attachmentsCacheDirectory)
      throws MessagingException {
    ContentValues contentValues = MessageDaoSource.prepareContentValues(account.getEmail(),
        JavaEmailConstants.FOLDER_OUTBOX, mimeMessage, generatedUID, false);

    contentValues.put(MessageDaoSource.COL_RAW_MESSAGE_WITHOUT_ATTACHMENTS, rawMessage);
    contentValues.put(MessageDaoSource.COL_FLAGS, MessageFlag.SEEN);
    contentValues.put(MessageDaoSource.COL_IS_MESSAGE_HAS_ATTACHMENTS,
        !CollectionUtils.isEmpty(outgoingMessageInfo.getAttachments())
            || !CollectionUtils.isEmpty(outgoingMessageInfo.getForwardedAttachments()));
    contentValues.put(MessageDaoSource.COL_IS_ENCRYPTED,
        outgoingMessageInfo.getEncryptionType() == MessageEncryptionType.ENCRYPTED);
    contentValues.put(MessageDaoSource.COL_STATE, outgoingMessageInfo.isForwarded()
        ? MessageState.NEW_FORWARDED.getValue() : MessageState.NEW.getValue());
    contentValues.put(MessageDaoSource.COL_ATTACHMENTS_DIRECTORY, attachmentsCacheDirectory.getName());

    return contentValues;
  }

  private void addAttachmentsToCache(OutgoingMessageInfo outgoingMessageInfo, long generatedUID, String[] pubKeys,
                                     File messageAttachmentCacheDirectory) {
    AttachmentDaoSource attachmentDaoSource = new AttachmentDaoSource();
    List<AttachmentInfo> cachedAttachments = new ArrayList<>();

    if (!CollectionUtils.isEmpty(outgoingMessageInfo.getAttachments())) {
      if (outgoingMessageInfo.getEncryptionType() == MessageEncryptionType.ENCRYPTED) {
        for (AttachmentInfo attachmentInfo : outgoingMessageInfo.getAttachments()) {
          try {
            Uri uriOfOriginalFile = attachmentInfo.getUri();

            InputStream inputStream = getContentResolver().openInputStream(uriOfOriginalFile);
            if (inputStream != null) {
              File encryptedTempFile = new File(messageAttachmentCacheDirectory,
                  attachmentInfo.getName() + Constants.PGP_FILE_EXT);
              byte[] encryptedBytes = js.crypto_message_encrypt(pubKeys, IOUtils.toByteArray
                  (inputStream), attachmentInfo.getName());
              FileUtils.writeByteArrayToFile(encryptedTempFile, encryptedBytes);
              attachmentInfo.setUri(FileProvider.getUriForFile(getApplicationContext(),
                  Constants.FILE_PROVIDER_AUTHORITY, encryptedTempFile));
              attachmentInfo.setName(encryptedTempFile.getName());
              cachedAttachments.add(attachmentInfo);

              if (Constants.FILE_PROVIDER_AUTHORITY.equalsIgnoreCase(uriOfOriginalFile.getAuthority())) {
                getContentResolver().delete(uriOfOriginalFile, null, null);
              }
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      } else {
        for (AttachmentInfo attachmentInfo : outgoingMessageInfo.getAttachments()) {
          try {
            Uri uriOfOriginalFile = attachmentInfo.getUri();

            InputStream inputStream = getContentResolver().openInputStream(uriOfOriginalFile);
            if (inputStream != null) {
              File cachedAttachment = new File(messageAttachmentCacheDirectory, attachmentInfo.getName());
              FileUtils.copyInputStreamToFile(inputStream, cachedAttachment);
              attachmentInfo.setUri(FileProvider.getUriForFile(getApplicationContext(),
                  Constants.FILE_PROVIDER_AUTHORITY, cachedAttachment));
              cachedAttachments.add(attachmentInfo);

              if (Constants.FILE_PROVIDER_AUTHORITY.equalsIgnoreCase(uriOfOriginalFile.getAuthority())) {
                getContentResolver().delete(uriOfOriginalFile, null, null);
              }
            }
          } catch (IOException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
          }
        }
      }
    }

    if (!CollectionUtils.isEmpty(outgoingMessageInfo.getForwardedAttachments())) {
      if (outgoingMessageInfo.getEncryptionType() == MessageEncryptionType.ENCRYPTED) {
        for (AttachmentInfo attachmentInfo : outgoingMessageInfo.getForwardedAttachments()) {
          AttachmentInfo attachmentInfoEncrypted =
              new AttachmentInfo(JavaEmailConstants.FOLDER_OUTBOX, attachmentInfo);
          attachmentInfoEncrypted.setName(attachmentInfoEncrypted.getName() + Constants.PGP_FILE_EXT);
          cachedAttachments.add(attachmentInfoEncrypted);
        }
      } else {
        for (AttachmentInfo attachmentInfo : outgoingMessageInfo.getForwardedAttachments()) {
          cachedAttachments.add(new AttachmentInfo(JavaEmailConstants.FOLDER_OUTBOX, attachmentInfo));
        }
      }
    }

    attachmentDaoSource.addRows(getApplicationContext(), account.getEmail(), JavaEmailConstants.FOLDER_OUTBOX,
        generatedUID, cachedAttachments);
  }

  private void setupIfNeed() {
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
   * @param outgoingMessageInfo - {@link OutgoingMessageInfo} which contains information about an outgoing message.
   */
  private void updateContactsLastUseDateTime(OutgoingMessageInfo outgoingMessageInfo) {
    ContactsDaoSource contactsDaoSource = new ContactsDaoSource();

    for (PgpContact pgpContact : EmailUtil.getAllRecipients(outgoingMessageInfo)) {
      int updateResult = contactsDaoSource.updateLastUseOfPgpContact(getApplicationContext(), pgpContact);
      if (updateResult == -1) {
        contactsDaoSource.addRow(getApplicationContext(), pgpContact);
      }
    }
  }
}
