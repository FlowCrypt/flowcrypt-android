/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.flowcrypt.email.database.entity.RecipientEntity

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
  @Query("SELECT * FROM contacts")
  suspend fun getAllContacts(): List<RecipientEntity>

  //fixed
  @Query("SELECT * FROM contacts")
  fun getAllContactsLD(): LiveData<List<RecipientEntity>>

  //fixed
  @Query("SELECT contacts.* FROM contacts INNER JOIN public_keys ON contacts.email = public_keys.recipient GROUP BY contacts.email ORDER BY contacts._id")
  fun getAllContactsWithPgpLD(): LiveData<List<RecipientEntity>>

  //fixed
  @Query("SELECT contacts.* FROM contacts INNER JOIN public_keys ON contacts.email = public_keys.recipient GROUP BY contacts.email ORDER BY contacts._id")
  suspend fun getAllContactsWithPgp(): List<RecipientEntity>

  //fixed
  @Query("SELECT contacts.* FROM contacts INNER JOIN public_keys ON contacts.email = public_keys.recipient WHERE (email LIKE :searchPattern OR name LIKE :searchPattern) GROUP BY contacts.email ORDER BY contacts._id")
  suspend fun getAllContactsWithPgpWhichMatched(searchPattern: String): List<RecipientEntity>

  @Query("SELECT * FROM contacts WHERE email = :email")
  suspend fun getContactByEmailSuspend(email: String): RecipientEntity?

  @Query("SELECT * FROM contacts WHERE email = :email")
  fun getContactByEmail(email: String): RecipientEntity?

  @Query("SELECT * FROM contacts WHERE email = :email")
  fun getContactByEmailLD(email: String): LiveData<RecipientEntity?>

  @Query("SELECT * FROM contacts WHERE email IN (:emails)")
  fun getContactsByEmails(emails: Collection<String>): List<RecipientEntity>

  @Query("SELECT * FROM contacts WHERE email LIKE :searchPattern ORDER BY last_use DESC")
  fun getFilteredCursor(searchPattern: String): Cursor?

  @Query("DELETE FROM contacts")
  suspend fun deleteAll(): Int
}
