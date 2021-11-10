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
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import com.flowcrypt.email.util.exception.ApiException
import com.flowcrypt.email.util.exception.ExceptionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
  private val controlledRunnerForPubKeysFromServer = ControlledRunner<Result<PubResponse?>>()

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
  val recipientsToLiveData: MutableLiveData<Result<List<RecipientWithPubKeys>>> = MutableLiveData()
  val recipientsCcLiveData: MutableLiveData<Result<List<RecipientWithPubKeys>>> = MutableLiveData()
  val recipientsBccLiveData: MutableLiveData<Result<List<RecipientWithPubKeys>>> = MutableLiveData()

  val pubKeysFromServerLiveData: MutableLiveData<Result<PubResponse?>> = MutableLiveData()
  private val lookUpPubKeysMutableStateFlow: MutableStateFlow<Result<PubResponse?>> =
    MutableStateFlow(Result.loading())
  val lookUpPubKeysStateFlow: StateFlow<Result<PubResponse?>> =
    lookUpPubKeysMutableStateFlow.asStateFlow()

  fun contactChangesLiveData(recipientEntity: RecipientEntity): LiveData<RecipientWithPubKeys?> {
    return roomDatabase.recipientDao().getRecipientsWithPubKeysByEmailsLD(recipientEntity.email)
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
   *  1. save an empty record eg `new RecipientWithPubKeys(email, null);` - this means we don't know if they have PGP yet
   *  1. look up the email on `flowcrypt.com/attester/pub/EMAIL>`
   *  1. if pubkey comes back, create something like `new RecipientWithPubKeys(js, email, null, pubkey,
   * client);`. The RecipientWithPubKeys constructor will define has_pgp, fingerprint, etc
   * for you. Then save that object into database.
   *  1. if no pubkey found, create `new RecipientWithPubKeys(js, email, null, null, null, null);` - this
   * means we know they don't currently have PGP
   */
  fun fetchAndUpdateInfoAboutRecipients(type: RecipientEntity.Type, emails: List<String>) {
    viewModelScope.launch {
      if (emails.isEmpty()) {
        return@launch
      }

      setResultForRemoteContactsLiveData(type, Result.loading())

      val recipients = ArrayList<RecipientWithPubKeys>()
      try {
        for (email in emails) {
          if (GeneralUtil.isEmailValid(email)) {
            val emailLowerCase = email.lowercase(Locale.getDefault())
            var cachedRecipientWithPubKeys = getCachedRecipientWithPubKeys(emailLowerCase)

            if (cachedRecipientWithPubKeys == null) {
              roomDatabase.recipientDao().insertSuspend(RecipientEntity(email = emailLowerCase))
              cachedRecipientWithPubKeys =
                roomDatabase.recipientDao().getRecipientWithPubKeysByEmailSuspend(emailLowerCase)
            } else {
              for (publicKeyEntity in cachedRecipientWithPubKeys.publicKeys) {
                try {
                  val result = PgpKey.parseKeys(publicKeyEntity.publicKey).pgpKeyDetailsList
                  publicKeyEntity.pgpKeyDetails = result.firstOrNull()
                } catch (e: Exception) {
                  e.printStackTrace()
                  publicKeyEntity.isNotUsable = true
                }
              }
            }

            try {
              getPgpInfoFromServer(email = emailLowerCase)?.let { pgpKeyDetailsList ->
                cachedRecipientWithPubKeys?.let { recipientWithPubKeys ->
                  updateCachedInfoWithPubKeysFromLookUp(
                    recipientWithPubKeys,
                    pgpKeyDetailsList
                  )
                }
              }
              cachedRecipientWithPubKeys = getCachedRecipientWithPubKeys(emailLowerCase)
              cachedRecipientWithPubKeys?.let { recipients.add(it) }
            } catch (e: Exception) {
              e.printStackTrace()
              ExceptionUtil.handleError(e)
            }
          }
        }
        setResultForRemoteContactsLiveData(type, Result.success(recipients))
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        setResultForRemoteContactsLiveData(type, Result.exception(e))
      }
    }
  }

  private suspend fun getCachedRecipientWithPubKeys(emailLowerCase: String): RecipientWithPubKeys? =
    withContext(Dispatchers.IO) {
      val cachedRecipientWithPubKeys = roomDatabase.recipientDao()
        .getRecipientWithPubKeysByEmailSuspend(emailLowerCase) ?: return@withContext null

      for (publicKeyEntity in cachedRecipientWithPubKeys.publicKeys) {
        try {
          val result = PgpKey.parseKeys(publicKeyEntity.publicKey).pgpKeyDetailsList
          publicKeyEntity.pgpKeyDetails = result.firstOrNull()
        } catch (e: Exception) {
          e.printStackTrace()
          publicKeyEntity.isNotUsable = true
        }
      }
      return@withContext cachedRecipientWithPubKeys
    }

  private suspend fun updateCachedInfoWithPubKeysFromLookUp(
    cachedRecipientEntity: RecipientWithPubKeys, list: List<PgpKeyDetails>
  ) = withContext(Dispatchers.IO) {
    val email = cachedRecipientEntity.recipient.email
    val mapOfUniquePubKeysFromLookUp = mutableMapOf<String, PgpKeyDetails>()

    for (pgpKeyDetails in list) {
      val fingerprint = pgpKeyDetails.fingerprint
      val existingPgpKeyDetails = mapOfUniquePubKeysFromLookUp[fingerprint]
      if (existingPgpKeyDetails == null) {
        mapOfUniquePubKeysFromLookUp[fingerprint] = pgpKeyDetails
      } else {
        val existingLastModified = existingPgpKeyDetails.lastModified ?: 0
        val candidateLastModified = pgpKeyDetails.lastModified ?: 0
        if (candidateLastModified > existingLastModified) {
          mapOfUniquePubKeysFromLookUp[fingerprint] = pgpKeyDetails
        }
      }
    }

    val lookUpList = mapOfUniquePubKeysFromLookUp.values
    for (pgpKeyDetails in lookUpList) {
      val existingPublicKeyEntity =
        cachedRecipientEntity.publicKeys.firstOrNull { it.fingerprint == pgpKeyDetails.fingerprint }
      if (existingPublicKeyEntity != null) {
        val existingLastModified = existingPublicKeyEntity.pgpKeyDetails?.lastModified ?: 0
        val lookUpLastModified = pgpKeyDetails.lastModified ?: 0

        if (lookUpLastModified > existingLastModified) {
          roomDatabase.pubKeyDao().updateSuspend(
            existingPublicKeyEntity.copy(publicKey = pgpKeyDetails.publicKey.toByteArray())
          )
        }
      } else {
        roomDatabase.pubKeyDao().insertWithReplaceSuspend(pgpKeyDetails.toPublicKeyEntity(email))
      }
    }
  }

  fun deleteContact(recipientEntity: RecipientEntity) {
    viewModelScope.launch {
      roomDatabase.recipientDao().deleteSuspend(recipientEntity)
    }
  }

  fun addRecipientsBasedOnPgpKeyDetails(pgpKeyDetails: PgpKeyDetails) {
    viewModelScope.launch {
      val primaryAddress = pgpKeyDetails.mimeAddresses.firstOrNull()?.address ?: return@launch
      val contact = roomDatabase.recipientDao().getRecipientByEmailSuspend(primaryAddress)
      if (contact == null) {
        val isInserted =
          roomDatabase.recipientDao().insertSuspend(RecipientEntity(email = primaryAddress)) > 0
        if (isInserted) {
          roomDatabase.pubKeyDao().insertSuspend(pgpKeyDetails.toPublicKeyEntity(primaryAddress))
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

  fun copyPubKeysToRecipient(recipientEntity: RecipientEntity?, pgpKeyDetails: PgpKeyDetails) {
    viewModelScope.launch {
      recipientEntity?.let {
        val existingPubKey = roomDatabase.pubKeyDao()
          .getPublicKeyByRecipientAndFingerprint(recipientEntity.email, pgpKeyDetails.fingerprint)
        if (existingPubKey == null) {
          roomDatabase.pubKeyDao()
            .insertSuspend(pgpKeyDetails.toPublicKeyEntity(recipientEntity.email))
        }
      }
    }
  }

  fun copyPubKeysBetweenRecipients(
    sourceRecipientEntity: RecipientEntity?,
    destinationRecipientEntity: RecipientEntity?
  ) {
    viewModelScope.launch {
      sourceRecipientEntity ?: return@launch
      destinationRecipientEntity ?: return@launch

      val existingPubKeysOfSource =
        roomDatabase.pubKeyDao().getPublicKeysByRecipient(sourceRecipientEntity.email)

      if (existingPubKeysOfSource.isNotEmpty()) {
        for (existingPubKey in existingPubKeysOfSource) {
          val fetchedPubKey = roomDatabase.pubKeyDao().getPublicKeyByRecipientAndFingerprint(
            destinationRecipientEntity.email,
            existingPubKey.fingerprint
          )

          if (fetchedPubKey == null) {
            roomDatabase.pubKeyDao()
              .insertSuspend(
                existingPubKey.copy(
                  id = null,
                  recipient = destinationRecipientEntity.email.lowercase()
                )
              )
          }
        }
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

  fun fetchPubKeys(keyIdOrEmail: String) {
    viewModelScope.launch {
      lookUpPubKeysMutableStateFlow.value = Result.loading()
      val activeAccount = getActiveAccountSuspend()
      lookUpPubKeysMutableStateFlow.value =
        controlledRunnerForPubKeysFromServer.cancelPreviousThenRun {
          return@cancelPreviousThenRun apiRepository.pubLookup(
            context = getApplication(),
            identData = keyIdOrEmail,
            orgRules = activeAccount?.clientConfiguration
          )
        }
    }
  }

  private fun setResultForRemoteContactsLiveData(
    type: RecipientEntity.Type,
    result: Result<List<RecipientWithPubKeys>>
  ) {
    when (type) {
      RecipientEntity.Type.TO -> {
        recipientsToLiveData.value = result
      }

      RecipientEntity.Type.CC -> {
        recipientsCcLiveData.value = result
      }

      RecipientEntity.Type.BCC -> {
        recipientsBccLiveData.value = result
      }
    }
  }

  /**
   * Get information about [RecipientWithPubKeys] from the remote server.
   *
   * @param email Used to generate a request to the server.
   * @return [RecipientWithPubKeys]
   * @throws IOException
   */
  private suspend fun getPgpInfoFromServer(
    email: String? = null,
    fingerprint: String? = null
  ): List<PgpKeyDetails>? = withContext(Dispatchers.IO) {
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
            val parsedResult = PgpKey.parseKeys(pubKeyString).pgpKeyDetailsList
            if (parsedResult.isNotEmpty()) {
              return@withContext parsedResult
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
