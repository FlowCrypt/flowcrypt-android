/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

import android.content.Context
import androidx.work.WorkerParameters
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.extensions.isAppForegrounded
import com.flowcrypt.email.extensions.kotlin.toHex
import com.flowcrypt.email.service.MessagesNotificationManager
import jakarta.mail.Flags
import jakarta.mail.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.angus.mail.imap.IMAPFolder

/**
 * @author Denys Bondarenko
 */
abstract class BaseIdleWorker(context: Context, params: WorkerParameters) :
  BaseSyncWorker(context, params) {
  protected val notificationManager = MessagesNotificationManager(applicationContext)

  protected suspend fun processDeletedMsgs(
    cachedUIDSet: Set<Long>, remoteFolder: IMAPFolder,
    msgs: Array<Message>, accountEntity: AccountEntity,
    folderFullName: String
  ) = withContext(Dispatchers.IO) {
    val deleteCandidatesUIDs = EmailUtil.genDeleteCandidates(cachedUIDSet, remoteFolder, msgs)
    processDeletedMsgs(accountEntity, folderFullName, deleteCandidatesUIDs)
  }

  protected suspend fun processDeletedMsgs(
    accountEntity: AccountEntity, folderFullName: String,
    deleteCandidatesUIDs: Collection<Long>
  ) {
    roomDatabase.msgDao()
      .deleteByUIDsSuspend(accountEntity.email, folderFullName, deleteCandidatesUIDs)
    tryToRemoveNotifications(deleteCandidatesUIDs)
  }

  protected fun tryToRemoveNotifications(uidList: Collection<Long>) {
    if (!applicationContext.isAppForegrounded()) {
      for (uid in uidList) {
        notificationManager.cancel(uid.toHex())
      }
    }
  }

  protected suspend fun processUpdatedMsgs(
    mapOfUIDAndMsgFlags: Map<Long, String?>,
    remoteFolder: IMAPFolder,
    msgs: Array<Message>,
    accountEntity: AccountEntity, folderFullName: String
  ) = withContext(Dispatchers.IO) {
    val updateCandidates = EmailUtil.genUpdateCandidates(mapOfUIDAndMsgFlags, remoteFolder, msgs)
      .associate { remoteFolder.getUID(it) to it.flags }
    processUpdatedMsgs(accountEntity, folderFullName, updateCandidates)
  }

  protected suspend fun processUpdatedMsgs(
    accountEntity: AccountEntity,
    folderFullName: String,
    updateCandidates: Map<Long, Flags>
  ) {
    roomDatabase.msgDao().updateFlagsSuspend(accountEntity.email, folderFullName, updateCandidates)

    removeNotificationForSeenMessages(updateCandidates)
  }

  protected fun removeNotificationForSeenMessages(updateCandidates: Map<Long, Flags>) {
    if (!applicationContext.isAppForegrounded()) {
      for (item in updateCandidates) {
        val uid = item.key
        if (item.value.contains(Flags.Flag.SEEN)) {
          notificationManager.cancel(uid.toHex())
        }
      }
    }
  }

  protected suspend fun processNewMsgs(
    newMsgs: Array<Message>, accountEntity: AccountEntity,
    localFolder: LocalFolder, remoteFolder: IMAPFolder
  ) = withContext(Dispatchers.IO) {
    if (newMsgs.isNotEmpty()) {
      //fetch base details
      EmailUtil.fetchMsgs(remoteFolder, newMsgs)
      //here we do additional search over fetched messages(over the content) to check PGP things
      val hasPgpAfterAdditionalSearchSet =
        remoteFolder.search(EmailUtil.genPgpThingsSearchTerm(accountEntity), newMsgs)
          .map { remoteFolder.getUID(it) }.toSet()

      val msgEntities = MessageEntity.genMessageEntities(
        context = applicationContext,
        account = accountEntity.email,
        accountType = accountEntity.accountType,
        label = localFolder.fullName,
        folder = remoteFolder,
        msgs = newMsgs,
        isNew = !applicationContext.isAppForegrounded(),
        isOnlyPgpModeEnabled = false,
        hasPgpAfterAdditionalSearchSet = hasPgpAfterAdditionalSearchSet
      )

      processNewMsgs(accountEntity, localFolder, msgEntities)
    }
  }

  protected suspend fun processNewMsgs(
    accountEntity: AccountEntity,
    localFolder: LocalFolder,
    msgEntities: List<MessageEntity>
  ) {
    roomDatabase.msgDao().insertWithReplaceSuspend(msgEntities)

    tryToShowNotificationsForNewMessages(accountEntity, localFolder)
  }

  protected suspend fun tryToShowNotificationsForNewMessages(
    accountEntity: AccountEntity,
    localFolder: LocalFolder
  ) {
    if (!applicationContext.isAppForegrounded()) {
      val detailsList =
        roomDatabase.msgDao().getNewMsgsSuspend(accountEntity.email, localFolder.fullName)
      notificationManager.notify(applicationContext, accountEntity, localFolder, detailsList)
    }
  }
}
