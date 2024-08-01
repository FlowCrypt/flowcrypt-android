/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.flowcrypt.email.database.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

/**
 * @author Denys Bondarenko
 */
@Dao
interface AttachmentDao : BaseDao<AttachmentEntity> {

  @Query(
    "SELECT * FROM attachment WHERE account = :account " +
        "AND account_type = :accountType AND folder = :label AND uid = :uid"
  )
  suspend fun getAttachments(
    account: String,
    accountType: String,
    label: String,
    uid: Long
  ): List<AttachmentEntity>

  @Query(
    "SELECT * FROM attachment WHERE account = :account " +
        "AND account_type = :accountType AND folder = :label AND uid = :uid"
  )
  fun getAttachmentsFlow(
    account: String,
    accountType: String,
    label: String,
    uid: Long
  ): Flow<List<AttachmentEntity>>

  @Query(
    "DELETE FROM attachment WHERE account = :account " +
        "AND account_type = :accountType AND folder = :label AND uid = :uid"
  )
  suspend fun deleteAttachments(account: String, accountType: String, label: String, uid: Long): Int
}
