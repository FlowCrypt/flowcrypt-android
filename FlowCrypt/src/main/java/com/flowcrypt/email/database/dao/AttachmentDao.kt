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

  @Query("DELETE FROM attachment WHERE account = :account AND folder = :label")
  suspend fun deleteAtt(account: String?, label: String?): Int

  @Query("SELECT * FROM attachment WHERE account = :account AND folder = :label AND uid = :uid")
  fun getAttachments(account: String, label: String, uid: Long): List<AttachmentEntity>

  @Query("SELECT * FROM attachment WHERE account = :account AND folder = :label AND uid = :uid")
  suspend fun getAttachmentsSuspend(
    account: String,
    label: String,
    uid: Long
  ): List<AttachmentEntity>

  @Query("SELECT * FROM attachment WHERE account = :account AND folder = :label AND uid = :uid")
  fun getAttachmentsFlow(account: String, label: String, uid: Long): Flow<List<AttachmentEntity>>

  @Query("DELETE FROM attachment WHERE account = :account AND folder = :label AND uid = :uid")
  fun deleteAtt(account: String, label: String, uid: Long): Int

  @Query("DELETE FROM attachment WHERE account = :account AND folder = :label AND uid = :uid")
  suspend fun deleteAttSuspend(account: String, label: String, uid: Long): Int

  @Query("DELETE FROM attachment WHERE account = :account")
  suspend fun deleteByEmailSuspend(account: String?): Int
}
