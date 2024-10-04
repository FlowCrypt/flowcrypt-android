/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.account.database.dao

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.dao.BaseDao
import com.flowcrypt.email.database.dao.BaseDao.Companion.doOperationViaSteps
import com.flowcrypt.email.database.dao.BaseDao.Companion.doOperationViaStepsSuspend
import com.flowcrypt.email.database.dao.BaseDao.Companion.getEntitiesViaStepsSuspend
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import jakarta.mail.Flags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * This class describes available methods for [MessageEntity]
 *
 * @author Denys Bondarenko
 */
@Dao
abstract class MessageDao : BaseDao<MessageEntity> {
  @Query("SELECT * FROM messages WHERE account = :account AND folder = :folder AND uid = :uid")
  abstract fun getMsg(account: String?, folder: String?, uid: Long): MessageEntity?

  @Query("SELECT * FROM messages WHERE account = :account AND folder = :folder AND uid = :uid")
  abstract suspend fun getMsgSuspend(account: String?, folder: String?, uid: Long): MessageEntity?

  @Query("SELECT * FROM messages WHERE _id = :id")
  abstract suspend fun getMsgById(id: Long): MessageEntity?

  @Query("SELECT * FROM messages WHERE _id = :id")
  abstract fun getMessageByIdFlow(id: Long): Flow<MessageEntity?>

  @Query("SELECT * FROM messages WHERE account = :account AND folder = :folder")
  abstract fun getMsgsLD(account: String, folder: String): LiveData<MessageEntity>

  @Query("SELECT * FROM messages WHERE account = :account AND folder = :folder")
  abstract fun getMsgs(account: String, folder: String): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE account = :account AND folder = :folder")
  abstract suspend fun getMsgsSuspend(account: String, folder: String): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE account = :account AND folder = :folder AND _id IN (:msgsID)")
  abstract suspend fun getMsgsByIDSuspend(
    account: String,
    folder: String,
    msgsID: Collection<Long>?
  ): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE _id IN (:msgsID)")
  abstract suspend fun getMessagesByIDs(msgsID: Collection<Long>): List<MessageEntity>

  @Query(
    "SELECT * FROM messages " +
        "WHERE account = :account AND folder = :folder AND is_new = 1 ORDER BY :orderBy"
  )
  abstract fun getNewMsgs(
    account: String, folder: String,
    orderBy: String = "received_date ASC"
  ): List<MessageEntity>

  @Query(
    "SELECT * FROM messages " +
        "WHERE account = :account AND folder = :folder AND is_new = 1 ORDER BY :orderBy"
  )
  abstract suspend fun getNewMsgsSuspend(
    account: String, folder: String,
    orderBy: String = "received_date ASC"
  ): List<MessageEntity>

  @Query(
    "SELECT * FROM messages " +
        "WHERE account = :account AND folder = :folder AND uid IN (:msgsUID)"
  )
  abstract fun getMsgsByUids(
    account: String?,
    folder: String?,
    msgsUID: Collection<Long>?
  ): List<MessageEntity>

  @Query(
    "SELECT * FROM messages " +
        "WHERE account = :account AND folder = :folder AND uid IN (:msgsUID)"
  )
  abstract suspend fun getMsgsByUidsSuspend(
    account: String?,
    folder: String?,
    msgsUID: Collection<Long>?
  ): List<MessageEntity>

  @Query(
    "SELECT * FROM messages " +
        "WHERE account = :account AND folder = :folder AND is_visible = 1 ORDER BY received_date DESC"
  )
  abstract fun getMessagesDataSourceFactory(
    account: String,
    folder: String
  ): DataSource.Factory<Int, MessageEntity>

