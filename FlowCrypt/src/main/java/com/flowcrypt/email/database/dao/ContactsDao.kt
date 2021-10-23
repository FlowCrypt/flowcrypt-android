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
import com.flowcrypt.email.database.entity.ContactEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys

/**
 * This object describes a logic of work with [ContactEntity].
 *
 * @author DenBond7
 * Date: 17.05.2017
 * Time: 12:22
 * E-mail: DenBond7@gmail.com
 */
@Dao
interface ContactsDao : BaseDao<ContactEntity> {
  //fixed
  @Query("SELECT * FROM contacts")
  suspend fun getAllContacts(): List<ContactEntity>

  //fixed
  @Query("SELECT * FROM contacts")
  fun getAllContactsLD(): LiveData<List<ContactEntity>>

  @Transaction
  @Query("SELECT * FROM contacts WHERE email IN (SELECT recipient FROM public_keys GROUP BY recipient)")
  fun getAllContactsWithPgpLD1(): LiveData<List<RecipientWithPubKeys>>

  @Query("SELECT * FROM contacts")
  //@Query("SELECT * FROM contacts WHERE has_pgp = 1")
  fun getAllContactsWithPgpLD(): LiveData<List<ContactEntity>>

  @Query("SELECT * FROM contacts")
  //@Query("SELECT * FROM contacts WHERE has_pgp = 1")
  suspend fun getAllContactsWithPgp(): List<ContactEntity>

  @Query("SELECT * FROM contacts WHERE (email LIKE :searchPattern OR name LIKE :searchPattern)")
  //@Query("SELECT * FROM contacts WHERE has_pgp = 1 AND (email LIKE :searchPattern OR name LIKE :searchPattern)")
  suspend fun getAllContactsWithPgpWhichMatched(searchPattern: String): List<ContactEntity>

  @Query("SELECT * FROM contacts WHERE email = :email")
  suspend fun getContactByEmailSuspend(email: String): ContactEntity?

  @Query("SELECT * FROM contacts WHERE email = :email")
  fun getContactByEmail(email: String): ContactEntity?

  @Query("SELECT * FROM contacts WHERE email = :email")
  fun getContactByEmailLD(email: String): LiveData<ContactEntity?>

  @Query("SELECT * FROM contacts WHERE email IN (:emails)")
  fun getContactsByEmails(emails: Collection<String>): List<ContactEntity>

  @Query("SELECT * FROM contacts WHERE email LIKE :searchPattern ORDER BY last_use DESC")
  fun getFilteredCursor(searchPattern: String): Cursor?

  @Query("DELETE FROM contacts")
  suspend fun deleteAll(): Int
}
