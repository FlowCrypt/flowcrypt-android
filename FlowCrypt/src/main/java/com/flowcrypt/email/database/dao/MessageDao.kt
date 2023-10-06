/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.dao.BaseDao.Companion.doOperationViaSteps
import com.flowcrypt.email.database.dao.BaseDao.Companion.doOperationViaStepsSuspend
import com.flowcrypt.email.database.dao.BaseDao.Companion.getEntitiesViaStepsSuspend
import com.flowcrypt.email.database.entity.MessageEntity
import jakarta.mail.Flags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * This class describes available methods for [MessageEntity]
 *
 * @author Denys Bondarenko
 */
@Dao
abstract class MessageDao : BaseDao<MessageEntity> {
  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder AND uid = :uid")
  abstract fun getMsg(account: String?, folder: String?, uid: Long): MessageEntity?

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder AND uid = :uid")
  abstract suspend fun getMsgSuspend(account: String?, folder: String?, uid: Long): MessageEntity?

  @Query("SELECT * FROM messages WHERE _id = :id")
  abstract suspend fun getMsgById(id: Long): MessageEntity?

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder")
  abstract fun getMsgsLD(account: String, folder: String): LiveData<MessageEntity>

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder")
  abstract fun getMsgs(account: String, folder: String): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder")
  abstract suspend fun getMsgsSuspend(account: String, folder: String): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder AND _id IN (:msgsID)")
  abstract suspend fun getMsgsByIDSuspend(
    account: String,
    folder: String,
    msgsID: Collection<Long>?
  ): List<MessageEntity>

  @Query(
    "SELECT * FROM messages " +
        "WHERE email = :account AND folder = :folder AND is_new = 1 ORDER BY :orderBy"
  )
  abstract fun getNewMsgs(
    account: String, folder: String,
    orderBy: String = "received_date ASC"
  ): List<MessageEntity>

  @Query(
    "SELECT * FROM messages " +
        "WHERE email = :account AND folder = :folder AND is_new = 1 ORDER BY :orderBy"
  )
  abstract suspend fun getNewMsgsSuspend(
    account: String, folder: String,
    orderBy: String = "received_date ASC"
  ): List<MessageEntity>

  @Query(
    "SELECT * FROM messages " +
        "WHERE email = :account AND folder = :folder AND uid IN (:msgsUID)"
  )
  abstract fun getMsgsByUids(
    account: String?,
    folder: String?,
    msgsUID: Collection<Long>?
  ): List<MessageEntity>

  @Query(
    "SELECT * FROM messages " +
        "WHERE email = :account AND folder = :folder AND uid IN (:msgsUID)"
  )
  abstract suspend fun getMsgsByUidsSuspend(
    account: String?,
    folder: String?,
    msgsUID: Collection<Long>?
  ): List<MessageEntity>

  @Query(
    "SELECT * FROM messages " +
        "WHERE email = :account AND folder = :folder ORDER BY received_date DESC"
  )
  abstract fun getMessagesDataSourceFactory(
    account: String,
    folder: String
  ): DataSource.Factory<Int, MessageEntity>

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder AND uid = :uid")
  abstract fun getMsgLiveData(account: String, folder: String, uid: Long): LiveData<MessageEntity?>

  @Query("SELECT * FROM messages WHERE _id = :id")
  abstract fun getMsgLiveDataById(id: Long): LiveData<MessageEntity?>

  @Query("DELETE FROM messages WHERE email = :email AND folder = :label")
  abstract suspend fun delete(email: String?, label: String?): Int

  @Query("DELETE FROM messages WHERE email = :email AND folder != :label")
  abstract suspend fun deleteAllExceptOutgoing(
    email: String?,
    label: String = JavaEmailConstants.FOLDER_OUTBOX
  ): Int

  @Query("DELETE FROM messages WHERE email = :email AND folder = :label AND uid IN (:msgsUID)")
  abstract fun delete(email: String?, label: String?, msgsUID: Collection<Long>): Int

  @Query("DELETE FROM messages WHERE email = :email AND folder = :label AND uid IN (:msgsUID)")
  abstract suspend fun deleteSuspendByUIDs(
    email: String?,
    label: String?,
    msgsUID: Collection<Long>
  ): Int

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :label")
  abstract fun getOutboxMsgs(
    account: String?,
    label: String = JavaEmailConstants.FOLDER_OUTBOX
  ): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :label")
  abstract fun getOutboxMsgsLD(
    account: String?,
    label: String = JavaEmailConstants.FOLDER_OUTBOX
  ): LiveData<List<MessageEntity>>

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :label")
  abstract suspend fun getOutboxMsgsSuspend(
    account: String?,
    label: String = JavaEmailConstants.FOLDER_OUTBOX
  ): List<MessageEntity>

