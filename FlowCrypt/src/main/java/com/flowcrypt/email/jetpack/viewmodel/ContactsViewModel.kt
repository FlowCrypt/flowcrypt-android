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
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.api.retrofit.request.model.PostLookUpEmailModel
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.ContactEntity
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.ApiException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.common.util.CollectionUtils
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
  private val repository: ApiRepository = FlowcryptApiRepository()
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
          val foundContacts = if (it.isNullOrEmpty()) {
            roomDatabase.contactsDao().getAllContactsWithPgp()
          } else {
            roomDatabase.contactsDao().getAllContactsWithPgpWhichMatched("%$it%")
          }
          emit(Result.success(foundContacts))
        }
      }
  val remoteContactsLiveData: MutableLiveData<Result<List<ContactEntity>>> = MutableLiveData()

  fun updateContactPubKey(pgpContact: PgpContact, pgpContactFromKey: PgpContact) {
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

  fun updateContactPubKey(email: String, pubKey: ByteArray?) {
    viewModelScope.launch {
      val contactEntity = roomDatabase.contactsDao().getContactByEmailSuspend(email)
          ?: return@launch
      roomDatabase.contactsDao().updateSuspend(contactEntity.copy(publicKey = pubKey))
    }
  }

  /**
   * Here we do the following things:
   *
   *  a) look up the email in the database;
   *  b) if there is a record for that email and has_pgp==true, we can use the `pubkey` instead of
   * querying Attester;
   *  c) if there is a record but `has_pgp==false`, do `flowcrypt.com/attester/lookup/email` API call
   * to see if you can now get the pubkey. If a pubkey is available, save it back to the database.
   *  e) no record in the db found:
   *  1. save an empty record eg `new PgpContact(email, null);` - this means we don't know if they have PGP yet
   *  1. look up the email on `flowcrypt.com/attester/lookup/email`
   *  1. if pubkey comes back, create something like `new PgpContact(js, email, null, pubkey,
   * client);`. The PgpContact constructor will define has_pgp, longid, fingerprint, etc
   * for you. Then save that object into database.
   *  1. if no pubkey found, create `new PgpContact(js, email, null, null, null, null);` - this
   * means we know they don't currently have PGP
   */
  fun fetchAndUpdateInfoAboutContacts(emails: List<String>) {
    viewModelScope.launch {
      remoteContactsLiveData.value = Result.loading()

      val pgpContacts = ArrayList<ContactEntity>()
      try {
        for (email in emails) {
          if (GeneralUtil.isEmailValid(email)) {
            val emailLowerCase = email.toLowerCase(Locale.getDefault())
            var localPgpContact = roomDatabase.contactsDao().getContactByEmailSuspend(emailLowerCase)

            if (localPgpContact == null) {
              localPgpContact = PgpContact(emailLowerCase, null).toContactEntity()
              roomDatabase.contactsDao().insertSuspend(localPgpContact)
            }

            try {
              if (!localPgpContact.hasPgp) {
                val remotePgpContact = getPgpContactInfoFromServer(emailLowerCase)
                if (remotePgpContact != null) {
                  val updateCandidate = if (localPgpContact.name.isNullOrEmpty()
                      && localPgpContact.email.equals(remotePgpContact.email, ignoreCase = true)) {
                    remotePgpContact.toContactEntity().copy(id = localPgpContact.id)
                  } else {
                    remotePgpContact.toContactEntity().copy(id = localPgpContact.id, name = localPgpContact.name)
                  }

                  roomDatabase.contactsDao().updateSuspend(updateCandidate)
                  localPgpContact = roomDatabase.contactsDao().getContactByEmailSuspend(emailLowerCase)
                }
              }

              localPgpContact?.let { pgpContacts.add(it) }
            } catch (e: Exception) {
              e.printStackTrace()
              ExceptionUtil.handleError(e)
            }
          }
        }

        remoteContactsLiveData.value = Result.success(pgpContacts)
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        remoteContactsLiveData.value = Result.exception(e)
      }
    }
  }

  fun deleteContact(contactEntity: ContactEntity) {
    viewModelScope.launch {
      roomDatabase.contactsDao().deleteSuspend(contactEntity)
    }
  }

  fun addContact(pgpContact: PgpContact) {
    viewModelScope.launch {
      roomDatabase.contactsDao().insertSuspend(pgpContact.toContactEntity())
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

  /**
   * Get information about [PgpContact] from the remote server.
   *
   * @param email Used to generate a request to the server.
   * @return [PgpContact]
   * @throws IOException
   */
  private suspend fun getPgpContactInfoFromServer(email: String): PgpContact? =
      withContext(Dispatchers.IO) {
        val response = repository.postLookUpEmail(getApplication(), PostLookUpEmailModel(email))
        when (response.status) {
          Result.Status.SUCCESS -> {
            if (response.data?.pubKey?.isNotEmpty() == true) {
              val client = if (response.data.hasCryptup()) {
                ContactEntity.CLIENT_FLOWCRYPT
              } else {
                ContactEntity.CLIENT_PGP
              }
              val details = NodeCallsExecutor.parseKeys(response.data.pubKey)
              if (!CollectionUtils.isEmpty(details)) {
                val pgpContact = details[0].primaryPgpContact
                pgpContact.client = client
                return@withContext pgpContact
              }
            }
          }

          Result.Status.ERROR -> {
            throw ApiException(response.data?.apiError ?: ApiError(msg = "Unknown API error"))
          }

          else -> {
            throw response.exception ?: java.lang.Exception()
          }
        }

        null
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
}