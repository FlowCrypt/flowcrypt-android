/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.ContentResolver
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.api.retrofit.ApiClientRepository
import com.flowcrypt.email.api.retrofit.response.attester.PubResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.extensions.kotlin.isValidEmail
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.adapter.RecipientChipRecyclerViewAdapter.RecipientInfo
import com.flowcrypt.email.util.exception.ApiException
import jakarta.mail.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InvalidObjectException
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Denis Bondarenko
 *         Date: 12/23/21
 *         Time: 10:44 AM
 *         E-mail: DenBond7@gmail.com
 */
class ComposeMsgViewModel(isCandidateToEncrypt: Boolean, application: Application) :
  RoomBasicViewModel(application) {
  private val recipientLookUpManager =
    RecipientLookUpManager(application, roomDatabase, viewModelScope) {
      replaceRecipient(Message.RecipientType.TO, it)
      replaceRecipient(Message.RecipientType.CC, it)
      replaceRecipient(Message.RecipientType.BCC, it)
    }

  private val messageEncryptionTypeMutableStateFlow: MutableStateFlow<MessageEncryptionType> =
    MutableStateFlow(
      if (isCandidateToEncrypt) {
        MessageEncryptionType.ENCRYPTED
      } else {
        MessageEncryptionType.STANDARD
      }
    )
  val messageEncryptionTypeStateFlow: StateFlow<MessageEncryptionType> =
    messageEncryptionTypeMutableStateFlow.asStateFlow()

  private val webPortalPasswordMutableStateFlow: MutableStateFlow<CharSequence> =
    MutableStateFlow("")
  val webPortalPasswordStateFlow: StateFlow<CharSequence> =
    webPortalPasswordMutableStateFlow.asStateFlow()

  private val recipientsToMutableStateFlow: MutableStateFlow<MutableMap<String, RecipientInfo>> =
    MutableStateFlow(mutableMapOf())
  val recipientsToStateFlow: StateFlow<Map<String, RecipientInfo>> =
    recipientsToMutableStateFlow.asStateFlow()

  private val recipientsCcMutableStateFlow: MutableStateFlow<MutableMap<String, RecipientInfo>> =
    MutableStateFlow(mutableMapOf())
  val recipientsCcStateFlow: StateFlow<Map<String, RecipientInfo>> =
    recipientsCcMutableStateFlow.asStateFlow()

  private val recipientsBccMutableStateFlow: MutableStateFlow<MutableMap<String, RecipientInfo>> =
    MutableStateFlow(mutableMapOf())
  val recipientsBccStateFlow: StateFlow<Map<String, RecipientInfo>> =
    recipientsBccMutableStateFlow.asStateFlow()

  val recipientsStateFlow = combine(
    recipientsToStateFlow,
    recipientsCcStateFlow,
    recipientsBccStateFlow
  ) { a, b, c ->
    a + b + c
  }

  val msgEncryptionType: MessageEncryptionType
    get() = messageEncryptionTypeStateFlow.value
  val recipientsTo: Map<String, RecipientInfo>
    get() = recipientsToStateFlow.value
  val recipientsCc: Map<String, RecipientInfo>
    get() = recipientsCcStateFlow.value
  val recipientsBcc: Map<String, RecipientInfo>
    get() = recipientsBccStateFlow.value
  val allRecipients: Map<String, RecipientInfo>
    get() = recipientsTo + recipientsCc + recipientsBcc

  val hasAttachmentsWithExternalStorageUri: Boolean
    get() = attachmentsStateFlow.value.any {
      ContentResolver.SCHEME_FILE.equals(it.uri?.scheme, ignoreCase = true)
    }

  private val outgoingMessageInfoMutableStateFlow: MutableStateFlow<OutgoingMessageInfo> =
    MutableStateFlow(OutgoingMessageInfo())
  val outgoingMessageInfoStateFlow: StateFlow<OutgoingMessageInfo> =
    outgoingMessageInfoMutableStateFlow.asStateFlow()

  private val attachmentsMutableStateFlow: MutableStateFlow<List<AttachmentInfo>> =
    MutableStateFlow(emptyList())
  val attachmentsStateFlow: StateFlow<List<AttachmentInfo>> =
    attachmentsMutableStateFlow.asStateFlow()

  fun addAttachments(attachments: List<AttachmentInfo>) {
    attachmentsMutableStateFlow.update { existingAttachments ->
      existingAttachments.toMutableList().apply {
        addAll(attachments)
      }
    }
  }

  fun removeAttachments(attachments: List<AttachmentInfo>) {
    attachmentsMutableStateFlow.update { existingAttachments ->
      existingAttachments.toMutableList().apply {
        removeAll(attachments)
      }
    }
  }

  fun updateOutgoingMessageInfo(outgoingMessageInfo: OutgoingMessageInfo) {
    outgoingMessageInfoMutableStateFlow.update { outgoingMessageInfo }
  }

  fun switchMessageEncryptionType(messageEncryptionType: MessageEncryptionType) {
    messageEncryptionTypeMutableStateFlow.value = messageEncryptionType
  }

  fun setWebPortalPassword(webPortalPassword: CharSequence = "") {
    webPortalPasswordMutableStateFlow.value = webPortalPassword
  }

  fun addRecipient(
    recipientType: Message.RecipientType,
    email: CharSequence
  ) {
    viewModelScope.launch {
      val normalizedEmail = email.toString().lowercase()
      val existingRecipient = roomDatabase.recipientDao()
        .getRecipientWithPubKeysByEmailSuspend(normalizedEmail)
        ?: if (normalizedEmail.isValidEmail()) {
          roomDatabase.recipientDao().insertSuspend(RecipientEntity(email = normalizedEmail))
          roomDatabase.recipientDao().getRecipientWithPubKeysByEmailSuspend(normalizedEmail)
        } else null

      existingRecipient?.let {
        val recipientInfo = RecipientInfo(recipientType, it)
        when (recipientType) {
          Message.RecipientType.TO -> recipientsToMutableStateFlow
          Message.RecipientType.CC -> recipientsCcMutableStateFlow
          Message.RecipientType.BCC -> recipientsBccMutableStateFlow
          else -> throw InvalidObjectException("unknown RecipientType: $recipientType")
        }.update { map ->
          map.toMutableMap().apply { put(normalizedEmail, RecipientInfo(recipientType, it)) }
        }

        recipientLookUpManager.enqueue(recipientInfo)
      }
    }
  }

  fun removeRecipient(
    recipientType: Message.RecipientType,
    recipientEmail: String
  ) {
    viewModelScope.launch {
      val normalizedEmail = recipientEmail.lowercase()

      when (recipientType) {
        Message.RecipientType.TO -> recipientsToMutableStateFlow
        Message.RecipientType.CC -> recipientsCcMutableStateFlow
        Message.RecipientType.BCC -> recipientsBccMutableStateFlow
        else -> throw InvalidObjectException("unknown RecipientType: $recipientType")
      }.update { map ->
        map.toMutableMap().apply { remove(normalizedEmail) }
      }

      recipientLookUpManager.dequeue(normalizedEmail)
    }
  }

  fun reCacheRecipient(
    recipientType: Message.RecipientType,
    email: CharSequence
  ) {
    viewModelScope.launch {
      val normalizedEmail = email.toString().lowercase()
      val existingRecipientWithPubKeys = roomDatabase.recipientDao()
        .getRecipientWithPubKeysByEmailSuspend(normalizedEmail) ?: return@launch
      val existingRecipientInfo = when (recipientType) {
        Message.RecipientType.TO -> recipientsToMutableStateFlow
        Message.RecipientType.CC -> recipientsCcMutableStateFlow
        Message.RecipientType.BCC -> recipientsBccMutableStateFlow
        else -> throw InvalidObjectException("unknown RecipientType: $recipientType")
      }.value[normalizedEmail] ?: return@launch
      when (recipientType) {
        Message.RecipientType.TO -> recipientsToMutableStateFlow
        Message.RecipientType.CC -> recipientsCcMutableStateFlow
        Message.RecipientType.BCC -> recipientsBccMutableStateFlow
        else -> throw InvalidObjectException("unknown RecipientType: $recipientType")
      }.update { map ->
        map.toMutableMap().apply {
          replace(
            normalizedEmail,
            existingRecipientInfo.copy(recipientWithPubKeys = existingRecipientWithPubKeys)
          )
        }
      }
    }
  }

  private fun replaceRecipient(
    recipientType: Message.RecipientType,
    recipientInfo: RecipientInfo
  ) {
    viewModelScope.launch {
      val normalizedEmail = recipientInfo.recipientWithPubKeys.recipient.email
      when (recipientType) {
        Message.RecipientType.TO -> recipientsToMutableStateFlow
        Message.RecipientType.CC -> recipientsCcMutableStateFlow
        Message.RecipientType.BCC -> recipientsBccMutableStateFlow
        else -> throw InvalidObjectException("unknown RecipientType: $recipientType")
      }.update { map ->
        map.toMutableMap().apply { replace(normalizedEmail, recipientInfo) }
      }
    }
  }

  fun callLookUpForMissedPubKeys() {
    viewModelScope.launch {
      allRecipients.forEach { entry ->
        recipientLookUpManager.enqueue(entry.value)
      }
    }
  }

  class RecipientLookUpManager(
    private val application: Application,
    private val roomDatabase: FlowCryptRoomDatabase,
    private val viewModelScope: CoroutineScope,
    private val updateListener: (recipientInfo: RecipientInfo) -> Unit
  ) {
    private val apiClientRepository = ApiClientRepository()
    private val lookUpCandidates = ConcurrentHashMap<String, RecipientInfo>()
    private val recipientsSessionCache = ConcurrentHashMap<String, RecipientWithPubKeys>()

    @OptIn(ExperimentalCoroutinesApi::class)
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
            val result = PgpKey.parseKeys(publicKeyEntity.publicKey).pgpKeyDetailsList
            publicKeyEntity.pgpKeyDetails = result.firstOrNull()
          } catch (e: Exception) {
            e.printStackTrace()
            publicKeyEntity.isNotUsable = true
          }
        }
        return@withContext cachedRecipientWithPubKeys
      }

    private suspend fun getPublicKeysFromRemoteServersInternal(email: String):
        List<PgpKeyDetails>? = withContext(Dispatchers.IO) {
      try {
        val activeAccount = roomDatabase.accountDao().getActiveAccountSuspend()
        if (!lookUpCandidates.containsKey(email)) {
          return@withContext null
        }
        val response = pubLookup(email, activeAccount)

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
              response.data?.apiError ?: ApiError(
                code = -1,
                msg = "Unknown API error"
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
      return@withContext apiClientRepository.pubLookup(
        context = application,
        email = email,
        clientConfiguration = activeAccount?.clientConfiguration
      )
    }

    private suspend fun updateCachedInfoWithPubKeysFromLookUp(
      cachedRecipientEntity: RecipientWithPubKeys, fetchedPgpKeyDetailsList: List<PgpKeyDetails>
    ) = withContext(Dispatchers.IO) {
      val email = cachedRecipientEntity.recipient.email
      val uniqueMapOfFetchedPubKeys =
        deduplicateFetchedPubKeysByFingerprint(fetchedPgpKeyDetailsList)

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

    companion object {
      const val PARALLELISM_COUNT = 10
    }
  }
}
