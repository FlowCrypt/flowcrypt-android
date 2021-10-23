/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.flowcrypt.email.database.entity.PublicKeyEntity

/**
 * @author Denis Bondarenko
 *         Date: 10/20/21
 *         Time: 4:58 PM
 *         E-mail: DenBond7@gmail.com
 */
@Dao
interface PubKeysDao : BaseDao<PublicKeyEntity> {
  @Query("SELECT * FROM public_keys")
  suspend fun getAllPublicKeys(): List<PublicKeyEntity>

  @Query("SELECT * FROM public_keys")
  fun getAllPublicKeysLD(): LiveData<List<PublicKeyEntity>>

  @Query("SELECT * FROM public_keys WHERE recipient = :recipient")
  suspend fun getPublicKeysByRecipient(recipient: String): List<PublicKeyEntity>

  @Query("SELECT * FROM public_keys WHERE fingerprint = :fingerprint")
  suspend fun getPublicKeysByFingerprint(fingerprint: String): List<PublicKeyEntity>
}
