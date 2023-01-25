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
import com.flowcrypt.email.api.retrofit.ApiClientRepository
import com.flowcrypt.email.api.retrofit.ApiRepository
import com.flowcrypt.email.api.retrofit.response.attester.PubResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import com.flowcrypt.email.util.exception.ApiException
import com.flowcrypt.email.util.exception.ExceptionUtil
import jakarta.mail.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

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
  private val apiClientRepository: ApiRepository = ApiClientRepository()
  private val searchPatternLiveData: MutableLiveData<String> = MutableLiveData()
  private val controlledRunnerForPubKeysFromServer = ControlledRunner<Result<PubResponse?>>()

  val allContactsLiveData: LiveData<List<RecipientEntity>> =
    roomDatabase.recipientDao().getAllRecipientsLD()
  val recipientsWithPgpFlow = roomDatabase.recipientDao().getAllRecipientsWithPgpFlow()
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
  val recipientsFromLiveData: MutableLiveData<Result<List<RecipientWithPubKeys>>> =
    MutableLiveData()

  private val lookUpPubKeysMutableStateFlow: MutableStateFlow<Result<PubResponse?>> =
    MutableStateFlow(Result.loading())
  val lookUpPubKeysStateFlow: StateFlow<Result<PubResponse?>> =
    lookUpPubKeysMutableStateFlow.asStateFlow()

  private val addPublicKeyToRecipientMutableStateFlow: MutableStateFlow<Result<RecipientWithPubKeys>> =
    MutableStateFlow(Result.none())
  val addPublicKeyToRecipientStateFlow: StateFlow<Result<RecipientWithPubKeys>> =
    addPublicKeyToRecipientMutableStateFlow.asStateFlow()

  private val updateRecipientPublicKeyMutableStateFlow: MutableStateFlow<Result<Boolean?>> =
    MutableStateFlow(Result.none())
  val updateRecipientPublicKeyStateFlow: StateFlow<Result<Boolean?>> =
    updateRecipientPublicKeyMutableStateFlow.asStateFlow()

  fun contactChangesLiveData(recipientEntity: RecipientEntity): LiveData<RecipientWithPubKeys?> {
    return roomDatabase.recipientDao().getRecipientsWithPubKeysByEmailsLD(recipientEntity.email)
  }

  fun fetchAndUpdateInfoAboutRecipients(type: Message.RecipientType, emails: List<String>) {
    viewModelScope.launch {
      if (emails.isEmpty()) {
        return@launch
      }

      setResultForRemoteContactsLiveData(type, Result.loading())

      val recipients = ArrayList<RecipientWithPubKeys>()
      try {
        for (email in emails) {
          if (GeneralUtil.isEmailValid(email)) {
            val emailLowerCase = email.lowercase()
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
              getPublicKeysFromRemoteServersInternal(email = emailLowerCase)?.let { pgpKeyDetailsList ->
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

  fun deleteRecipient(recipientEntity: RecipientEntity) {
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

  fun copyPubKeysToRecipient(recipientEntity: RecipientEntity, pgpKeyDetails: PgpKeyDetails) {
    viewModelScope.launch {
      addPublicKeyToRecipientMutableStateFlow.value = Result.loading()
      try {
        val existingPubKey = roomDatabase.pubKeyDao()
          .getPublicKeyByRecipientAndFingerprint(recipientEntity.email, pgpKeyDetails.fingerprint)
        if (existingPubKey == null) {
          roomDatabase.pubKeyDao()
            .insertSuspend(pgpKeyDetails.toPublicKeyEntity(recipientEntity.email))
        }
        addPublicKeyToRecipientMutableStateFlow.value = Result.success(
          requireNotNull(
            roomDatabase.recipientDao().getRecipientWithPubKeysByEmailSuspend(recipientEntity.email)
          )
        )
      } catch (e: Exception) {
        addPublicKeyToRecipientMutableStateFlow.value = Result.exception(e)
      }
    }
  }

  fun updateExistingPubKey(publicKeyEntity: PublicKeyEntity, pgpKeyDetails: PgpKeyDetails) {
    viewModelScope.launch {
      updateRecipientPublicKeyMutableStateFlow.value = Result.loading()

      try {
        updateRecipientPublicKeyMutableStateFlow.value = Result.success(
          roomDatabase.pubKeyDao()
            .updateSuspend(
              pgpKeyDetails.toPublicKeyEntity(publicKeyEntity.recipient).copy(
                id = publicKeyEntity.id,
                recipient = publicKeyEntity.recipient
              )
            ) > 0
        )
      } catch (e: Exception) {
        updateRecipientPublicKeyMutableStateFlow.value = Result.exception(e)
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

  fun filterContacts(searchPattern: String) {
    searchPatternLiveData.value = searchPattern
  }

  fun deleteContactByEmail(email: String) {
    viewModelScope.launch {
      roomDatabase.recipientDao().getRecipientByEmailSuspend(email)?.let {
        roomDatabase.recipientDao().deleteSuspend(it)
      }
    }
  }

  fun getRawPublicKeysFromRemoteServers(email: String) {
    viewModelScope.launch {
      lookUpPubKeysMutableStateFlow.value = Result.loading()
      val activeAccount = getActiveAccountSuspend()
      lookUpPubKeysMutableStateFlow.value =
        controlledRunnerForPubKeysFromServer.cancelPreviousThenRun {
          return@cancelPreviousThenRun apiClientRepository.pubLookup(
            context = getApplication(),
            email = email,
            clientConfiguration = activeAccount?.clientConfiguration
          )
        }
    }
  }

  private fun setResultForRemoteContactsLiveData(
    type: Message.RecipientType,
    result: Result<List<RecipientWithPubKeys>>
  ) {
    when (type) {
      Message.RecipientType.TO -> {
        recipientsToLiveData.value = result
      }

      Message.RecipientType.CC -> {
        recipientsCcLiveData.value = result
      }

      Message.RecipientType.BCC -> {
        recipientsBccLiveData.value = result
      }

      FROM -> {
        recipientsFromLiveData.value = result
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
  private suspend fun getPublicKeysFromRemoteServersInternal(email: String):
      List<PgpKeyDetails>? = withContext(Dispatchers.IO) {
    try {
      val activeAccount = getActiveAccountSuspend()
      val response = apiClientRepository.pubLookup(
        context = getApplication(),
        email = email,
        clientConfiguration = activeAccount?.clientConfiguration
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
    cachedRecipientEntity: RecipientWithPubKeys, fetchedPgpKeyDetailsList: List<PgpKeyDetails>
  ) = withContext(Dispatchers.IO) {
    val email = cachedRecipientEntity.recipient.email
    val uniqueMapOfFetchedPubKeys = deduplicateFetchedPubKeysByFingerprint(fetchedPgpKeyDetailsList)

    val deDuplicatedListOfFetchedPubKeys = uniqueMapOfFetchedPubKeys.values
    for (fetchedPgpKeyDetails in deDuplicatedListOfFetchedPubKeys) {
      if (!fetchedPgpKeyDetails.usableForEncryption) {
        //we skip a key that is not usable for encryption
        continue
      }

      val existingPublicKeyEntity = cachedRecipientEntity.publicKeys.firstOrNull {
        it.fingerprint == fetchedPgpKeyDetails.fingerprint
      }
      val existingPgpKeyDetails = existingPublicKeyEntity?.pgpKeyDetails
      if (existingPgpKeyDetails != null) {
        val isExistingKeyRevoked = existingPgpKeyDetails.isRevoked
        if (!isExistingKeyRevoked && fetchedPgpKeyDetails.isNewerThan(existingPgpKeyDetails)) {
          roomDatabase.pubKeyDao().updateSuspend(
            existingPublicKeyEntity.copy(publicKey = fetchedPgpKeyDetails.publicKey.toByteArray())
          )
        }
      } else {
        roomDatabase.pubKeyDao()
          .insertWithReplaceSuspend(fetchedPgpKeyDetails.toPublicKeyEntity(email))
      }
    }
  }

  private fun deduplicateFetchedPubKeysByFingerprint(
    fetchedPgpKeyDetailsList: List<PgpKeyDetails>
  ): Map<String, PgpKeyDetails> {
    val uniqueMapOfFetchedPubKeys = mutableMapOf<String, PgpKeyDetails>()

    for (fetchedPgpKeyDetails in fetchedPgpKeyDetailsList) {
      val fetchedFingerprint = fetchedPgpKeyDetails.fingerprint
      val alreadyEncounteredFetchedPgpKeyDetails = uniqueMapOfFetchedPubKeys[fetchedFingerprint]
      if (alreadyEncounteredFetchedPgpKeyDetails == null) {
        uniqueMapOfFetchedPubKeys[fetchedFingerprint] = fetchedPgpKeyDetails
      } else {
        if (fetchedPgpKeyDetails.isNewerThan(alreadyEncounteredFetchedPgpKeyDetails)) {
          uniqueMapOfFetchedPubKeys[fetchedFingerprint] = fetchedPgpKeyDetails
        }
      }
    }

    return uniqueMapOfFetchedPubKeys
  }

  class FROM : Message.RecipientType("From")

  companion object {
    val FROM = FROM()
  }
}
