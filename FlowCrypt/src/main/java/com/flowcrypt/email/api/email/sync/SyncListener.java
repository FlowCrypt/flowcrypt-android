/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync;

import android.content.Context;
import android.util.LongSparseArray;

import com.flowcrypt.email.api.email.LocalFolder;
import com.flowcrypt.email.api.email.sync.tasks.CheckIsLoadedMessagesEncryptedSyncTask;
import com.flowcrypt.email.database.dao.source.AccountDao;
import com.sun.mail.imap.IMAPFolder;

import java.util.List;

import javax.mail.Folder;
import javax.mail.Message;

/**
 * This class can be used for communication with {@link EmailSyncManager}
 *
 * @author DenBond7
 * Date: 19.06.2017
 * Time: 13:35
 * E-mail: DenBond7@gmail.com
 */

public interface SyncListener {

  /**
   * Get the service context.
   *
   * @return The service context
   */
  Context getContext();

  /**
   * This method called when a message with a backup to the key owner was sent.
   *
   * @param account     The {@link AccountDao} object which contains information about an email account.
   * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
   * @param requestCode The unique request code for the reply to
   *                    {@link android.os.Messenger}.
   * @param isSent      true if the message was sent, false otherwise.
   */
  void onMsgWithBackupToKeyOwnerSent(AccountDao account, String ownerKey, int requestCode, boolean isSent);

  /**
   * This method called when the private keys found.
   *
   * @param account     The {@link AccountDao} object which contains information about an email account.
   * @param keys        The private keys list.
   * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
   * @param requestCode The unique request code for the reply to
   *                    {@link android.os.Messenger}.
   */
  void onPrivateKeysFound(AccountDao account, List<String> keys, String ownerKey, int requestCode);

  /**
   * This method called when a message was sent.
   *
   * @param account     The {@link AccountDao} object which contains information about an email account.
   * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
   * @param requestCode The unique request code for the reply to
   *                    {@link android.os.Messenger}.
   * @param isSent      true if the message was sent, false otherwise.
   */
  void onMsgSent(AccountDao account, String ownerKey, int requestCode, boolean isSent);

  /**
   * This method called when a new messages received from the some folder.
   *
   * @param account     The {@link AccountDao} object which contains information about an email account.
   * @param srcFolder   The source folder where the messages exist.
   * @param destFolder  The destination folder where the messages were moved.
   * @param msgs        The moved messages.
   * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
   * @param requestCode The unique request code for the reply to
   *                    {@link android.os.Messenger}.
   */
  void onMsgsMoved(AccountDao account, IMAPFolder srcFolder, IMAPFolder destFolder, Message[] msgs,
                   String ownerKey, int requestCode);

  /**
   * This method called when a new messages received from the some folder.
   *
   * @param account     The {@link AccountDao} object which contains information about an email account.
   * @param srcFolder   The source folder where the messages exist.
   * @param destFolder  The destination folder where the messages were moved.
   * @param msg         The moved message.
   * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
   * @param requestCode The unique request code for the reply to
   *                    {@link android.os.Messenger}.
   */
  void onMsgMoved(AccountDao account, IMAPFolder srcFolder, IMAPFolder destFolder,
                  Message msg, String ownerKey, int requestCode);

  /**
   * This method called when a new messages received from the some folder.
   *
   * @param account           The {@link AccountDao} object which contains information about an email
   *                          account;
   * @param localFolder       The local implementation of the remote folder;
   * @param remoteFolder      The folder where the new messages exist;
   * @param uid               The UID of the message;
   * @param msg               The received message;
   * @param rawMsgWithoutAtts The raw message without attachments;
   * @param ownerKey          The name of the reply to {@link android.os.Messenger};
   * @param requestCode       The unique request code for the reply to
   *                          {@link android.os.Messenger}.
   */
  void onMsgDetailsReceived(AccountDao account, LocalFolder localFolder, IMAPFolder remoteFolder, long uid,
                            Message msg, String rawMsgWithoutAtts, String ownerKey, int requestCode);

  /**
   * This method called when a new messages received from the some folder.
   *
   * @param account      The {@link AccountDao} object which contains information about an
   *                     email account.
   * @param localFolder  The local implementation of the remote folder
   * @param remoteFolder The remote folder where the new messages exist.
   * @param msgs         The new messages.
   * @param ownerKey     The name of the reply to {@link android.os.Messenger}.
   * @param requestCode  The unique request code for the reply to
   *                     {@link android.os.Messenger}.
   */
  void onMsgsReceived(AccountDao account, LocalFolder localFolder, IMAPFolder remoteFolder, Message[] msgs,
                      String ownerKey, int requestCode);