  @Query(
    "SELECT * FROM messages " +
        "WHERE email = :account AND folder = :label AND state IN (:msgStates)"
  )
  abstract fun getOutboxMsgsByStates(
    account: String?, label: String = JavaEmailConstants.FOLDER_OUTBOX,
    msgStates: Collection<Int>
  ): List<MessageEntity>

  @Query(
    "SELECT * FROM messages " +
        "WHERE email = :account AND folder = :label AND state IN (:msgStates)"
  )
  abstract suspend fun getOutboxMsgsByStatesSuspend(
    account: String?,
    label: String = JavaEmailConstants.FOLDER_OUTBOX,
    msgStates: Collection<Int>
  ): List<MessageEntity>

  @Query(
    "DELETE FROM messages WHERE email = :email " +
        "AND folder = :label AND uid = :uid AND (state NOT IN (:msgStates) OR state IS NULL)"
  )
  abstract suspend fun deleteOutgoingMsg(
    email: String?, label: String?, uid: Long?,
    msgStates: Collection<Int> = listOf(
      MessageState.SENDING.value,
      MessageState.SENT_WITHOUT_LOCAL_COPY.value,
      MessageState.QUEUED_MAKE_COPY_IN_SENT_FOLDER.value
    )
  ): Int

  @Query(
    "SELECT COUNT(*) FROM messages WHERE email = :account " +
        "AND folder = :label AND (state IN (:msgStates) OR state IS NULL)"
  )
  abstract fun getFailedOutgoingMsgsCount(
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

  @Query(
    "SELECT COUNT(*) FROM messages WHERE email = :account " +
        "AND folder = :label AND (state IN (:msgStates) OR state IS NULL)"
  )
  abstract suspend fun getFailedOutgoingMsgsCountSuspend(
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
  ): Int?

  @Query("SELECT COUNT(*) FROM messages WHERE email = :account AND folder = :folder")
  abstract fun count(account: String?, folder: String?): Int

  @Query("SELECT COUNT(*) FROM messages WHERE email = :account AND folder = :folder")
  abstract suspend fun countSuspend(account: String?, folder: String?): Int?

  @Query("SELECT max(uid) FROM messages WHERE email = :account AND folder = :folder")
  abstract fun getLastUIDOfMsgForLabel(account: String?, folder: String?): Int

  @Query(
    "SELECT * FROM messages " +
        "WHERE email = :account AND folder = :folder ORDER BY uid DESC LIMIT 1"
  )
  abstract suspend fun getNewestMsg(account: String?, folder: String?): MessageEntity?

  @Query("SELECT max(uid) FROM messages WHERE email = :account AND folder = :folder")
  abstract suspend fun getLastUIDOfMsgForLabelSuspend(account: String?, folder: String?): Int?

  @Query("SELECT min(uid) FROM messages WHERE email = :account AND folder = :folder")
  abstract fun getOldestUIDOfMsgForLabel(account: String?, folder: String?): Int

  @Query("SELECT min(uid) FROM messages WHERE email = :account AND folder = :folder")
  abstract suspend fun getOldestUIDOfMsgForLabelSuspend(account: String?, folder: String?): Int?

  @Query("SELECT uid FROM messages WHERE email = :account AND folder = :folder")
  abstract suspend fun getUIDsForLabel(account: String?, folder: String?): List<Long>

  /**
   * Get the list of UID of all messages in the database which were not checked to encryption.
   *
   * @param account   The user email.
   * @param label     The label name.
   * @return The list of UID of selected messages in the database for some label.
   */
  @Query(
    "SELECT uid FROM messages " +
        "WHERE email = :account AND folder = :label AND is_encrypted = -1"
  )
  abstract suspend fun getNotCheckedUIDs(account: String?, label: String): List<Long>

  /**
   * Get a list of UID and flags of all unseen messages in the database for some label.
   *
   * @param account   The user email.
   * @param label     The label name.
   * @return The list of UID and flags of all unseen messages in the database for some label.
   */
  @Query(
    "SELECT uid FROM messages " +
        "WHERE email = :account AND folder = :label AND flags NOT LIKE '%\\SEEN'"
  )
  abstract fun getUIDOfUnseenMsgs(account: String?, label: String): List<Long>

  /**
   * Switch [MessageState] for messages of the given folder of the given account
   *
   * @param account      The email that the message linked.
   * @param label      The folder label.
   * @param oldValue   The old value.
   * @param newValues  The new value.
   * @return The count of the changed rows or -1 up.
   */
  @Query(
    "UPDATE messages SET state=:newValues " +
        "WHERE email = :account AND folder = :label AND state = :oldValue"
  )
  abstract fun changeMsgsState(account: String?, label: String?, oldValue: Int, newValues: Int): Int

  @Query(
    "UPDATE messages SET state=:newValue " +
        "WHERE email = :account AND folder = :label"
  )
  abstract fun changeMsgsState(account: String?, label: String?, newValue: Int? = null): Int

  @Query(
    "UPDATE messages SET state=:newValue " +
        "WHERE email = :account AND folder = :label"
  )
  abstract suspend fun changeMsgsStateSuspend(
    account: String?,
    label: String?,
    newValue: Int? = null
  ): Int

  @Query(
    "UPDATE messages SET state=:newValues " +
        "WHERE email = :account AND folder = :label AND state = :oldValue"
  )
  abstract suspend fun changeMsgsStateSuspend(
    account: String?, label: String?, oldValue: Int,
    newValues: Int
  ): Int

  /**
   * Add the messages which have a current state equal [MessageState.SENDING] to the sending queue again.
   *
   * @param account   The email that the message linked
   */
  @Query(
    "UPDATE messages SET state=2 " +
        "WHERE email = :account AND folder = :label AND state =:oldValue"
  )
  abstract fun resetMsgsWithSendingState(
    account: String?,
    label: String = JavaEmailConstants.FOLDER_OUTBOX,
    oldValue: Int = MessageState.SENDING.value
  ): Int

  /**
   * Add the messages which have a current state equal [MessageState.SENDING] to the sending queue again.
   *
   * @param account   The email that the message linked
   */
  @Query(
    "UPDATE messages SET state=2 " +
        "WHERE email = :account AND folder = :label AND state =:oldValue"
  )
  abstract suspend fun resetMsgsWithSendingStateSuspend(
    account: String?,
    label: String = JavaEmailConstants.FOLDER_OUTBOX,
    oldValue: Int = MessageState.SENDING.value
  ): Int

  @Query("SELECT uid, flags FROM messages WHERE email = :account AND folder = :label")
  abstract fun getUIDAndFlagsPairs(account: String?, label: String): List<UidFlagsPair>

  @Query("SELECT uid, flags FROM messages WHERE email = :account AND folder = :label")
  abstract suspend fun getUIDAndFlagsPairsSuspend(
    account: String?,
    label: String
  ): List<UidFlagsPair>

  @Query("SELECT * FROM messages WHERE email = :account AND state =:stateValue")
  abstract fun getMsgsWithState(account: String?, stateValue: Int): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE email = :account AND state =:stateValue")
  abstract suspend fun getMsgsWithStateSuspend(
    account: String?,
    stateValue: Int
  ): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :label AND state =:stateValue")
  abstract fun getMsgsWithState(
    account: String?,
    label: String?,
    stateValue: Int
  ): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :label AND state =:stateValue")
  abstract suspend fun getMsgsWithStateSuspend(
    account: String?,
    label: String?,
    stateValue: Int
  ): List<MessageEntity>

  @Query(
    "UPDATE messages SET is_new = 0 " +
        "WHERE email = :account AND folder = :label AND uid IN (:uidList)"
  )
  abstract suspend fun markMsgsAsOld(
    account: String?,
    label: String?,
    uidList: Collection<Long>
  ): Int

  @Query("UPDATE messages SET is_new = 0 WHERE email = :account AND folder = :label AND is_new = 1")
  abstract suspend fun markMsgsAsOld(account: String?, label: String?): Int

  @Query("DELETE FROM messages WHERE email = :email")
  abstract suspend fun deleteByEmailSuspend(email: String?): Int

  @Query("SELECT COUNT(*) FROM messages WHERE email = :account AND folder = :label")
  abstract suspend fun getMsgsCount(account: String, label: String): Int

  @Transaction
  open fun deleteByUIDs(email: String?, label: String?, msgsUID: Collection<Long>) {
    doOperationViaSteps(list = ArrayList(msgsUID)) { stepUIDs: Collection<Long> ->
      delete(email, label, stepUIDs)
    }
  }

  @Transaction
  open suspend fun deleteByUIDsSuspend(email: String?, label: String?, msgsUID: Collection<Long>) =
    withContext(Dispatchers.IO) {
      doOperationViaStepsSuspend(list = ArrayList(msgsUID)) { stepUIDs: Collection<Long> ->
        deleteSuspendByUIDs(email, label, stepUIDs)
      }
    }

  @Transaction
  open fun updateFlags(email: String?, label: String?, flagsMap: Map<Long, Flags>) {
    doOperationViaSteps(list = ArrayList(flagsMap.keys)) { stepUIDs: Collection<Long> ->
      updateFlagsByUIDs(email, label, flagsMap, stepUIDs)
    }
  }

  @Transaction
  open suspend fun updateFlagsSuspend(email: String?, label: String?, flagsMap: Map<Long, Flags>) =
    withContext(Dispatchers.IO) {
      doOperationViaStepsSuspend(list = ArrayList(flagsMap.keys)) { stepUIDs: Collection<Long> ->
        updateFlagsByUIDsSuspend(email, label, flagsMap, stepUIDs)
      }
    }

  /**
   * Update the message flags in the local database.
   *
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param uid     The message UID.
   * @param flags   The message flags.
   */
  @Transaction
  open suspend fun updateLocalMsgFlags(email: String?, label: String?, uid: Long, flags: Flags) =
    withContext(Dispatchers.IO) {
      val msgEntity =
        getMsgSuspend(account = email, folder = label, uid = uid) ?: return@withContext
      val modifiedMsgEntity = if (flags.contains(Flags.Flag.SEEN)) {
        msgEntity.copy(flags = flags.toString().uppercase(), isNew = false)
      } else {
        msgEntity.copy(flags = flags.toString().uppercase())
      }
      updateSuspend(modifiedMsgEntity)
    }

  @Transaction
  open suspend fun updateEncryptionStates(
    email: String?,
    label: String?,
    flagsMap: Map<Long, Boolean>
  ) = withContext(Dispatchers.IO) {
    val msgEntities = getMsgsByUidsSuspend(account = email, folder = label, msgsUID = flagsMap.keys)
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
   * @param email   The user email.
   * @param label   The label name.
   * @return The map of UID and flags of all messages in the database for some label.
   */
  @Transaction
  open fun getMapOfUIDAndMsgFlags(email: String, label: String): Map<Long, String?> {
    return getUIDAndFlagsPairs(email, label).associate { it.uid to it.flags }
  }

  /**
   * Get a map of UID and flags of all messages in the database for some label.
   *
   * @param email   The user email.
   * @param label   The label name.
   * @return The map of UID and flags of all messages in the database for some label.
   */
  @Transaction
  open suspend fun getMapOfUIDAndMsgFlagsSuspend(email: String, label: String): Map<Long, String?> =
    withContext(Dispatchers.IO) {
      return@withContext getUIDAndFlagsPairsSuspend(email, label).associate { it.uid to it.flags }
    }

  /**
   * Mark messages as old in the local database.
   *
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param uidList The list of the UIDs.
   */
  @Transaction
  open suspend fun setOldStatus(email: String?, label: String?, uidList: List<Long>) =
    withContext(Dispatchers.IO) {
      doOperationViaStepsSuspend(list = uidList) { stepUIDs: Collection<Long> ->
        markMsgsAsOld(email, label, stepUIDs)
      }
    }

  open suspend fun getMsgsByUIDs(
    email: String,
    label: String,
    uidList: List<Long>
  ): List<MessageEntity> = withContext(Dispatchers.IO) {
    return@withContext getEntitiesViaStepsSuspend(list = uidList) { stepUIDs: Collection<Long> ->
      getMsgsByUids(email, label, stepUIDs)
    }
  }

  open suspend fun updateGmailLabels(
    email: String?,
    label: String?,
    labelsToBeUpdatedMap: Map<Long, String>
  ) =
    withContext(Dispatchers.IO) {
      if (email == null || label == null) {
        return@withContext
      }

      val messagesToBeUpdated = getMsgsByUIDs(
        email = email,
        label = label,
        uidList = labelsToBeUpdatedMap.keys.toList()
      ).map { entity ->
        entity.copy(labelIds = labelsToBeUpdatedMap[entity.uid])
      }

      updateSuspend(messagesToBeUpdated)
    }

  private suspend fun updateFlagsByUIDsSuspend(
    email: String?, label: String?, flagsMap: Map<Long, Flags>,
    uids: Collection<Long>?
  ): Int = withContext(Dispatchers.IO) {
    val msgEntities = getMsgsByUidsSuspend(account = email, folder = label, msgsUID = uids)
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
    email: String?, label: String?, flagsMap: Map<Long, Flags>,
    uids: Collection<Long>?
  ): Int {
    val msgEntities = getMsgsByUids(account = email, folder = label, msgsUID = uids)
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
