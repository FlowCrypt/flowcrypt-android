/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.flowcrypt.email.database.entity.KeyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @author Denys Bondarenko
 */
@Dao
abstract class KeysDao {
  @Query("SELECT * FROM keys WHERE account = :account")
  abstract fun getAllKeysByAccount(account: String): List<KeyEntity>

  @Query("SELECT * FROM keys WHERE account = :account")
  abstract suspend fun getAllKeysByAccountSuspend(account: String): List<KeyEntity>

  @Query("SELECT * FROM keys WHERE account = :account")
  abstract fun getAllKeysByAccountLD(account: String): LiveData<List<KeyEntity>>

  @Query("DELETE FROM keys WHERE account = :account AND fingerprint = :fingerprint")
  abstract fun deleteByAccountAndFingerprint(account: String, fingerprint: String): Int

  @Query("DELETE FROM keys WHERE account = :account AND fingerprint = :fingerprint")
  abstract suspend fun deleteByAccountAndFingerprintSuspend(
    account: String, fingerprint: String
  ): Int

  @Query("SELECT * FROM keys WHERE account = :account AND fingerprint = :fingerprint")
  abstract suspend fun getKeyByAccountAndFingerprintSuspend(
    account: String, fingerprint: String
  ): KeyEntity?

  @Query("SELECT * FROM keys WHERE account = :account AND fingerprint = :fingerprint")
  abstract fun getKeyByAccountAndFingerprint(account: String, fingerprint: String): KeyEntity?

  @Insert
  abstract fun insertInternal(entity: KeyEntity): Long

  @Insert
  abstract suspend fun insertInternalSuspend(entity: KeyEntity): Long

  @Insert
  abstract suspend fun insertInternalSuspend(entities: Iterable<KeyEntity>)

  @Update
  abstract fun updateInternal(entities: Iterable<KeyEntity>): Int

  @Update
  abstract suspend fun updateInternalSuspend(entity: KeyEntity): Int

  @Update
  abstract suspend fun updateInternalSuspend(entities: Iterable<KeyEntity>): Int

  @Delete
  abstract suspend fun deleteSuspend(entities: Iterable<KeyEntity>): Int

  @Delete
  abstract fun delete(entity: KeyEntity): Int

  @Insert
  open fun insert(keyEntity: KeyEntity): Long {
    return insertInternal(processKeyEntity(keyEntity))
  }

  @Update
  open fun update(entities: Iterable<KeyEntity>): Int {
    return updateInternal(entities.map { processKeyEntity(it) })
  }

  @Insert
  open suspend fun insertSuspend(keyEntity: KeyEntity): Long =
    withContext(Dispatchers.IO) { insertInternalSuspend(processKeyEntity(keyEntity)) }

  @Insert
  open suspend fun insertSuspend(entities: Iterable<KeyEntity>) =
    withContext(Dispatchers.IO) { insertInternalSuspend(entities.map { processKeyEntity(it) }) }

  @Update
  open suspend fun updateSuspend(keyEntity: KeyEntity): Int =
    withContext(Dispatchers.IO) { updateInternalSuspend(processKeyEntity(keyEntity)) }

  @Update
  open suspend fun updateSuspend(entities: Iterable<KeyEntity>): Int =
    withContext(Dispatchers.IO) { updateInternalSuspend(entities.map { processKeyEntity(it) }) }

  private fun processKeyEntity(keyEntity: KeyEntity) =
    keyEntity.copy(
      storedPassphrase = if (keyEntity.passphraseType == KeyEntity.PassphraseType.RAM) {
        null
      } else {
        keyEntity.storedPassphrase
      }
    )
}