  @Query(
    "SELECT * FROM (" +
        "SELECT * FROM messages " +
        "WHERE account = :account AND folder = :folder AND is_visible = 1 AND received_date > :date " +
        "ORDER BY received_date ASC " +
        "LIMIT :limit) " +
        "UNION " +
        "SELECT * FROM (" +
        "SELECT * FROM messages " +
        "WHERE account = :account AND folder = :folder AND is_visible = 1 AND received_date <= :date " +
        "ORDER BY received_date DESC " +
        "LIMIT :limit) " +
        "ORDER BY received_date DESC"
  )
  abstract suspend fun getMessagesForViewPager(
    account: String,
    folder: String,
    date: Long,
    limit: Int
  ): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE account = :account AND folder = :folder AND uid = :uid")
  abstract fun getMsgLiveData(account: String, folder: String, uid: Long): LiveData<MessageEntity?>

  @Query("SELECT * FROM messages WHERE _id = :id")
  abstract fun getMsgLiveDataById(id: Long): LiveData<MessageEntity?>

  @Query("DELETE FROM messages WHERE account = :account AND folder = :label")
  abstract suspend fun delete(account: String?, label: String?): Int

  @Query("DELETE FROM messages WHERE account = :account AND folder NOT IN (:labels)")
  abstract suspend fun deleteAllExceptRelatedToLabels(
    account: String?,
    labels: Collection<String>
  ): Int

  @Query("DELETE FROM messages WHERE account = :account AND folder = :label AND uid IN (:msgsUID)")
  abstract fun delete(account: String?, label: String?, msgsUID: Collection<Long>): Int

  @Query("DELETE FROM messages WHERE account = :account AND folder = :label AND uid IN (:msgsUID)")
  abstract suspend fun deleteSuspendByUIDs(
    account: String?,
    label: String?,
    msgsUID: Collection<Long>
  ): Int

  @Query("SELECT * FROM messages WHERE account = :account AND folder = :label")
  abstract fun getOutboxMsgs(
    account: String?,
    label: String = JavaEmailConstants.FOLDER_OUTBOX
  ): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE account = :account AND folder = :label")
  abstract fun getOutboxMsgsLD(
    account: String?,
    label: String = JavaEmailConstants.FOLDER_OUTBOX
  ): LiveData<List<MessageEntity>>

  @Query("SELECT * FROM messages WHERE account = :account AND folder = :label")
  abstract suspend fun getOutboxMsgsSuspend(
    account: String?,
    label: String = JavaEmailConstants.FOLDER_OUTBOX
  ): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE folder = :label")
  abstract suspend fun getAllOutboxMessages(
    label: String = JavaEmailConstants.FOLDER_OUTBOX
  ): List<MessageEntity>

  @Query(
    "SELECT * FROM messages " +
        "WHERE account = :account AND folder = :label AND state IN (:msgStates)"
  )
  abstract fun getOutboxMsgsByStates(
    account: String?, label: String = JavaEmailConstants.FOLDER_OUTBOX,
    msgStates: Collection<Int>
  ): List<MessageEntity>

  @Query(
    "SELECT * FROM messages " +
        "WHERE account = :account AND folder = :label AND state IN (:msgStates)"
  )
  abstract suspend fun getOutboxMsgsByStatesSuspend(
    account: String?,
    label: String = JavaEmailConstants.FOLDER_OUTBOX,
    msgStates: Collection<Int>
  ): List<MessageEntity>

  @Query(
    "DELETE FROM messages WHERE account = :account " +
        "AND folder = :label AND uid = :uid AND (state NOT IN (:msgStates) OR state IS NULL)"
  )
  abstract suspend fun deleteOutgoingMsg(
    account: String?, label: String?, uid: Long?,
    msgStates: Collection<Int> = listOf(
      MessageState.SENDING.value,
      MessageState.SENT_WITHOUT_LOCAL_COPY.value,
      MessageState.QUEUED_MAKE_COPY_IN_SENT_FOLDER.value
    )
  ): Int

