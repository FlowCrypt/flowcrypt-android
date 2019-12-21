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
  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder")
  abstract fun getMessages(account: String, folder: String): LiveData<MessageEntity>

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder ORDER BY received_date DESC")
  abstract fun getMessagesDataSourceFactory(account: String, folder: String): DataSource
  .Factory<Int, MessageEntity>

  @Query("SELECT * FROM messages")
  abstract fun msgs(): DataSource.Factory<Int, MessageEntity>

  @Query("DELETE FROM messages WHERE email = :email AND folder = :label")
  abstract suspend fun delete(email: String?, label: String?): Int

  @Query("DELETE FROM messages WHERE email = :email AND folder = :label AND uid IN (:msgsUID)")
  abstract fun delete(email: String?, label: String?, msgsUID: Collection<Long>): Int

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :label AND state NOT IN (:msgStates)")
  abstract fun getOutgoingMessages(account: String?, label: String = JavaEmailConstants.FOLDER_OUTBOX,
                                   msgStates: Collection<Int> = listOf(
                                       MessageState.SENDING.value,
                                       MessageState.SENT_WITHOUT_LOCAL_COPY.value)): List<MessageEntity>

  @Query("DELETE FROM messages WHERE email = :email AND folder = :label AND uid = :uid AND state NOT IN (:msgStates)")
  abstract suspend fun deleteOutgoingMsg(email: String?, label: String?, uid: Long?,
                                         msgStates: Collection<Int> = listOf(
                                             MessageState.SENDING.value,
                                             MessageState.SENT_WITHOUT_LOCAL_COPY.value)): Int

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
}