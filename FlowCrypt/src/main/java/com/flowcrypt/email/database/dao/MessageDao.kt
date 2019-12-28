/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.MessageEntity
import java.util.*
import javax.mail.Flags
import kotlin.collections.ArrayList

/**
 * This class describes available methods for [MessageEntity]
 *
 * @author DenBond7
 * Date: 20.06.2017
 * Time: 10:49
 * E-mail: DenBond7@gmail.com
 */
@Dao
abstract class MessageDao : BaseDao<MessageEntity> {
  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder AND uid = :uid")
  abstract fun getMsg(account: String?, folder: String?, uid: Long): MessageEntity?

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder")
  abstract fun getMsgs(account: String, folder: String): LiveData<MessageEntity>

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder AND is_new = 1 ORDER BY :orderBy")
  abstract fun getNewMsgs(account: String, folder: String,
                          orderBy: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            "received_date ASC"
                          } else {
                            "received_date DESC"
                          }): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder AND uid IN (:msgsUID)")
  abstract fun getMsgsByUids(account: String?, folder: String?, msgsUID: Collection<Long>?): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder ORDER BY received_date DESC")
  abstract fun getMessagesDataSourceFactory(account: String, folder: String): DataSource
  .Factory<Int, MessageEntity>

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder AND uid = :uid")
  abstract fun getMsgLiveData(account: String, folder: String, uid: Long): LiveData<MessageEntity?>

  @Query("DELETE FROM messages WHERE email = :email AND folder = :label")
  abstract suspend fun delete(email: String?, label: String?): Int

  @Query("DELETE FROM messages WHERE email = :email AND folder = :label AND uid IN (:msgsUID)")
  abstract fun delete(email: String?, label: String?, msgsUID: Collection<Long>): Int

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :label AND state NOT IN (:msgStates)")
  abstract fun getOutboxMessages(account: String?, label: String = JavaEmailConstants.FOLDER_OUTBOX,
                                 msgStates: Collection<Int> = listOf(
                                     MessageState.SENDING.value,
                                     MessageState.SENT_WITHOUT_LOCAL_COPY.value)): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :label AND state NOT IN (:msgStateValue)")
  abstract fun getOutboxMessages(account: String?, label: String = JavaEmailConstants.FOLDER_OUTBOX,
                                 msgStateValue: Int): List<MessageEntity>

  @Query("DELETE FROM messages WHERE email = :email AND folder = :label AND uid = :uid AND state NOT IN (:msgStates)")
  abstract suspend fun deleteOutgoingMsg(email: String?, label: String?, uid: Long?,
                                         msgStates: Collection<Int> = listOf(
                                             MessageState.SENDING.value,
                                             MessageState.SENT_WITHOUT_LOCAL_COPY.value)): Int

  @Query("SELECT COUNT(*) FROM messages WHERE email = :account AND folder = :folder")
  abstract fun count(account: String?, folder: String?): Int

  @Query("SELECT max(uid) FROM messages WHERE email = :account AND folder = :folder")
  abstract fun getLastUIDOfMsgForLabel(account: String?, folder: String?): Int

  @Query("SELECT min(uid) FROM messages WHERE email = :account AND folder = :folder")
  abstract fun getOldestUIDOfMsgForLabel(account: String?, folder: String?): Int

  /**
   * Get the list of UID of all messages in the database which were not checked to encryption.
   *
   * @param account   The user email.
   * @param label     The label name.
   * @return The list of UID of selected messages in the database for some label.
   */
  @Query("SELECT uid FROM messages WHERE email = :account AND folder = :label AND is_encrypted = -1")
  abstract fun getNotCheckedUIDs(account: String?, label: String): List<Long>

  /**
   * Get a list of UID and flags of all unseen messages in the database for some label.
   *
   * @param account   The user email.
   * @param label     The label name.
   * @return The list of UID and flags of all unseen messages in the database for some label.
   */
  @Query("SELECT uid FROM messages WHERE email = :account AND folder = :label AND flags NOT LIKE '%\\SEEN'")
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
  @Query("UPDATE messages SET state=:newValues WHERE email = :account AND folder = :label AND state = :oldValue")
  abstract fun changeMsgsState(account: String?, label: String?, oldValue: Int, newValues: Int): Int

  /**
   * Add the messages which have a current state equal [MessageState.SENDING] to the sending queue again.
   *
   * @param account   The email that the message linked
   */
  @Query("UPDATE messages SET state=2 WHERE email = :account AND folder = :label AND state =:oldValue")
  abstract fun resetMsgsWithSendingState(account: String?,
                                         label: String = JavaEmailConstants.FOLDER_OUTBOX,
                                         oldValue: Int = MessageState.SENDING.value): Int

  @Query("SELECT uid, flags FROM messages WHERE email = :account AND folder = :label")
  abstract fun getUIDAndFlagsPairs(account: String?, label: String): List<UidFlagsPair>

  @Query("SELECT * FROM messages WHERE email = :account AND state =:stateValue")
  abstract fun getMsgsWithState(account: String?, stateValue: Int): List<MessageEntity>

  @Query("UPDATE messages SET is_new = 0 WHERE email = :account AND folder = :label AND uid IN (:uidList)")
  abstract suspend fun markMsgsAsOld(account: String?, label: String?, uidList: Collection<Long>): Int

  @Query("UPDATE messages SET is_new = 0 WHERE email = :account AND folder = :label AND is_new = 1")
  abstract suspend fun markMsgsAsOld(account: String?, label: String?): Int

  @Transaction
  open fun deleteByUIDs(email: String?, label: String?, msgsUID: Collection<Long>) {
    val step = 50
    val list = ArrayList(msgsUID)

    if (msgsUID.size <= step) {
      delete(email, label, msgsUID)
    } else {
      var i = 0
      while (i < list.size) {
        val stepUIDs = if (list.size - i > step) {
          list.subList(i, i + step)
        } else {
          list.subList(i, list.size)
        }
        delete(email, label, stepUIDs)
        i += step
      }
    }
  }

  @Transaction
  open fun updateFlags(email: String?, label: String?, flagsMap: Map<Long, Flags>) {
    val msgEntities = getMsgsByUids(account = email, folder = label, msgsUID = flagsMap.keys)
    val modifiedMsgEntities = ArrayList<MessageEntity>()

    for (msgEntity in msgEntities) {
      val flags = flagsMap[msgEntity.uid]
      flags?.let {
        val modifiedMsgEntity = if (it.contains(Flags.Flag.SEEN)) {
          msgEntity.copy(flags = it.toString().toUpperCase(Locale.getDefault()), isNew = false)
        } else {
          msgEntity.copy(flags = it.toString().toUpperCase(Locale.getDefault()))
        }
        modifiedMsgEntities.add(modifiedMsgEntity)
      }
    }

    update(modifiedMsgEntities)
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
  open fun updateLocalMsgFlags(email: String?, label: String?, uid: Long, flags: Flags) {
    val msgEntity = getMsg(account = email, folder = label, uid = uid)
    val modifiedMsgEntity = if (flags.contains(Flags.Flag.SEEN)) {
      msgEntity?.copy(flags = flags.toString().toUpperCase(Locale.getDefault()), isNew = false)
    } else {
      msgEntity?.copy(flags = flags.toString().toUpperCase(Locale.getDefault()))
    }

    modifiedMsgEntity?.let {
      update(it)
    }
  }

  @Transaction
  open fun updateEncryptionStates(email: String?, label: String?, flagsMap: Map<Long, Boolean>) {
    val msgEntities = getMsgsByUids(account = email, folder = label, msgsUID = flagsMap.keys)
    val modifiedMsgEntities = ArrayList<MessageEntity>()

    for (msgEntity in msgEntities) {
      val isEncrypted = flagsMap[msgEntity.uid]
      isEncrypted?.let {
        modifiedMsgEntities.add(msgEntity.copy(isEncrypted = isEncrypted))
      }
    }

    update(modifiedMsgEntities)
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
    return getUIDAndFlagsPairs(email, label).map { it.uid to it.flags }.toMap()
  }

  /**
   * Mark messages as old in the local database.
   *
   * @param email   The email that the message linked.
   * @param label   The folder label.
   * @param uidList The list of the UIDs.
   */
  @Transaction
  open suspend fun setOldStatus(email: String?, label: String?, uidList: List<Long>) {
    val step = 50
    if (uidList.isNotEmpty()) {
      if (uidList.size <= step) {
        markMsgsAsOld(email, label, uidList)
      } else {
        var i = 0
        while (i < uidList.size) {
          val tempList = if (uidList.size - i > step) {
            uidList.subList(i, i + step)
          } else {
            uidList.subList(i, uidList.size)
          }
          markMsgsAsOld(email, label, tempList)
          i += step
        }
      }
    }
  }

  data class UidFlagsPair(val uid: Long, val flags: String? = null)
}
