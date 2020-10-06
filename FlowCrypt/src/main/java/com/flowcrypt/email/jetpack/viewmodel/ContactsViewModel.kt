/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.retrofit.ApiRepository
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.api.retrofit.node.NodeRepository
import com.flowcrypt.email.api.retrofit.request.node.ParseKeysRequest
import com.flowcrypt.email.api.retrofit.response.attester.PubResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.entity.ContactEntity
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.ApiException
import com.flowcrypt.email.util.exception.ExceptionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

/**
 * @author Denis Bondarenko
 *         Date: 4/7/20
 *         Time: 11:19 AM
 *         E-mail: DenBond7@gmail.com
 */
class ContactsViewModel(application: Application) : AccountViewModel(application) {
  private val apiRepository: ApiRepository = FlowcryptApiRepository()
  private val pgpApiRepository = NodeRepository()
  private val searchPatternLiveData: MutableLiveData<String> = MutableLiveData()

  val allContactsLiveData: LiveData<List<ContactEntity>> = roomDatabase.contactsDao().getAllContactsLD()
  val contactsWithPgpLiveData: LiveData<Result<List<ContactEntity>>> =
      Transformations.switchMap(roomDatabase.contactsDao().getAllContactsWithPgpLD()) {
        liveData {
          emit(Result.success(it))
        }
      }
  val contactsWithPgpSearchLiveData: LiveData<Result<List<ContactEntity>>> =
      Transformations.switchMap(searchPatternLiveData) {
        liveData {
          emit(Result.loading())
          val foundContacts = if (it.isNullOrEmpty()) {
            roomDatabase.contactsDao().getAllContactsWithPgp()
          } else {
            roomDatabase.contactsDao().getAllContactsWithPgpWhichMatched("%$it%")
          }
          emit(Result.success(foundContacts))
        }
      }
  val contactsToLiveData: MutableLiveData<Result<List<ContactEntity>>> = MutableLiveData()
  val contactsCcLiveData: MutableLiveData<Result<List<ContactEntity>>> = MutableLiveData()
  val contactsBccLiveData: MutableLiveData<Result<List<ContactEntity>>> = MutableLiveData()

  val pubKeysFromAttesterLiveData: MutableLiveData<Result<PubResponse?>> = MutableLiveData()

  fun updateContactPgpInfo(pgpContact: PgpContact, pgpContactFromKey: PgpContact) {
    viewModelScope.launch {
      val contact = roomDatabase.contactsDao().getContactByEmailSuspend(pgpContact.email)
      if (contact != null) {
        val updateCandidate = pgpContact.toContactEntity().copy(id = contact.id)
        roomDatabase.contactsDao().updateSuspend(updateCandidate)
      }

      if (!pgpContact.email.equals(pgpContactFromKey.email, ignoreCase = true)) {
        val existedContact = roomDatabase.contactsDao().getContactByEmailSuspend(pgpContactFromKey.email)
        if (existedContact == null) {
          roomDatabase.contactsDao().insertSuspend(pgpContactFromKey.toContactEntity())
        }
      }
    }
  }

  fun updateContactPgpInfo(email: String, contactEntity: ContactEntity) {
    viewModelScope.launch {
      val originalContactEntity = roomDatabase.contactsDao().getContactByEmailSuspend(email)
          ?: return@launch
      roomDatabase.contactsDao().updateSuspend(
          originalContactEntity.copy(
              publicKey = contactEntity.publicKey,
              fingerprint = contactEntity.fingerprint,
              longId = contactEntity.longId,
              keywords = contactEntity.keywords,
              hasPgp = true))
    }
  }

