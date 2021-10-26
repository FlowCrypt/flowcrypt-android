/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.ApiRepository
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.api.retrofit.response.attester.PubResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.ApiException
import com.flowcrypt.email.util.exception.ExceptionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

/**
 * This is used in the message compose/reply view when recipient public keys need to be retrieved,
 * either from local storage or from remote servers eg Attester or WKD, based on client
 * configuration.
 *
 * @author Denis Bondarenko
 *         Date: 4/7/20
 *         Time: 11:19 AM
 *         E-mail: DenBond7@gmail.com
 */
class RecipientsViewModel(application: Application) : AccountViewModel(application) {
  private val apiRepository: ApiRepository = FlowcryptApiRepository()
  private val searchPatternLiveData: MutableLiveData<String> = MutableLiveData()

  val allContactsLiveData: LiveData<List<RecipientEntity>> =
    roomDatabase.recipientDao().getAllRecipientsLD()
  val contactsWithPgpLiveData: LiveData<Result<List<RecipientEntity>>> =
    Transformations.switchMap(roomDatabase.recipientDao().getAllRecipientsWithPgpLD()) {
      liveData {
        emit(Result.success(it))
      }
    }
  val contactsWithPgpSearchLiveData: LiveData<Result<List<RecipientEntity>>> =
    Transformations.switchMap(searchPatternLiveData) {
      liveData {
        emit(Result.loading())
        val foundContacts = if (it.isNullOrEmpty()) {
          roomDatabase.recipientDao().getAllRecipientsWithPgp()
        } else {
          roomDatabase.recipientDao().getAllRecipientsWithPgpWhichMatched("%$it%")
        }
        emit(Result.success(foundContacts))
      }
    }
  val contactsToLiveData: MutableLiveData<Result<List<PgpContact>>> = MutableLiveData()
  val contactsCcLiveData: MutableLiveData<Result<List<PgpContact>>> = MutableLiveData()
  val contactsBccLiveData: MutableLiveData<Result<List<PgpContact>>> = MutableLiveData()

  val pubKeysFromServerLiveData: MutableLiveData<Result<PubResponse?>> = MutableLiveData()

  fun copyPubKeysToRecipient(pgpContact: PgpContact, pgpContactFromKey: PgpContact) {
    viewModelScope.launch {
      val recipient = roomDatabase.recipientDao().getRecipientByEmailSuspend(pgpContact.email)
      if (recipient != null) {
        val updateCandidate = pgpContact.toRecipientEntity().copy(id = recipient.id)
        roomDatabase.recipientDao().updateSuspend(updateCandidate)
      }

      if (!pgpContact.email.equals(pgpContactFromKey.email, ignoreCase = true)) {
        val existedContact =
          roomDatabase.recipientDao().getRecipientByEmailSuspend(pgpContactFromKey.email)
        if (existedContact == null) {
          roomDatabase.recipientDao().insertSuspend(pgpContactFromKey.toRecipientEntity())
        }
      }
    }
  }

  fun copyPubKeysToRecipient(email: String, recipientEntity: RecipientEntity) {
    viewModelScope.launch {
      val pubKeysOfCopyCandidate =
        roomDatabase.pubKeysDao().getPublicKeysByRecipient(recipientEntity.email)
      if (pubKeysOfCopyCandidate.isEmpty()) return@launch
      roomDatabase.pubKeysDao().insertSuspend(
        pubKeysOfCopyCandidate.map { it.copy(id = null, recipient = email) }
      )
    }
  }

  fun contactChangesLiveData(recipientEntity: RecipientEntity): LiveData<RecipientEntity?> {
    return roomDatabase.recipientDao().getRecipientByEmailLD(recipientEntity.email)
  }

