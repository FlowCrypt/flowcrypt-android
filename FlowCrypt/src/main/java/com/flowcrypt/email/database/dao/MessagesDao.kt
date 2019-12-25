/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import com.flowcrypt.email.database.entity.MessageEntity
import java.util.*
import javax.mail.Flags
import kotlin.collections.ArrayList


/**
 * This class describes available methods for [MessageEntity]
 *
 * @author Denis Bondarenko
 *         Date: 12/15/19
 *         Time: 4:37 PM
 *         E-mail: DenBond7@gmail.com
 */
@Dao
abstract class MessagesDao : BaseDao<MessageEntity> {
  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder AND uid = :uid")
  abstract fun getMessage(account: String, folder: String, uid: Long): MessageEntity?

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder")
  abstract fun getMessages(account: String, folder: String): LiveData<MessageEntity>

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder AND uid IN (:msgsUID)")
  abstract fun getMsgsByUids(account: String?, folder: String?, msgsUID: Collection<Long>?): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder ORDER BY received_date DESC")
  abstract fun getMessagesDataSourceFactory(account: String, folder: String): DataSource
  .Factory<Int, MessageEntity>

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
   * @param email      The email that the message linked.
   * @param label      The folder label.
   * @param oldValue   The old value.
   * @param newValues  The new value.
   * @return The count of the changed rows or -1 up.
   */
  @Query("UPDATE messages SET state=:newValues WHERE state = :oldValue")
  abstract fun changeMsgsState(email: String?, label: String?, oldValue: Int, newValues: Int): LiveData<MessageEntity>

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
          msgEntity.copy(flags = it.toString().toUpperCase(Locale
              .getDefault()), isNew = false)
        } else {
          msgEntity.copy(flags = it.toString().toUpperCase(Locale.getDefault()))
        }
        modifiedMsgEntities.add(modifiedMsgEntity)
      }
    }

    update(modifiedMsgEntities)
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
}