  @Query(
    "SELECT COUNT(*) FROM messages WHERE account = :account " +
        "AND folder = :label AND (state IN (:msgStates) OR state IS NULL)"
  )
  abstract suspend fun getFailedOutgoingMessagesCountSuspend(
    account: String?,
    label: String = JavaEmailConstants.FOLDER_OUTBOX,
    msgStates: Collection<Int> = listOf(
      MessageState.ERROR_CACHE_PROBLEM.value,
      MessageState.ERROR_DURING_CREATION.value,
      MessageState.ERROR_ORIGINAL_MESSAGE_MISSING.value,
      MessageState.ERROR_ORIGINAL_ATTACHMENT_NOT_FOUND.value,
      MessageState.ERROR_SENDING_FAILED.value,
      MessageState.ERROR_PRIVATE_KEY_NOT_FOUND.value,
      MessageState.ERROR_COPY_NOT_SAVED_IN_SENT_FOLDER.value,
      MessageState.ERROR_PASSWORD_PROTECTED.value
    )
  ): Int

  @Query("SELECT COUNT(*) FROM messages WHERE account = :account AND folder = :folder")
  abstract fun count(account: String?, folder: String?): Int

  @Query("SELECT COUNT(*) FROM messages WHERE account = :account AND folder = :folder")
  abstract suspend fun countSuspend(account: String?, folder: String?): Int?

  @Query("SELECT max(uid) FROM messages WHERE account = :account AND folder = :folder")
  abstract fun getLastUIDOfMsgForLabel(account: String?, folder: String?): Int

  @Query(
    "SELECT * FROM messages " +
        "WHERE account = :account AND folder = :folder ORDER BY uid DESC LIMIT 1"
  )
  abstract suspend fun getNewestMsg(account: String?, folder: String?): MessageEntity?

  @Query(
    "SELECT * FROM messages " +
        "WHERE account = :account AND folder = :folder ORDER BY thread_id DESC LIMIT 1"
  )
  abstract suspend fun getNewestThread(account: String?, folder: String?): MessageEntity?

  @Query("SELECT max(uid) FROM messages WHERE account = :account AND folder = :folder")
  abstract suspend fun getLastUIDOfMsgForLabelSuspend(account: String?, folder: String?): Int?

  @Query("SELECT min(uid) FROM messages WHERE account = :account AND folder = :folder")
  abstract fun getOldestUIDOfMsgForLabel(account: String?, folder: String?): Int

  @Query("SELECT min(uid) FROM messages WHERE account = :account AND folder = :folder")
  abstract suspend fun getOldestUIDOfMsgForLabelSuspend(account: String?, folder: String?): Int?

  @Query("SELECT uid FROM messages WHERE account = :account AND folder = :folder")
  abstract suspend fun getUIDsForLabel(account: String?, folder: String?): List<Long>

  /**
   * Get the list of UID of all messages in the database which were not checked to encryption.
   *
   * @param account   The user account.
   * @param label     The label name.
   * @return The list of UID of selected messages in the database for some label.
   */
  @Query(
    "SELECT uid FROM messages " +
        "WHERE account = :account AND folder = :label AND is_encrypted = -1"
  )
  abstract suspend fun getNotCheckedUIDs(account: String?, label: String): List<Long>

  /**
   * Get a list of UID and flags of all unseen messages in the database for some label.
   *
   * @param account   The user account.
   * @param label     The label name.
   * @return The list of UID and flags of all unseen messages in the database for some label.
   */
  @Query(
    "SELECT uid FROM messages " +
        "WHERE account = :account AND folder = :label AND flags NOT LIKE '%\\SEEN'"
  )
  abstract fun getUIDOfUnseenMsgs(account: String?, label: String): List<Long>

  /**
   * Switch [MessageState] for messages of the given folder of the given account
   *
   * @param account    The account that the message linked.
   * @param label      The folder label.
   * @param oldValue   The old value.
   * @param newValues  The new value.
   * @return The count of the changed rows or -1 up.
   */
  @Query(
    "UPDATE messages SET state=:newValues " +
        "WHERE account = :account AND folder = :label AND state = :oldValue"
  )
  abstract fun changeMsgsState(account: String?, label: String?, oldValue: Int, newValues: Int): Int

