/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service

import android.content.Context
import android.content.Intent
import android.content.OperationApplicationException
import android.os.RemoteException
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.api.email.sync.EmailSyncManager
import com.flowcrypt.email.api.email.sync.SyncListener
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.jetpack.lifecycle.ConnectionLifecycleObserver
import com.flowcrypt.email.jetpack.workmanager.sync.CheckIsLoadedMessagesEncryptedWorker
import com.flowcrypt.email.model.EmailAndNamePair
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.LogsUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.sun.mail.imap.IMAPFolder
import javax.mail.Flags
import javax.mail.MessagingException
import javax.mail.internet.InternetAddress

/**
 * This the email synchronization service. This class is responsible for the logic of
 * synchronization work. Using this service we can asynchronous download information and send
 * data to the IMAP server.
 *
 * @author DenBond7
 * Date: 14.06.2017
 * Time: 12:18
 * E-mail: DenBond7@gmail.com
 */
class EmailSyncService : LifecycleService(), SyncListener {
  private lateinit var emailSyncManager: EmailSyncManager
  private lateinit var notificationManager: MessagesNotificationManager
  private lateinit var connectionLifecycleObserver: ConnectionLifecycleObserver

  override val context: Context
    get() = this.applicationContext

  override fun onCreate() {
    super.onCreate()
    LogsUtil.d(TAG, "onCreate")
    setupConnectionObserver()

    notificationManager = MessagesNotificationManager(this)
    emailSyncManager = EmailSyncManager(this)
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    LogsUtil.d(TAG, "onStartCommand |intent =$intent|flags = $flags|startId = $startId")

    if (intent != null && intent.action != null) {
      when (intent.action) {
        else -> if (::emailSyncManager.isInitialized) {
          emailSyncManager.beginSync()
        }
      }
    } else if (::emailSyncManager.isInitialized) {
      emailSyncManager.beginSync()
    }

    return super.onStartCommand(intent, flags, startId)
  }

  override fun onDestroy() {
    super.onDestroy()
    LogsUtil.d(TAG, "onDestroy")

    if (::emailSyncManager.isInitialized) {
      emailSyncManager.stopSync()
    }

    lifecycle.removeObserver(connectionLifecycleObserver)
  }

