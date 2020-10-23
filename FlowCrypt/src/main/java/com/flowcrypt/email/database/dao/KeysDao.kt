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
  @Query("DELETE FROM keys WHERE long_id = :longId")
  abstract fun deleteByLongId(longId: String): Int

  @Query("DELETE FROM keys WHERE long_id = :longId")
  abstract suspend fun deleteByLongIdSuspend(longId: String): Int

  @Query("SELECT * FROM keys")
  abstract fun getAllKeys(): List<KeyEntity>

  @Query("SELECT * FROM keys")
  abstract fun getAllKeysLD(): LiveData<List<KeyEntity>>

  @Query("SELECT * FROM keys WHERE account = :account")
  abstract fun getAllKeysByAccount(account: String): List<KeyEntity>

  @Query("SELECT * FROM keys WHERE account = :account")
  abstract fun getAllKeysByAccountLD(account: String): LiveData<List<KeyEntity>>

  @Query("SELECT * FROM keys WHERE long_id = :longId")
  abstract suspend fun getKeyByLongIdSuspend(longId: String): KeyEntity?

  @Query("SELECT * FROM keys WHERE long_id = :longId AND account = :account")
  abstract suspend fun getKeyByLongIdAndAccountSuspend(longId: String, account: String): KeyEntity?
}
