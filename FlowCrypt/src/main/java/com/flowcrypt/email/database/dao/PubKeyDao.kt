/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.flowcrypt.email.database.entity.PublicKeyEntity
import kotlinx.coroutines.flow.Flow

/**
 * @author Denys Bondarenko
 */
@Dao
interface PubKeyDao : BaseDao<PublicKeyEntity> {
  @Query(SELECT_ALL_PUB_KEYS)
  suspend fun getAllPublicKeys(): List<PublicKeyEntity>

  @Query(SELECT_ALL_PUB_KEYS)
  fun getAllPublicKeysFlow(): Flow<List<PublicKeyEntity>>

  @Query("SELECT * FROM public_keys WHERE recipient = :recipient")
  suspend fun getPublicKeysByRecipient(recipient: String): List<PublicKeyEntity>

  @Query("SELECT * FROM public_keys WHERE recipient = :recipient")
  fun getPublicKeysByRecipientFlow(recipient: String): Flow<List<PublicKeyEntity>>

  @Query("SELECT * FROM public_keys WHERE fingerprint = :fingerprint")
  suspend fun getPublicKeysByFingerprint(fingerprint: String): List<PublicKeyEntity>

  @Query("SELECT * FROM public_keys WHERE recipient = :recipient AND fingerprint = :fingerprint")
  suspend fun getPublicKeyByRecipientAndFingerprint(
    recipient: String,
    fingerprint: String
  ): PublicKeyEntity?

  @Query("SELECT * FROM public_keys WHERE _id = :id")
  fun getPublicKeyByIdFlow(id: Long): Flow<PublicKeyEntity?>

  companion object {
    private const val SELECT_ALL_PUB_KEYS = "SELECT * FROM public_keys"
  }
}