  /**
   * Here we do the following things:
   *
   *  a) if there is a record for that email and has_pgp==true, do `flowcrypt.com/attester/pub/<FINGERPRINT>` API
   *  call to see if you can now get the fresher pubkey. If we successfully load a key, we
   *  compare date of last signature on the key we have and on the key we received.
   *  If the key from attester has a newer signature on it, then it's more recent, and we will automatically replace the local version
   *  b) if there is a record but `has_pgp==false`, do `flowcrypt.com/attester/pub/<EMAIL>` API
   *  call
   * to see if you can now get the pubkey. If a pubkey is available, save it back to the database.
   *  c) no record in the db found:
   *  1. save an empty record eg `new PgpContact(email, null);` - this means we don't know if they have PGP yet
   *  1. look up the email on `flowcrypt.com/attester/pub/EMAIL>`
   *  1. if pubkey comes back, create something like `new PgpContact(js, email, null, pubkey,
   * client);`. The PgpContact constructor will define has_pgp, fingerprint, etc
   * for you. Then save that object into database.
   *  1. if no pubkey found, create `new PgpContact(js, email, null, null, null, null);` - this
   * means we know they don't currently have PGP
   */
  fun fetchAndUpdateInfoAboutContacts(type: RecipientEntity.Type, emails: List<String>) {
    viewModelScope.launch {
      if (emails.isEmpty()) {
        return@launch
      }

      setResultForRemoteContactsLiveData(type, Result.loading())

      val pgpContacts = ArrayList<PgpContact>()
      try {
        for (email in emails) {
          if (GeneralUtil.isEmailValid(email)) {
            val emailLowerCase = email.lowercase(Locale.getDefault())
            var cachedRecipientEntity =
              roomDatabase.recipientDao().getRecipientByEmailSuspend(emailLowerCase)

            if (cachedRecipientEntity == null) {
              cachedRecipientEntity = PgpContact(emailLowerCase, null).toRecipientEntity()
              roomDatabase.recipientDao().insertSuspend(cachedRecipientEntity)
              cachedRecipientEntity =
                roomDatabase.recipientDao().getRecipientByEmailSuspend(emailLowerCase)
            } else {
              try {
                /*cachedRecipientEntity.publicKey?.let {
                  val result = PgpKey.parseKeys(it).pgpKeyDetailsList
                  cachedRecipientEntity?.pgpKeyDetails = result.firstOrNull()
                }*/
              } catch (e: Exception) {
                e.printStackTrace()
                pgpContacts.add(
                  cachedRecipientEntity.toPgpContact().copy(hasNotUsablePubKey = true)
                )
                continue
              }
            }

            try {
              if (true) {
                getPgpContactInfoFromServer(email = emailLowerCase)?.let {
                  cachedRecipientEntity =
                    updateCachedInfoWithAttesterInfo(cachedRecipientEntity, it, emailLowerCase)
                }
              } else {
                cachedRecipientEntity?.pgpKeyDetails?.fingerprint?.let { fingerprint ->
                  getPgpContactInfoFromServer(fingerprint = fingerprint)?.let {
                    val cacheLastModified = cachedRecipientEntity?.pgpKeyDetails?.lastModified ?: 0
                    val attesterLastModified = it.pgpKeyDetails?.lastModified ?: 0
                    val attesterFingerprint = it.pgpKeyDetails?.fingerprint

                    if (attesterLastModified > cacheLastModified && fingerprint.equals(
                        attesterFingerprint,
                        true
                      )
                    ) {
                      cachedRecipientEntity =
                        updateCachedInfoWithAttesterInfo(cachedRecipientEntity, it, emailLowerCase)
                    }
                  }
                }
              }

              cachedRecipientEntity?.let { pgpContacts.add(it.toPgpContact()) }
            } catch (e: Exception) {
              e.printStackTrace()
              ExceptionUtil.handleError(e)
            }
          }
        }
        setResultForRemoteContactsLiveData(type, Result.success(pgpContacts))
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        setResultForRemoteContactsLiveData(type, Result.exception(e))
      }
    }
  }

  private suspend fun updateCachedInfoWithAttesterInfo(
    cachedRecipientEntity: RecipientEntity?,
    attesterPgpContact: PgpContact, emailLowerCase: String
  ): RecipientEntity? {
    cachedRecipientEntity ?: return null
    val updateCandidate = if (
      cachedRecipientEntity.name.isNullOrEmpty()
      && cachedRecipientEntity.email.equals(attesterPgpContact.email, ignoreCase = true)
    ) {
      attesterPgpContact.toRecipientEntity().copy(
        id = cachedRecipientEntity.id,
        email = cachedRecipientEntity.email
      )
    } else {
      attesterPgpContact.toRecipientEntity().copy(
        id = cachedRecipientEntity.id,
        name = cachedRecipientEntity.name,
        email = cachedRecipientEntity.email
      )
    }

    roomDatabase.recipientDao().updateSuspend(updateCandidate)
    val lastVersion = roomDatabase.recipientDao().getRecipientByEmailSuspend(emailLowerCase)

    /*lastVersion?.publicKey?.let {
      val result = PgpKey.parseKeys(it).pgpKeyDetailsList
      lastVersion.pgpKeyDetails = result.firstOrNull()
    }*/

    return lastVersion
  }