  /**
   * This method called when received information about new messages in some folder.
   *
   * @param account              The {@link AccountDao} object which contains information about an email account.
   * @param localFolder          The local implementation of the remote folder.
   * @param remoteFolder         The folder where the new  messages exist.
   * @param newMsgs              The new messages.
   * @param msgsEncryptionStates An array which contains info about a message encryption state
   * @param ownerKey             The name of the reply to {@link android.os.Messenger}.
   * @param requestCode          The unique request code for the reply to {@link android.os.Messenger}.
   */
  void onNewMsgsReceived(AccountDao account, LocalFolder localFolder, IMAPFolder remoteFolder, Message[] newMsgs,
                         LongSparseArray<Boolean> msgsEncryptionStates, String ownerKey, int requestCode);

  /**
   * This method called when a new messages received from the some folder.
   *
   * @param account      The {@link AccountDao} object which contains information about an
   *                     email account.
   * @param localFolder  The local implementation of the remote folder.
   * @param remoteFolder The folder where the messages exist.
   * @param msgs         The new messages.
   * @param ownerKey     The name of the reply to {@link android.os.Messenger}.
   * @param requestCode  The unique request code for the reply to
   *                     {@link android.os.Messenger}.
   */
  void onSearchMsgsReceived(AccountDao account, LocalFolder localFolder,
                            IMAPFolder remoteFolder, Message[] msgs, String ownerKey, int requestCode);

  /**
   * This method called when received information about messages which already exist in the local database.
   *
   * @param account      The {@link AccountDao} object which contains information about an email account.
   * @param localFolder  The local implementation of the remote folder.
   * @param remoteFolder The folder where the messages exist.
   * @param newMsgs      The refreshed messages.
   * @param updateMsgs   The messages which will must be updated.
   * @param ownerKey     The name of the reply to {@link android.os.Messenger}.
   * @param requestCode  The unique request code for the reply to {@link android.os.Messenger}.
   */
  void onRefreshMsgsReceived(AccountDao account, LocalFolder localFolder,
                             IMAPFolder remoteFolder, Message[] newMsgs, Message[] updateMsgs,
                             String ownerKey, int requestCode);

  /**
   * This method called when new folders list received.
   *
   * @param account     The {@link AccountDao} object which contains information about an email account.
   * @param folders     The new folders list.
   * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
   * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
   */
  void onFolderInfoReceived(AccountDao account, Folder[] folders, String ownerKey, int requestCode);

  /**
   * Handle an error of synchronization.
   *
   * @param account     The {@link AccountDao} object which contains information about an email account.
   * @param errorType   The error type code.
   * @param e           The exception that occurred during synchronization.
   * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
   * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
   */
  void onError(AccountDao account, int errorType, Exception e, String ownerKey, int requestCode);

  /**
   * This method can be used for debugging. Using this method we can identify a progress of some operation.
   *
   * @param account     The {@link AccountDao} object which contains information about an email account.
   * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
   * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
   * @param resultCode  The unique result code for the reply which identifies the progress of some request.
   */
  void onActionProgress(AccountDao account, String ownerKey, int requestCode, int resultCode);

  /**
   * This method will be called when some message was changed.
   *
   * @param account      The {@link AccountDao} object which contains information about an email account.
   * @param localFolder  The local implementation of the remote folder
   * @param remoteFolder The remote folder where the new messages exist.
   * @param msg          The message which was changed.
   * @param ownerKey     The name of the reply to {@link android.os.Messenger}.
   * @param requestCode  The unique request code for the reply to {@link android.os.Messenger}.
   */
  void onMsgChanged(AccountDao account, LocalFolder localFolder,
                    IMAPFolder remoteFolder, Message msg, String ownerKey, int requestCode);

  /**
   * This method called when {@link CheckIsLoadedMessagesEncryptedSyncTask} was completed.
   *
   * @param account      The {@link AccountDao} object which contains information about an email account.
   * @param localFolder  The local implementation of the remote folder
   * @param remoteFolder The remote folder where the new messages exist.
   * @param ownerKey     The name of the reply to {@link android.os.Messenger}.
   * @param requestCode  The unique request code for the reply to
   *                     {@link android.os.Messenger}.
   */
  void onIdentificationToEncryptionCompleted(AccountDao account, LocalFolder localFolder,
                                             IMAPFolder remoteFolder, String ownerKey, int requestCode);
}
