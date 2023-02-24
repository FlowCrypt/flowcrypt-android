/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.flowcrypt.email.database.entity.AttachmentEntity

/**
 * @author Denys Bondarenko
 */
@Dao
interface AttachmentDao : BaseDao<AttachmentEntity> {

  @Query("DELETE FROM attachment WHERE email = :email AND folder = :label")
  suspend fun deleteAtt(email: String?, label: String?): Int

  @Query("SELECT * FROM attachment WHERE email = :account AND folder = :label AND uid = :uid")
  fun getAttachments(account: String, label: String, uid: Long): List<AttachmentEntity>

  @Query("SELECT * FROM attachment WHERE email = :account AND folder = :label AND uid = :uid")
  suspend fun getAttachmentsSuspend(
    account: String,
    label: String,
    uid: Long
  ): List<AttachmentEntity>

  @Query("SELECT * FROM attachment WHERE email = :account AND folder = :label AND uid = :uid")
  fun getAttachmentsLD(account: String, label: String, uid: Long): LiveData<List<AttachmentEntity>>

  @Query("DELETE FROM attachment WHERE email = :account AND folder = :label AND uid = :uid")
  fun deleteAtt(account: String, label: String, uid: Long): Int

  @Query("DELETE FROM attachment WHERE email = :account AND folder = :label AND uid = :uid")
  suspend fun deleteAttSuspend(account: String, label: String, uid: Long): Int

  @Query("DELETE FROM attachment WHERE email = :email")
  suspend fun deleteByEmailSuspend(email: String?): Int
}
