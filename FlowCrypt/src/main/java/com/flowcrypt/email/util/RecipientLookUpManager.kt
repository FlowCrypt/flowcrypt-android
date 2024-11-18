/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util

import android.app.Application
import com.flowcrypt.email.api.retrofit.ApiClientRepository
import com.flowcrypt.email.api.retrofit.response.attester.PubResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.util.exception.ApiException
import jakarta.mail.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Denys Bondarenko
 */
class RecipientLookUpManager(
  private val application: Application,
  private val roomDatabase: FlowCryptRoomDatabase,
  private val viewModelScope: CoroutineScope,
  private val updateListener: (recipientInfo: RecipientInfo) -> Unit
) {
  private val lookUpCandidates = ConcurrentHashMap<String, RecipientInfo>()
  private val recipientsSessionCache = ConcurrentHashMap<String, RecipientWithPubKeys>()

  private val lookUpLimitedParallelismDispatcher =
    Dispatchers.IO.limitedParallelism(PARALLELISM_COUNT)

  suspend fun enqueue(recipientInfo: RecipientInfo) = withContext(Dispatchers.IO) {
    viewModelScope.launch {
      val email = recipientInfo.recipientWithPubKeys.recipient.email
      if (recipientsSessionCache.containsKey(email)) {
        //we return a value from the session cache
        updateListener.invoke(
          recipientInfo.copy(
            isUpdating = false,
            recipientWithPubKeys = requireNotNull(recipientsSessionCache[email])
          )
        )
      } else {
        lookUpCandidates[email] = recipientInfo
        if (!recipientInfo.isUpdating) {
          updateListener.invoke(recipientInfo.copy(isUpdating = true))
        }
        try {
          val recipientWithPubKeysAfterLookUp = lookUp(email)
          dequeue(email)
          if (recipientWithPubKeysAfterLookUp.hasUsablePubKey()) {
            recipientsSessionCache[email] = recipientWithPubKeysAfterLookUp
          }
          updateListener.invoke(
            recipientInfo.copy(
              isUpdating = false,
              recipientWithPubKeys = recipientWithPubKeysAfterLookUp
            )
          )
        } catch (e: Exception) {
          e.printStackTrace()
          updateListener.invoke(recipientInfo.copy(isUpdating = false))
        }
      }
    }
  }

  private suspend fun lookUp(email: String): RecipientWithPubKeys = withContext(Dispatchers.IO) {
    val emailLowerCase = email.lowercase()
    var cachedRecipientWithPubKeys = getCachedRecipientWithPubKeys(emailLowerCase)
    if (cachedRecipientWithPubKeys == null) {
      roomDatabase.recipientDao().insertSuspend(RecipientEntity(email = emailLowerCase))
      cachedRecipientWithPubKeys =
        roomDatabase.recipientDao().getRecipientWithPubKeysByEmailSuspend(emailLowerCase)
    }

    getPublicKeysFromRemoteServersInternal(email = emailLowerCase)?.let { pgpKeyDetailsList ->
      cachedRecipientWithPubKeys?.let { recipientWithPubKeys ->
        updateCachedInfoWithPubKeysFromLookUp(
          recipientWithPubKeys,
          pgpKeyDetailsList
        )
      }
    }
    cachedRecipientWithPubKeys = getCachedRecipientWithPubKeys(emailLowerCase)

    return@withContext requireNotNull(cachedRecipientWithPubKeys)
  }

  fun dequeue(email: String) {
    lookUpCandidates.remove(email)
  }

  private suspend fun getCachedRecipientWithPubKeys(emailLowerCase: String): RecipientWithPubKeys? =
    withContext(Dispatchers.IO) {
      val cachedRecipientWithPubKeys = roomDatabase.recipientDao()
        .getRecipientWithPubKeysByEmailSuspend(emailLowerCase) ?: return@withContext null

      for (publicKeyEntity in cachedRecipientWithPubKeys.publicKeys) {
        try {
          val result = PgpKey.parseKeys(source = publicKeyEntity.publicKey).pgpKeyDetailsList
          publicKeyEntity.pgpKeyRingDetails = result.firstOrNull()
        } catch (e: Exception) {
          e.printStackTrace()
          publicKeyEntity.isNotUsable = true
        }
      }
      return@withContext cachedRecipientWithPubKeys
    }

  private suspend fun getPublicKeysFromRemoteServersInternal(email: String):
      List<PgpKeyRingDetails>? = withContext(Dispatchers.IO) {
    try {
      val activeAccount = roomDatabase.accountDao().getActiveAccountSuspend()
      if (!lookUpCandidates.containsKey(email)) {
        return@withContext null
      }
      val response = pubLookup(email, activeAccount)

      when (response.status) {
        Result.Status.SUCCESS -> {
          val sourceString = response.data?.pubkey
          if (sourceString?.isNotEmpty() == true) {
            val parsedResult = PgpKey.parseKeys(source = sourceString).pgpKeyDetailsList
            if (parsedResult.isNotEmpty()) {
              return@withContext parsedResult
            }
          }
        }

        Result.Status.ERROR -> {
          throw ApiException(
            response.apiError ?: ApiError(
              code = -1,
              message = "Unknown API error"
            )
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

  private suspend fun pubLookup(
    email: String,
    activeAccount: AccountEntity?
  ): Result<PubResponse> = withContext(lookUpLimitedParallelismDispatcher) {
    return@withContext ApiClientRepository.PubLookup.fetchPubKey(
      context = application,
      email = email,
      clientConfiguration = activeAccount?.clientConfiguration
    )
  }

  private suspend fun updateCachedInfoWithPubKeysFromLookUp(
    cachedRecipientEntity: RecipientWithPubKeys,
    fetchedPgpKeyRingDetailsList: List<PgpKeyRingDetails>
  ) = withContext(Dispatchers.IO) {
    val email = cachedRecipientEntity.recipient.email
    val uniqueMapOfFetchedPubKeys =
      deduplicateFetchedPubKeysByFingerprint(fetchedPgpKeyRingDetailsList)

    val deDuplicatedListOfFetchedPubKeys = uniqueMapOfFetchedPubKeys.values
    for (fetchedPgpKeyDetails in deDuplicatedListOfFetchedPubKeys) {
      val existingPublicKeyEntity = cachedRecipientEntity.publicKeys.firstOrNull {
        it.fingerprint == fetchedPgpKeyDetails.fingerprint
      }
      val existingPgpKeyDetails = existingPublicKeyEntity?.pgpKeyRingDetails
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
    fetchedPgpKeyRingDetailsList: List<PgpKeyRingDetails>
  ): Map<String, PgpKeyRingDetails> {
    val uniqueMapOfFetchedPubKeys = mutableMapOf<String, PgpKeyRingDetails>()

    for (fetchedPgpKeyDetails in fetchedPgpKeyRingDetailsList) {
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

  data class RecipientInfo(
    val recipientType: Message.RecipientType,
    val recipientWithPubKeys: RecipientWithPubKeys,
    val creationTime: Long = System.currentTimeMillis(),
    var isUpdating: Boolean = true,
    var isUpdateFailed: Boolean = false,
    val isModifyingEnabled: Boolean = true
  )

  companion object {
    const val PARALLELISM_COUNT = 10
  }
}