  @Query(
    "UPDATE messages SET state=:newValue " +
        "WHERE account = :account AND folder = :label"
  )
  abstract fun changeMsgsState(account: String?, label: String?, newValue: Int? = null): Int

  @Query(
    "UPDATE messages SET state=:newValue " +
        "WHERE account = :account AND folder = :label"
  )
  abstract suspend fun changeMsgsStateSuspend(
    account: String?,
    label: String?,
    newValue: Int? = null
  ): Int

  @Query(
    "UPDATE messages SET state=:newValues " +
        "WHERE account = :account AND folder = :label AND state = :oldValue"
  )
  abstract suspend fun changeMsgsStateSuspend(
    account: String?, label: String?, oldValue: Int,
    newValues: Int
  ): Int

  /**
   * Add the messages which have a current state equal [MessageState.SENDING] to the sending queue again.
   *
   * @param account   The account that the message linked
   */
  @Query(
    "UPDATE messages SET state=2 " +
        "WHERE account = :account AND folder = :label AND state =:oldValue"
  )
  abstract fun resetMsgsWithSendingState(
    account: String?,
    label: String = JavaEmailConstants.FOLDER_OUTBOX,
    oldValue: Int = MessageState.SENDING.value
  ): Int

  /**
   * Add the messages which have a current state equal [MessageState.SENDING] to the sending queue again.
   *
   * @param account   The account that the message linked
   */
  @Query(
    "UPDATE messages SET state=2 " +
        "WHERE account = :account AND folder = :label AND state =:oldValue"
  )
  abstract suspend fun resetMsgsWithSendingStateSuspend(
    account: String?,
    label: String = JavaEmailConstants.FOLDER_OUTBOX,
    oldValue: Int = MessageState.SENDING.value
  ): Int

  @Query("SELECT uid, flags FROM messages WHERE account = :account AND folder = :label")
  abstract fun getUIDAndFlagsPairs(account: String?, label: String): List<UidFlagsPair>

  @Query("SELECT uid, flags FROM messages WHERE account = :account AND folder = :label")
  abstract suspend fun getUIDAndFlagsPairsSuspend(
    account: String?,
    label: String
  ): List<UidFlagsPair>

  @Query("SELECT * FROM messages WHERE account = :account AND state =:stateValue")
  abstract fun getMsgsWithState(account: String?, stateValue: Int): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE account = :account AND state =:stateValue")
  abstract suspend fun getMsgsWithStateSuspend(
    account: String?,
    stateValue: Int
  ): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE account = :account AND folder = :label AND state =:stateValue")
  abstract fun getMsgsWithState(
    account: String?,
    label: String?,
    stateValue: Int
  ): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE account = :account AND folder = :label AND state =:stateValue")
  abstract suspend fun getMsgsWithStateSuspend(
    account: String?,
    label: String?,
    stateValue: Int
  ): List<MessageEntity>

  @Query(
    "UPDATE messages SET is_new = 0 " +
        "WHERE account = :account AND folder = :label AND uid IN (:uidList)"
  )
  abstract suspend fun markMsgsAsOld(
    account: String?,
    label: String?,
    uidList: Collection<Long>
  ): Int

  @Query("UPDATE messages SET is_new = 0 WHERE account = :account AND folder = :label AND is_new = 1")
  abstract suspend fun markMsgsAsOld(account: String?, label: String?): Int

  @Query("DELETE FROM messages WHERE account = :account")
  abstract suspend fun deleteByEmailSuspend(account: String?): Int

  @Query("SELECT COUNT(*) FROM messages WHERE account = :account AND folder = :label")
  abstract suspend fun getMsgsCount(account: String, label: String): Int

  @Query("SELECT * FROM messages WHERE account = :account AND folder = :folder AND thread_id = :threadId")
  abstract suspend fun getMessagesForGmailThread(
    account: String,
    folder: String,
    threadId: Long
  ): List<MessageEntity>

  @Query("DELETE FROM messages WHERE account = :account AND folder = :folder AND thread_id = :threadId AND is_visible = 0")
  abstract suspend fun clearCacheForGmailThread(account: String?, folder: String, threadId: String)