  fun deleteContact(recipientEntity: RecipientEntity) {
    viewModelScope.launch {
      roomDatabase.recipientDao().deleteSuspend(recipientEntity)
    }
  }

  fun addContact(pgpContact: PgpContact) {
    viewModelScope.launch {
      val contact = roomDatabase.recipientDao().getRecipientByEmailSuspend(pgpContact.email)
      if (contact == null) {
        val isInserted =
          roomDatabase.recipientDao().insertSuspend(pgpContact.toRecipientEntity()) > 0
        if (isInserted) {
          roomDatabase.pubKeysDao().insertSuspend(pgpContact.toPubKey())
        } else {
          val context: Context = getApplication()
          Toast.makeText(
            context,
            context.getString(R.string.could_not_save_new_recipient), Toast.LENGTH_LONG
          ).show()
        }
      }
    }
  }

  fun updateContact(pgpContact: PgpContact) {
    viewModelScope.launch {
      val contact = roomDatabase.recipientDao().getRecipientByEmailSuspend(pgpContact.email)
      if (contact != null) {
        val updateCandidate = pgpContact.toRecipientEntity().copy(id = contact.id)
        roomDatabase.recipientDao().updateSuspend(updateCandidate)
      }
    }
  }

  fun copyPubKeysToRecipient(recipientEntity: RecipientEntity?, pgpKeyDetails: PgpKeyDetails) {
    viewModelScope.launch {
      recipientEntity?.let {
        val recipientEntityFromPrimaryPgpContact =
          pgpKeyDetails.primaryPgpContact.toRecipientEntity()
        roomDatabase.recipientDao().updateSuspend(
          recipientEntityFromPrimaryPgpContact.copy(
            id = recipientEntity.id,
            email = recipientEntity.email.lowercase(Locale.US),
          )
        )
      }
    }
  }

  fun filterContacts(searchPattern: String?) {
    searchPatternLiveData.value = searchPattern
  }

  fun deleteContactByEmail(email: String) {
    viewModelScope.launch {
      roomDatabase.recipientDao().getRecipientByEmailSuspend(email)?.let {
        roomDatabase.recipientDao().deleteSuspend(it)
      }
    }
  }

  fun fetchPubKeys(keyIdOrEmail: String, requestCode: Long) {
    viewModelScope.launch {
      pubKeysFromServerLiveData.value = Result.loading(requestCode = requestCode)
      val activeAccount = getActiveAccountSuspend()
      pubKeysFromServerLiveData.value = apiRepository.pubLookup(
        requestCode = requestCode,
        context = getApplication(),
        identData = keyIdOrEmail,
        orgRules = activeAccount?.clientConfiguration
      )
    }
  }

  private fun setResultForRemoteContactsLiveData(
    type: RecipientEntity.Type,
    result: Result<List<PgpContact>>
  ) {
    when (type) {
      RecipientEntity.Type.TO -> {
        contactsToLiveData.value = result
      }

      RecipientEntity.Type.CC -> {
        contactsCcLiveData.value = result
      }

      RecipientEntity.Type.BCC -> {
        contactsBccLiveData.value = result
      }
    }
  }

  /**
   * Get information about [PgpContact] from the remote server.
   *
   * @param email Used to generate a request to the server.
   * @return [PgpContact]
   * @throws IOException
   */
  private suspend fun getPgpContactInfoFromServer(
    email: String? = null,
    fingerprint: String? = null
  ): PgpContact? = withContext(Dispatchers.IO) {
    try {
      val activeAccount = getActiveAccountSuspend()
      val response = apiRepository.pubLookup(
        context = getApplication(),
        identData = email ?: fingerprint ?: "",
        orgRules = activeAccount?.clientConfiguration
      )

      when (response.status) {
        Result.Status.SUCCESS -> {
          val pubKeyString = response.data?.pubkey
          if (pubKeyString?.isNotEmpty() == true) {
            PgpKey.parseKeys(pubKeyString).pgpKeyDetailsList.firstOrNull()?.let {
              val pgpContact = it.primaryPgpContact
              pgpContact.pgpKeyDetails = it
              return@withContext pgpContact
            }
          }
        }

        Result.Status.ERROR -> {
          throw ApiException(
            response.data?.apiError
              ?: ApiError(code = -1, msg = "Unknown API error")
          )
        }

        else -> {
          throw response.exception ?: java.lang.Exception()
        }
      }
    } catch (e: IOException) {
      e.printStackTrace()
    }

    null
  }
}
