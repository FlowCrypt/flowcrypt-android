/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys

/**
 * This object describes a logic of work with [RecipientEntity].
 *
 * @author Denys Bondarenko
 */
@Dao
interface RecipientDao : BaseDao<RecipientEntity> {
  @Query("SELECT * FROM recipients")
  suspend fun getAllRecipients(): List<RecipientEntity>

  @Query("SELECT * FROM recipients")
  fun getAllRecipientsLD(): LiveData<List<RecipientEntity>>

  @Query(
    "SELECT recipients.*, public_keys.fingerprint IS NOT NULL AS has_pgp FROM recipients INNER JOIN public_keys " +
        "ON recipients.email = public_keys.recipient " +
        "WHERE (email LIKE :searchPattern OR name LIKE :searchPattern) " +
        "GROUP BY recipients.email ORDER BY recipients._id"
  )
  suspend fun getAllMatchedRecipientsWithPgpAndPgpMarker(searchPattern: String): List<RecipientEntity.WithPgpMarker>

  @Query(
    "SELECT recipients.*, public_keys.fingerprint IS NOT NULL AS has_pgp FROM recipients LEFT JOIN public_keys " +
        "ON recipients.email = public_keys.recipient " +
        "WHERE (email LIKE :searchPattern OR name LIKE :searchPattern) " +
        "GROUP BY recipients.email ORDER BY recipients._id"
  )
  suspend fun getAllMatchedRecipientsWithPgpMarker(searchPattern: String): List<RecipientEntity.WithPgpMarker>

  @Query("SELECT * FROM recipients WHERE email = :email")
  suspend fun getRecipientByEmailSuspend(email: String): RecipientEntity?

  @Query("SELECT * FROM recipients WHERE email = :email")
  fun getRecipientByEmail(email: String): RecipientEntity?

  @Query("SELECT * FROM recipients WHERE email IN (:emails)")
  suspend fun getRecipientsByEmails(emails: Collection<String>): List<RecipientEntity>

  @Transaction
  @Query("SELECT * FROM recipients WHERE email = :email")
  fun getRecipientsWithPubKeysByEmailsLD(email: String): LiveData<RecipientWithPubKeys?>

  @Transaction
  @Query("SELECT * FROM recipients WHERE email IN (:emails)")
  fun getRecipientsWithPubKeysByEmails(emails: Collection<String>): List<RecipientWithPubKeys>

  @Transaction
  @Query("SELECT * FROM recipients WHERE email IN (:emails)")
  suspend fun getRecipientsWithPubKeysByEmailsSuspend(emails: Collection<String>):
      List<RecipientWithPubKeys>

  @Transaction
  @Query("SELECT * FROM recipients WHERE email LIKE :searchPattern ORDER BY last_use DESC")
  suspend fun findMatchingRecipients(searchPattern: String): List<RecipientWithPubKeys>

  @Query("DELETE FROM recipients")
  suspend fun deleteAll(): Int

  @Transaction
  @Query("SELECT * FROM recipients WHERE email = :email")
  suspend fun getRecipientWithPubKeysByEmailSuspend(email: String): RecipientWithPubKeys?

  @Transaction
  @Query("SELECT * FROM recipients WHERE email = :email")
  fun getRecipientWithPubKeysByEmail(email: String): RecipientWithPubKeys?
}
