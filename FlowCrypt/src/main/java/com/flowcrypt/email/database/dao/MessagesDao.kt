/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import com.flowcrypt.email.database.entity.MessageEntity

/**
 * This class describes available methods for [MessageEntity]
 *
 * @author Denis Bondarenko
 *         Date: 12/15/19
 *         Time: 4:37 PM
 *         E-mail: DenBond7@gmail.com
 */
@Dao
interface MessagesDao {
  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder")
  fun getMessages(account: String, folder: String): LiveData<MessageEntity>

  @Query("SELECT * FROM messages WHERE email = :account AND folder = :folder")
  fun getMessagesDataSourceFactory(account: String, folder: String): DataSource.Factory<Int, MessageEntity>

  @Query("SELECT * FROM messages")
  fun msgs(): DataSource.Factory<Int, MessageEntity>
}