/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.dao

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.UserIdEmailsKeysEntity
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.util.GeneralUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * This class describes available methods for [UserIdEmailsKeysEntity]
 *
 * @author Denis Bondarenko
 * Date: 30.07.2018
 * Time: 10:16
 * E-mail: DenBond7@gmail.com
 */
@Dao
interface UserIdEmailsKeysDao : BaseDao<UserIdEmailsKeysEntity> {
  @Query("SELECT * FROM user_id_emails_and_keys")
  fun getAllLD(): LiveData<List<UserIdEmailsKeysEntity>>

  @Query("SELECT * FROM user_id_emails_and_keys WHERE user_id_email IN (:users)")
  fun getUserIdEmailsKeysByEmailsLD(users: Collection<String>): LiveData<List<UserIdEmailsKeysEntity>>

  /**
   * Get a list of longId using a given email.
   *
   * @param email   An email which will be used for searching.
   * @return A list of found longId.
   */
  @Query("SELECT long_id FROM user_id_emails_and_keys WHERE user_id_email = :email")
  suspend fun getLongIdsByEmailSuspend(email: String?): List<String>

  /**
   * Get a list of longId using a given email.
   *
   * @param email   An email which will be used for searching.
   * @return A list of found longId.
   */
  @Query("SELECT long_id FROM user_id_emails_and_keys WHERE user_id_email = :email")
  fun getLongIdsByEmail(email: String?): List<String>

  @Query("SELECT long_id FROM user_id_emails_and_keys WHERE user_id_email = :account")
  fun getLongIdsByEmailLD(account: String): LiveData<List<String>>

  /**
   * Delete information about a private key by longid.
   *
   * @param longId The key longid.
   * @return The count of deleted rows. Will be 1 if information about the key was deleted or -1 otherwise.
   */
  @Query("DELETE FROM user_id_emails_and_keys WHERE long_id = :longId")
  fun deleteByLongId(longId: String): Int

  /**
   * Delete information about a private key by longid.
   *
   * @param longId The key longid.
   * @return The count of deleted rows. Will be 1 if information about the key was deleted or -1 otherwise.
   */
  @Query("DELETE FROM user_id_emails_and_keys WHERE long_id = :longId")
  suspend fun deleteByLongIdSuspend(longId: String): Int

  companion object {
    suspend fun genEntities(context: Context, keyDetails: NodeKeyDetails,
                            contacts: List<PgpContact>): List<UserIdEmailsKeysEntity> =
        withContext(Dispatchers.IO) {
          val contactsDao = FlowCryptRoomDatabase.getDatabase(context).contactsDao()
          val list = mutableListOf<UserIdEmailsKeysEntity>()
          for (pgpContact in contacts) {
            pgpContact.pubkey = keyDetails.publicKey
            val temp = contactsDao.getContactByEmailSuspend(pgpContact.email)
            if (GeneralUtil.isEmailValid(pgpContact.email) && temp == null) {
              contactsDao.insertWithReplaceSuspend(pgpContact.toContactEntity())
              //todo-DenBond7 Need to resolve a situation with different public keys. For example
              // we can have a situation when we have to different public keys with the same email
            }

            keyDetails.longId?.let {
              list.add(UserIdEmailsKeysEntity(longId = it,
                  userIdEmail = pgpContact.email.toLowerCase(Locale.getDefault())))
            }
          }
          return@withContext list
        }
  }
}