  suspend fun getNewestMsgOrThread(accountEntity: AccountEntity, folder: String?): MessageEntity? {
    return if (accountEntity.isGoogleSignInAccount && accountEntity.useAPI && accountEntity.useConversationMode) {
      getNewestThread(accountEntity.email, folder)
    } else {
      getNewestMsg(accountEntity.email, folder)
    }
  }

  @Transaction
  open fun deleteByUIDs(account: String?, label: String?, msgsUID: Collection<Long>) {
    doOperationViaSteps(list = ArrayList(msgsUID)) { stepUIDs: Collection<Long> ->
      delete(account, label, stepUIDs)
    }
  }

  @Transaction
  open suspend fun deleteByUIDsSuspend(account: String?, label: String?, msgsUID: Collection<Long>) =
    withContext(Dispatchers.IO) {
      doOperationViaStepsSuspend(list = ArrayList(msgsUID)) { stepUIDs: Collection<Long> ->
        deleteSuspendByUIDs(account, label, stepUIDs)
      }
    }

  @Transaction
  open fun updateFlags(account: String?, label: String?, flagsMap: Map<Long, Flags>) {
    doOperationViaSteps(list = ArrayList(flagsMap.keys)) { stepUIDs: Collection<Long> ->
      updateFlagsByUIDs(account, label, flagsMap, stepUIDs)
    }
  }

  @Transaction
  open suspend fun updateFlagsSuspend(account: String?, label: String?, flagsMap: Map<Long, Flags>) =
    withContext(Dispatchers.IO) {
      doOperationViaStepsSuspend(list = ArrayList(flagsMap.keys)) { stepUIDs: Collection<Long> ->
        updateFlagsByUIDsSuspend(account, label, flagsMap, stepUIDs)
      }
    }

  /**
   * Update the message flags in the local database.
   *
   * @param account   The account that the message linked.
   * @param label   The folder label.
   * @param uid     The message UID.
   * @param flags   The message flags.
   */
  @Transaction
  open suspend fun updateLocalMsgFlags(account: String?, label: String?, uid: Long, flags: Flags) =
    withContext(Dispatchers.IO) {
      val msgEntity =
        getMsgSuspend(account = account, folder = label, uid = uid) ?: return@withContext
      val modifiedMsgEntity = if (flags.contains(Flags.Flag.SEEN)) {
        msgEntity.copy(flags = flags.toString().uppercase(), isNew = false)
      } else {
        msgEntity.copy(flags = flags.toString().uppercase())
      }
      updateSuspend(modifiedMsgEntity)
    }

  @Transaction
  open suspend fun updateEncryptionStates(
    account: String?,
    label: String?,
    flagsMap: Map<Long, Boolean>
  ) = withContext(Dispatchers.IO) {
    val msgEntities = getMsgsByUidsSuspend(account = account, folder = label, msgsUID = flagsMap.keys)
    val modifiedMsgEntities = ArrayList<MessageEntity>()

    for (msgEntity in msgEntities) {
      val isEncrypted = flagsMap[msgEntity.uid]
      isEncrypted?.let {
        modifiedMsgEntities.add(msgEntity.copy(isEncrypted = isEncrypted))
      }
    }

    updateSuspend(modifiedMsgEntities)
  }

  /**
   * Get a map of UID and flags of all messages in the database for some label.
   *
   * @param account   The user account.
   * @param label   The label name.
   * @return The map of UID and flags of all messages in the database for some label.
   */
  @Transaction
  open fun getMapOfUIDAndMsgFlags(account: String, label: String): Map<Long, String?> {
    return getUIDAndFlagsPairs(account, label).associate { it.uid to it.flags }
  }

