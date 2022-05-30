/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import kotlinx.coroutines.flow.Flow

/**
 * This object describes a logic of work with [RecipientEntity].
 *
 * @author DenBond7
 * Date: 17.05.2017
 * Time: 12:22
 * E-mail: DenBond7@gmail.com
 */
@Dao
interface RecipientDao : BaseDao<RecipientEntity> {
  @Query("SELECT * FROM recipients")
  suspend fun getAllRecipients(): List<RecipientEntity>

  @Query("SELECT * FROM recipients")
  fun getAllRecipientsLD(): LiveData<List<RecipientEntity>>

  @Query(
    "SELECT recipients.* FROM recipients INNER JOIN public_keys " +
        "ON recipients.email = public_keys.recipient " +
        "GROUP BY recipients.email ORDER BY recipients._id"
  )
  fun getAllRecipientsWithPgpFlow(): Flow<List<RecipientEntity>>

  @Query(
    "SELECT recipients.* FROM recipients INNER JOIN public_keys " +
        "ON recipients.email = public_keys.recipient " +
        "GROUP BY recipients.email ORDER BY recipients._id"
  )
  suspend fun getAllRecipientsWithPgp(): List<RecipientEntity>

  @Query(
    "SELECT recipients.* FROM recipients INNER JOIN public_keys " +
        "ON recipients.email = public_keys.recipient " +
        "WHERE (email LIKE :searchPattern OR name LIKE :searchPattern) " +
        "GROUP BY recipients.email ORDER BY recipients._id"
  )
  suspend fun getAllRecipientsWithPgpWhichMatched(searchPattern: String): List<RecipientEntity>

  @Query("SELECT * FROM recipients WHERE email = :email")
  suspend fun getRecipientByEmailSuspend(email: String): RecipientEntity?

  @Query("SELECT * FROM recipients WHERE email = :email")
  fun getRecipientByEmail(email: String): RecipientEntity?

  @Query("SELECT * FROM recipients WHERE email IN (:emails)")
  fun getRecipientsByEmails(emails: Collection<String>): List<RecipientEntity>

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

  @Query("SELECT * FROM recipients WHERE email LIKE :searchPattern ORDER BY last_use DESC")
  fun getFilteredCursor(searchPattern: String): Cursor?

  @Query("DELETE FROM recipients")
  suspend fun deleteAll(): Int

  @Transaction
  @Query("SELECT * FROM recipients WHERE email = :email")
  suspend fun getRecipientWithPubKeysByEmailSuspend(email: String): RecipientWithPubKeys?

  @Transaction
  @Query("SELECT * FROM recipients WHERE email = :email")
  fun getRecipientWithPubKeysByEmail(email: String): RecipientWithPubKeys?
}
