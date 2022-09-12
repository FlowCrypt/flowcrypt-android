/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.flowcrypt.email.database.entity.DraftEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DraftDao : BaseDao<DraftEntity> {
  @Query("SELECT * FROM drafts WHERE account = :account AND account_type = :accountType AND draft_id = :draftId")
  suspend fun getDraftEntity(
    account: String? = null,
    accountType: String? = null,
    draftId: String? = null
  ): DraftEntity?

  @Query("SELECT * FROM drafts WHERE _id = :id")
  suspend fun getDraftEntityById(id: String): DraftEntity?

  @Query("SELECT * FROM drafts WHERE account = :account AND account_type = :accountType AND draft_id = :draftId")
  fun getDraftEntityFlow(
    account: String? = null,
    accountType: String? = null,
    draftId: String? = null
  ): Flow<DraftEntity?>
}
