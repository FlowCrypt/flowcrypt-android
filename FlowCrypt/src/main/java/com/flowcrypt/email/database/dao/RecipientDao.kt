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
  //fixed
  @Query("SELECT * FROM recipients")
  suspend fun getAllRecipients(): List<RecipientEntity>

  //fixed
  @Query("SELECT * FROM recipients")
  fun getAllRecipientsLD(): LiveData<List<RecipientEntity>>

  //fixed
  @Query("SELECT recipients.* FROM recipients INNER JOIN public_keys ON recipients.email = public_keys.recipient GROUP BY recipients.email ORDER BY recipients._id")
  fun getAllRecipientsWithPgpLD(): LiveData<List<RecipientEntity>>

  //fixed
  @Query("SELECT recipients.* FROM recipients INNER JOIN public_keys ON recipients.email = public_keys.recipient GROUP BY recipients.email ORDER BY recipients._id")
  suspend fun getAllRecipientsWithPgp(): List<RecipientEntity>

  //fixed
  @Query("SELECT recipients.* FROM recipients INNER JOIN public_keys ON recipients.email = public_keys.recipient WHERE (email LIKE :searchPattern OR name LIKE :searchPattern) GROUP BY recipients.email ORDER BY recipients._id")
  suspend fun getAllRecipientsWithPgpWhichMatched(searchPattern: String): List<RecipientEntity>

  @Query("SELECT * FROM recipients WHERE email = :email")
  suspend fun getRecipientByEmailSuspend(email: String): RecipientEntity?

  //fixed
  @Query("SELECT * FROM recipients WHERE email = :email")
  fun getRecipientByEmail(email: String): RecipientEntity?

  @Query("SELECT * FROM recipients WHERE email = :email")
  fun getRecipientByEmailLD(email: String): LiveData<RecipientEntity?>

  //fixed
  @Transaction
  @Query("SELECT * FROM recipients WHERE email IN (:emails)")
  fun getRecipientsWithPubKeysByEmails(emails: Collection<String>): List<RecipientWithPubKeys>

  //fixed
  @Query("SELECT * FROM recipients WHERE email LIKE :searchPattern ORDER BY last_use DESC")
  fun getFilteredCursor(searchPattern: String): Cursor?

  //fixed
  @Query("DELETE FROM recipients")
  suspend fun deleteAll(): Int

  @Transaction
  @Query("SELECT * FROM recipients WHERE email = :email")
  fun getRecipientWithPubKeysByEmail(email: String): RecipientWithPubKeys?
}
