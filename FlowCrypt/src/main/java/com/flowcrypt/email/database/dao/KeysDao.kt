/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.flowcrypt.email.database.entity.KeyEntity

/**
 * @author DenBond7
 * Date: 13.05.2017
 * Time: 12:44
 * E-mail: DenBond7@gmail.com
 */
@Dao
abstract class KeysDao : BaseDao<KeyEntity> {
  @Query("SELECT * FROM keys WHERE account = :account")
  abstract fun getAllKeysByAccount(account: String): List<KeyEntity>

  @Query("SELECT * FROM keys WHERE account = :account")
  abstract suspend fun getAllKeysByAccountSuspend(account: String): List<KeyEntity>

  @Query("SELECT * FROM keys WHERE account = :account")
  abstract fun getAllKeysByAccountLD(account: String): LiveData<List<KeyEntity>>

  @Query("DELETE FROM keys WHERE account = :account AND long_id = :longId")
  abstract fun deleteByAccountAndLongId(account: String, longId: String): Int

  @Query("DELETE FROM keys WHERE account = :account AND long_id = :longId")
  abstract suspend fun deleteByAccountAndLongIdSuspend(account: String, longId: String): Int

  @Query("SELECT * FROM keys WHERE account = :account AND long_id = :longId")
  abstract suspend fun getKeyByAccountAndLongIdSuspend(account: String, longId: String): KeyEntity?
}