  /**
   * Get a map of UID and flags of all messages in the database for some label.
   *
   * @param account   The user account.
   * @param label   The label name.
   * @return The map of UID and flags of all messages in the database for some label.
   */
  @Transaction
  open suspend fun getMapOfUIDAndMsgFlagsSuspend(account: String, label: String): Map<Long, String?> =
    withContext(Dispatchers.IO) {
      return@withContext getUIDAndFlagsPairsSuspend(account, label).associate { it.uid to it.flags }
    }

  /**
   * Mark messages as old in the local database.
   *
   * @param account   The account that the message linked.
   * @param label   The folder label.
   * @param uidList The list of the UIDs.
   */
  @Transaction
  open suspend fun setOldStatus(account: String?, label: String?, uidList: List<Long>) =
    withContext(Dispatchers.IO) {
      doOperationViaStepsSuspend(list = uidList) { stepUIDs: Collection<Long> ->
        markMsgsAsOld(account, label, stepUIDs)
      }
    }

  open suspend fun getMsgsByUIDs(
    account: String,
    label: String,
    uidList: List<Long>
  ): List<MessageEntity> = withContext(Dispatchers.IO) {
    return@withContext getEntitiesViaStepsSuspend(list = uidList) { stepUIDs: Collection<Long> ->
      getMsgsByUids(account, label, stepUIDs)
    }
  }

  open suspend fun updateGmailLabels(
    account: String?,
    label: String?,
    labelsToBeUpdatedMap: Map<Long, String>
  ) =
    withContext(Dispatchers.IO) {
      if (account == null || label == null) {
        return@withContext
      }

      val messagesToBeUpdated = getMsgsByUIDs(
        account = account,
        label = label,
        uidList = labelsToBeUpdatedMap.keys.toList()
      ).map { entity ->
        entity.copy(labelIds = labelsToBeUpdatedMap[entity.uid])
      }

      updateSuspend(messagesToBeUpdated)
    }

  open suspend fun deleteAllExceptOutgoingAndDraft(
    context: Context,
    accountEntity: AccountEntity
  ): Int {

    val labels = mutableListOf<String>()
    labels.add(JavaEmailConstants.FOLDER_OUTBOX)
    FoldersManager.fromDatabaseSuspend(context, accountEntity).folderDrafts?.let {
      labels.add(it.fullName)
    }

    return deleteAllExceptRelatedToLabels(accountEntity.email, labels)
  }

  private suspend fun updateFlagsByUIDsSuspend(
    account: String?, label: String?, flagsMap: Map<Long, Flags>,
    uids: Collection<Long>?
  ): Int = withContext(Dispatchers.IO) {
    val msgEntities = getMsgsByUidsSuspend(account = account, folder = label, msgsUID = uids)
    val modifiedMsgEntities = ArrayList<MessageEntity>()

    for (msgEntity in msgEntities) {
      val flags = flagsMap[msgEntity.uid]
      flags?.let {
        val modifiedMsgEntity = if (it.contains(Flags.Flag.SEEN)) {
          msgEntity.copy(flags = it.toString().uppercase(), isNew = false)
        } else {
          msgEntity.copy(flags = it.toString().uppercase())
        }
        modifiedMsgEntities.add(modifiedMsgEntity)
      }
    }

    return@withContext updateSuspend(modifiedMsgEntities)
  }

  private fun updateFlagsByUIDs(
    account: String?, label: String?, flagsMap: Map<Long, Flags>,
    uids: Collection<Long>?
  ): Int {
    val msgEntities = getMsgsByUids(account = account, folder = label, msgsUID = uids)
    val modifiedMsgEntities = ArrayList<MessageEntity>()

    for (msgEntity in msgEntities) {
      val flags = flagsMap[msgEntity.uid]
      flags?.let {
        val modifiedMsgEntity = if (it.contains(Flags.Flag.SEEN)) {
          msgEntity.copy(flags = it.toString().uppercase(), isNew = false)
        } else {
          msgEntity.copy(flags = it.toString().uppercase())
        }
        modifiedMsgEntities.add(modifiedMsgEntity)
      }
    }

    return update(modifiedMsgEntities)
  }

  data class UidFlagsPair(val uid: Long, val flags: String? = null)
}