  fun contactChangesLiveData(contactEntity: ContactEntity): LiveData<ContactEntity?> {
    return roomDatabase.contactsDao().getContactByEmailLD(contactEntity.email)
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
   * client);`. The PgpContact constructor will define has_pgp, longid, fingerprint, etc
   * for you. Then save that object into database.
   *  1. if no pubkey found, create `new PgpContact(js, email, null, null, null, null);` - this
   * means we know they don't currently have PGP
   */
  fun fetchAndUpdateInfoAboutContacts(type: ContactEntity.Type, emails: List<String>) {
    viewModelScope.launch {
      if (emails.isEmpty()) {
        return@launch
      }

      setResultForRemoteContactsLiveData(type, Result.loading())

      val pgpContacts = ArrayList<ContactEntity>()
      try {
        for (email in emails) {
          if (GeneralUtil.isEmailValid(email)) {
            val emailLowerCase = email.toLowerCase(Locale.getDefault())
            var cachedContactEntity = roomDatabase.contactsDao().getContactByEmailSuspend(emailLowerCase)

            if (cachedContactEntity == null) {
              cachedContactEntity = PgpContact(emailLowerCase, null).toContactEntity()
              roomDatabase.contactsDao().insertSuspend(cachedContactEntity)
              cachedContactEntity = roomDatabase.contactsDao().getContactByEmailSuspend(emailLowerCase)
            } else {
              cachedContactEntity.publicKey?.let {
                val result = pgpApiRepository.fetchKeyDetails(ParseKeysRequest(String(it)))
                cachedContactEntity?.nodeKeyDetails = result.data?.nodeKeyDetails?.firstOrNull()
              }
            }

            try {
              if (cachedContactEntity?.hasPgp == false) {
                getPgpContactInfoFromServer(email = emailLowerCase)?.let {
                  cachedContactEntity = updateCachedInfoWithAttesterInfo(cachedContactEntity, it, emailLowerCase)
                }
              } else {
                cachedContactEntity?.nodeKeyDetails?.fingerprint?.let { fingerprint ->
                  getPgpContactInfoFromServer(fingerprint = fingerprint)?.let {
                    val cacheLastModified = cachedContactEntity?.nodeKeyDetails?.lastModified ?: 0
                    val attesterLastModified = it.nodeKeyDetails?.lastModified ?: 0
                    val attesterFingerprint = it.nodeKeyDetails?.fingerprint

                    if (attesterLastModified > cacheLastModified && fingerprint.equals(attesterFingerprint, true)) {
                      cachedContactEntity = updateCachedInfoWithAttesterInfo(cachedContactEntity, it, emailLowerCase)
                    }
                  }
                }
              }

              cachedContactEntity?.let { pgpContacts.add(it) }
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

  private suspend fun updateCachedInfoWithAttesterInfo(cachedContactEntity: ContactEntity?,
                                                       attesterPgpContact: PgpContact, emailLowerCase: String): ContactEntity? {
    cachedContactEntity ?: return null
    val updateCandidate = if (
        cachedContactEntity.name.isNullOrEmpty()
        && cachedContactEntity.email.equals(attesterPgpContact.email, ignoreCase = true)) {
      attesterPgpContact.toContactEntity().copy(
          id = cachedContactEntity.id,
          email = cachedContactEntity.email)
    } else {
      attesterPgpContact.toContactEntity().copy(
          id = cachedContactEntity.id,
          name = cachedContactEntity.name,
          email = cachedContactEntity.email)
    }

    roomDatabase.contactsDao().updateSuspend(updateCandidate)
    val lastVersion = roomDatabase.contactsDao().getContactByEmailSuspend(emailLowerCase)

    lastVersion?.publicKey?.let {
      val result = pgpApiRepository.fetchKeyDetails(ParseKeysRequest(String(it)))
      lastVersion.nodeKeyDetails = result.data?.nodeKeyDetails?.firstOrNull()
    }

    return lastVersion
  }

  fun deleteContact(contactEntity: ContactEntity) {
    viewModelScope.launch {
      roomDatabase.contactsDao().deleteSuspend(contactEntity)
    }
  }

  fun addContact(pgpContact: PgpContact) {
    viewModelScope.launch {
      val contact = roomDatabase.contactsDao().getContactByEmailSuspend(pgpContact.email)
      if (contact == null) {
        roomDatabase.contactsDao().insertSuspend(pgpContact.toContactEntity())
      }
    }
  }

  fun updateContact(pgpContact: PgpContact) {
    viewModelScope.launch {
      val contact = roomDatabase.contactsDao().getContactByEmailSuspend(pgpContact.email)
      if (contact != null) {
        val updateCandidate = pgpContact.toContactEntity().copy(id = contact.id)
        roomDatabase.contactsDao().updateSuspend(updateCandidate)
      }
    }
  }

  fun updateContactPgpInfo(contactEntity: ContactEntity?, nodeKeyDetails: NodeKeyDetails) {
    viewModelScope.launch {
      contactEntity?.let {
        val contactEntityFromPrimaryPgpContact = nodeKeyDetails.primaryPgpContact.toContactEntity()
        roomDatabase.contactsDao().updateSuspend(contactEntityFromPrimaryPgpContact.copy(
            id = contactEntity.id,
            email = contactEntity.email.toLowerCase(Locale.US),
            client = ContactEntity.CLIENT_PGP,
        ))
      }
    }
  }

  fun filterContacts(searchPattern: String?) {
    searchPatternLiveData.value = searchPattern
  }

  fun deleteContactByEmail(email: String) {
    viewModelScope.launch {
      roomDatabase.contactsDao().getContactByEmailSuspend(email)?.let {
        roomDatabase.contactsDao().deleteSuspend(it)
      }
    }
  }

  fun fetchPubKeys(keyIdOrEmail: String, requestCode: Long) {
    viewModelScope.launch {
      pubKeysFromAttesterLiveData.value = Result.loading(requestCode = requestCode)
      pubKeysFromAttesterLiveData.value = apiRepository.getPub(requestCode = requestCode, context = getApplication(), identData = keyIdOrEmail)
    }
  }

  private fun setResultForRemoteContactsLiveData(type: ContactEntity.Type, result: Result<List<ContactEntity>>) {
    when (type) {
      ContactEntity.Type.TO -> {
        contactsToLiveData.value = result
      }

      ContactEntity.Type.CC -> {
        contactsCcLiveData.value = result
      }

      ContactEntity.Type.BCC -> {
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
  private suspend fun getPgpContactInfoFromServer(email: String? = null, fingerprint: String? = null):
      PgpContact? =
      withContext(Dispatchers.IO) {
        try {
          val response = apiRepository.getPub(
              context = getApplication(),
              identData = email ?: fingerprint ?: "")

          when (response.status) {
            Result.Status.SUCCESS -> {
              val pubKeyString = response.data?.pubkey
              val client = ContactEntity.CLIENT_PGP

              if (pubKeyString?.isNotEmpty() == true) {
                pgpApiRepository.fetchKeyDetails(ParseKeysRequest(pubKeyString)).data?.nodeKeyDetails?.firstOrNull()?.let {
                  val pgpContact = it.primaryPgpContact
                  pgpContact.client = client
                  pgpContact.nodeKeyDetails = it
                  return@withContext pgpContact
                }
              }
            }

            Result.Status.ERROR -> {
              throw ApiException(response.data?.apiError
                  ?: ApiError(code = -1, msg = "Unknown API error"))
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