  override fun onNewMsgsReceived(account: AccountEntity, localFolder: LocalFolder,
                                 remoteFolder: IMAPFolder, newMsgs: Array<javax.mail.Message>,
                                 msgsEncryptionStates: Map<Long, Boolean>, ownerKey: String,
                                 requestCode: Int) {
    LogsUtil.d(TAG, "onMessagesReceived:message count: " + newMsgs.size)
    try {
      val email = account.email
      val folderName = localFolder.fullName

      val folderType = FoldersManager.getFolderType(localFolder)
      val isNew = !GeneralUtil.isAppForegrounded() && folderType === FoldersManager.FolderType.INBOX

      val msgEntities = MessageEntity.genMessageEntities(
          context = this,
          email = email,
          label = folderName,
          folder = remoteFolder,
          msgs = newMsgs,
          msgsEncryptionStates = msgsEncryptionStates,
          isNew = isNew,
          areAllMsgsEncrypted = false
      )

      val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
      roomDatabase.msgDao().insertWithReplace(msgEntities)

      if (!GeneralUtil.isAppForegrounded()) {
        val detailsList = roomDatabase.msgDao().getNewMsgs(email, folderName)
        notificationManager.notify(this, account, localFolder, detailsList)
      }
    } catch (e: MessagingException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    } catch (e: RemoteException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  override fun onRefreshMsgsReceived(account: AccountEntity, localFolder: LocalFolder,
                                     remoteFolder: IMAPFolder, newMsgs: Array<javax.mail.Message>,
                                     updateMsgs: Array<javax.mail.Message>, ownerKey: String, requestCode: Int) {
    LogsUtil.d(TAG, "onRefreshMessagesReceived: imapFolder = " + remoteFolder.fullName + " newMessages " +
        "count: " + newMsgs.size + ", updateMessages count = " + updateMsgs.size)
    val email = account.email
    val folderName = localFolder.fullName

    try {
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

      val mapOfUIDAndMsgFlags = roomDatabase.msgDao().getMapOfUIDAndMsgFlags(email, folderName)
      val msgsUIDs = HashSet(mapOfUIDAndMsgFlags.keys)
      val deleteCandidatesUIDs = EmailUtil.genDeleteCandidates(msgsUIDs, remoteFolder, updateMsgs)

      roomDatabase.msgDao().deleteByUIDs(account.email, folderName, deleteCandidatesUIDs)

      val folderType = FoldersManager.getFolderType(localFolder)
      if (!GeneralUtil.isAppForegrounded() && folderType === FoldersManager.FolderType.INBOX) {
        for (uid in deleteCandidatesUIDs) {
          notificationManager.cancel(uid.toInt())
        }
      }

      val newCandidates = EmailUtil.genNewCandidates(msgsUIDs, remoteFolder, newMsgs)

      val isEncryptedModeEnabled = account.isShowOnlyEncrypted ?: false
      val isNew = !GeneralUtil.isAppForegrounded() && folderType === FoldersManager.FolderType.INBOX

      val msgEntities = MessageEntity.genMessageEntities(
          context = this,
          email = email,
          label = folderName,
          folder = remoteFolder,
          msgs = newCandidates,
          isNew = isNew,
          areAllMsgsEncrypted = isEncryptedModeEnabled
      )

      roomDatabase.msgDao().insertWithReplace(msgEntities)

      if (!isEncryptedModeEnabled) {
        CheckIsLoadedMessagesEncryptedWorker.enqueue(applicationContext, localFolder)
      }

      val updateCandidates = EmailUtil.genUpdateCandidates(mapOfUIDAndMsgFlags, remoteFolder, updateMsgs)
          .map { remoteFolder.getUID(it) to it.flags }.toMap()
      roomDatabase.msgDao().updateFlags(account.email, folderName, updateCandidates)

      updateLocalContactsIfNeeded(remoteFolder, newCandidates)
    } catch (e: RemoteException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    } catch (e: MessagingException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    } catch (e: OperationApplicationException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  override fun onMsgChanged(account: AccountEntity, localFolder: LocalFolder, remoteFolder: IMAPFolder,
                            msg: javax.mail.Message, ownerKey: String, requestCode: Int) {
    val folderType = FoldersManager.getFolderType(localFolder)

    if (!GeneralUtil.isAppForegrounded() && folderType === FoldersManager.FolderType.INBOX) {
      try {
        if (msg.flags.contains(Flags.Flag.SEEN)) {
          notificationManager.cancel(remoteFolder.getUID(msg).toInt())
        }
      } catch (e: MessagingException) {
        e.printStackTrace()
      }
    }
  }

  private fun setupConnectionObserver() {
    connectionLifecycleObserver = ConnectionLifecycleObserver(this)
    lifecycle.addObserver(connectionLifecycleObserver)
    connectionLifecycleObserver.connectionLiveData.observe(this, {
      if (it == true) {
        if (::emailSyncManager.isInitialized) {
          emailSyncManager.beginSync()
        }
      }
    })
  }

  /**
   * Update information about contacts in the local database if current messages from the
   * Sent folder.
   *
   * @param imapFolder The folder where messages exist.
   * @param messages   The received messages.
   */
  private fun updateLocalContactsIfNeeded(imapFolder: IMAPFolder, messages: Array<javax.mail.Message>) {
    try {
      val isSentFolder = listOf(*imapFolder.attributes).contains("\\Sent")

      if (isSentFolder) {
        val emailAndNamePairs = ArrayList<EmailAndNamePair>()
        for (message in messages) {
          emailAndNamePairs.addAll(getEmailAndNamePairs(message))
        }

        EmailAndNameUpdaterService.enqueueWork(this, emailAndNamePairs)
      }
    } catch (e: MessagingException) {
      e.printStackTrace()
      ExceptionUtil.handleError(e)
    }
  }

  /**
   * Generate a list of [EmailAndNamePair] objects from the input message.
   * This information will be retrieved from "to" and "cc" headers.
   *
   * @param msg The input [javax.mail.Message].
   * @return <tt>[List]</tt> of EmailAndNamePair objects, which contains information
   * about
   * emails and names.
   * @throws MessagingException when retrieve information about recipients.
   */
  private fun getEmailAndNamePairs(msg: javax.mail.Message): List<EmailAndNamePair> {
    val pairs = ArrayList<EmailAndNamePair>()

    val addressesTo = msg.getRecipients(javax.mail.Message.RecipientType.TO)
    if (addressesTo != null) {
      for (address in addressesTo) {
        val internetAddress = address as InternetAddress
        pairs.add(EmailAndNamePair(internetAddress.address, internetAddress.personal))
      }
    }

    val addressesCC = msg.getRecipients(javax.mail.Message.RecipientType.CC)
    if (addressesCC != null) {
      for (address in addressesCC) {
        val internetAddress = address as InternetAddress
        pairs.add(EmailAndNamePair(internetAddress.address, internetAddress.personal))
      }
    }

    return pairs
  }

  companion object {
    private val TAG = EmailSyncService::class.java.simpleName

    /**
     * This method can bu used to start [EmailSyncService].
     *
     * @param context Interface to global information about an application environment.
     */
    fun startEmailSyncService(context: Context) {
      val startEmailServiceIntent = Intent(context, EmailSyncService::class.java)
      context.startService(startEmailServiceIntent)
    }

    /**
     * Restart [EmailSyncService].
     *
     * @param context Interface to global information about an application environment.
     */
    fun restart(context: Context) {
      NotificationManagerCompat.from(context).cancelAll()
      val intent = Intent(context, EmailSyncService::class.java)
      context.stopService(intent)
      context.startService(intent)
    }
  }
}
