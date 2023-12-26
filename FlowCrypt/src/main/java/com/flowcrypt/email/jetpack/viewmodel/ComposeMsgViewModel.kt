/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.ContentResolver
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.api.retrofit.ApiClientRepository
import com.flowcrypt.email.api.retrofit.response.attester.PubResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiError
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.extensions.kotlin.isValidEmail
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.security.KeyStoreCryptoManager
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.adapter.RecipientChipRecyclerViewAdapter.RecipientInfo
import com.flowcrypt.email.util.OutgoingMessageInfoManager
import com.flowcrypt.email.util.coroutines.runners.ControlledRunner
import com.flowcrypt.email.util.exception.ApiException
import jakarta.mail.Flags
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
 * @author Denys Bondarenko
 */
class ComposeMsgViewModel(isCandidateToEncrypt: Boolean, application: Application) :
  AccountViewModel(application) {
  private val recipientLookUpManager = RecipientLookUpManager(
    application = application,
    roomDatabase = roomDatabase,
    viewModelScope = viewModelScope
  ) {
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

  private val addOutgoingMessageInfoToQueueMutableStateFlow: MutableStateFlow<Result<Boolean?>> =
    MutableStateFlow(Result.none())
  val addOutgoingMessageInfoToQueueStateFlow: StateFlow<Result<Boolean?>> =
    addOutgoingMessageInfoToQueueMutableStateFlow.asStateFlow()
  private val controlledRunnerForAddingOutgoingMessageInfoToQueue =
    ControlledRunner<Result<Boolean?>>()

  fun sendMessage(password: CharArray?) {
    viewModelScope.launch {
      addOutgoingMessageInfoToQueueMutableStateFlow.value = Result.loading()
      addOutgoingMessageInfoToQueueMutableStateFlow.value =
        controlledRunnerForAddingOutgoingMessageInfoToQueue.joinPreviousOrRun {
          withContext(Dispatchers.IO) {
            var messageEntity: MessageEntity? = null
            try {
              val activeAccount = getActiveAccountSuspend()
                ?: throw IllegalStateException("No active account")
              val finalOutgoingMessageInfo =
                outgoingMessageInfoStateFlow.value.copy(password = password)

              messageEntity = finalOutgoingMessageInfo.toMessageEntity(
                folder = JavaEmailConstants.FOLDER_OUTBOX,
                flags = Flags(Flags.Flag.SEEN),
                password = finalOutgoingMessageInfo.password?.let {
                  KeyStoreCryptoManager.encrypt(
                    String(it)
                  ).toByteArray()
                }
              )
              val messageId = roomDatabase.msgDao().insertSuspend(messageEntity)
              messageEntity = messageEntity.copy(id = messageId, uid = messageId)
              roomDatabase.msgDao().updateSuspend(messageEntity)

              OutgoingMessageInfoManager.enqueueOutgoingMessageInfo(
                context = getApplication(),
                messageId = messageId,
                outgoingMessageInfo = outgoingMessageInfoStateFlow.value.copy(password = password)
              )
              updateOutgoingMsgCount(activeAccount.email, activeAccount.accountType)
              Result.success(true)
            } catch (e: Exception) {
              try {
                messageEntity?.let {
                  if (messageEntity.id != null) {
                    roomDatabase.msgDao().deleteSuspend(messageEntity)
                    OutgoingMessageInfoManager.deleteOutgoingMessageInfo(
                      context = getApplication(),
                      messageId = requireNotNull(messageEntity.id),
                    )
                  }
                }
              } catch (e: Exception) {
                e.printStackTrace()
              }
              Result.exception(e)
            }
          }
        }
    }
  }

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
    outgoingMessageInfoMutableStateFlow.update {
      outgoingMessageInfo.copy(timestamp = System.currentTimeMillis())
    }
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

  fun callLookUpForMissedPubKeys() {
    viewModelScope.launch {
      allRecipients.forEach { entry ->
        recipientLookUpManager.enqueue(entry.value)
      }
    }
  }

  fun callLookUpForRecipientIfNeeded(email: String?) {
    viewModelScope.launch {
      allRecipients.entries
        .firstOrNull { it.key.equals(email?.lowercase(), ignoreCase = true) }
        ?.value?.let { recipientLookUpManager.enqueue(it) }
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

  private suspend fun updateOutgoingMsgCount(
    email: String,
    accountType: String?
  ) {
    val outgoingMsgCount = roomDatabase.msgDao().getOutboxMsgsSuspend(email).size
    val outboxLabel =
      roomDatabase.labelDao().getLabelSuspend(email, accountType, JavaEmailConstants.FOLDER_OUTBOX)

    outboxLabel?.let {
      roomDatabase.labelDao().updateSuspend(it.copy(messagesTotal = outgoingMsgCount))
    }
  }

  class RecipientLookUpManager(
    private val application: Application,
    private val roomDatabase: FlowCryptRoomDatabase,
    private val viewModelScope: CoroutineScope,
    private val updateListener: (recipientInfo: RecipientInfo) -> Unit
  ) {
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

    companion object {
      const val PARALLELISM_COUNT = 10
    }
  }
}
