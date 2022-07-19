/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.retrofit.ApiRepository
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.ui.adapter.RecipientChipRecyclerViewAdapter.RecipientInfo
import jakarta.mail.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
  private val recipientLookUpManager = RecipientLookUpManager(roomDatabase, viewModelScope) {
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

  fun switchMessageEncryptionType(messageEncryptionType: MessageEncryptionType) {
    messageEncryptionTypeMutableStateFlow.value = messageEncryptionType
  }

  fun setWebPortalPassword(webPortalPassword: CharSequence = "") {
    webPortalPasswordMutableStateFlow.value = webPortalPassword
  }

  fun replaceRecipient(
    recipientType: Message.RecipientType,
    recipientInfo: RecipientInfo
  ) {
    val normalizedEmail = recipientInfo.recipientWithPubKeys.recipient.email
    when (recipientType) {
      Message.RecipientType.TO -> recipientsToMutableStateFlow
      Message.RecipientType.CC -> recipientsCcMutableStateFlow
      Message.RecipientType.BCC -> recipientsBccMutableStateFlow
      else -> throw InvalidObjectException("unknown RecipientType: $recipientType")
    }.update { map ->
      map.toMutableMap().apply { put(normalizedEmail, recipientInfo) }
    }
  }

  fun replaceRecipients(recipientType: Message.RecipientType, list: List<RecipientWithPubKeys>) {
    when (recipientType) {
      Message.RecipientType.TO -> recipientsToMutableStateFlow
      Message.RecipientType.CC -> recipientsCcMutableStateFlow
      Message.RecipientType.BCC -> recipientsBccMutableStateFlow
      else -> throw InvalidObjectException("unknown RecipientType: $recipientType")
    }.update {
      list.associateBy(
        { it.recipient.email },
        { RecipientInfo(recipientType, it) }).toMutableMap()
    }
  }

  fun addRecipientByEmail(
    recipientType: Message.RecipientType,
    email: CharSequence
  ) {
    viewModelScope.launch {
      val normalizedEmail = email.toString().lowercase()
      val existingRecipient = roomDatabase.recipientDao()
        .getRecipientWithPubKeysByEmailSuspend(normalizedEmail)

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

  class RecipientLookUpManager(
    private val roomDatabase: FlowCryptRoomDatabase,
    private val viewModelScope: CoroutineScope,
    private val updateListener: (recipientInfo: RecipientInfo) -> Unit
  ) {
    private val apiRepository: ApiRepository = FlowcryptApiRepository()
    private val lookUpCandidates = mutableMapOf<String, RecipientInfo>()
    private val recipientsSessionCache = ConcurrentHashMap<String, RecipientWithPubKeys>()

    suspend fun enqueue(recipientInfo: RecipientInfo) = withContext(Dispatchers.IO) {
      val email = recipientInfo.recipientWithPubKeys.recipient.email
      if (recipientsSessionCache.containsKey(email)) {
        updateListener.invoke(
          recipientInfo.copy(
            isUpdating = false,
            recipientWithPubKeys = requireNotNull(recipientsSessionCache[email])
          )
        )
      } else {
        lookUpCandidates[email] = recipientInfo
        val existingValue = roomDatabase.recipientDao().getRecipientWithPubKeysByEmailSuspend(email)
        if (existingValue != null) {
          lookUpCandidates.remove(email)
          recipientsSessionCache[email] = existingValue
          updateListener.invoke(
            recipientInfo.copy(
              isUpdating = false,
              recipientWithPubKeys = existingValue
            )
          )
        }
      }
    }

    fun dequeue(email: String) {
      lookUpCandidates.remove(email)
    }
  }
}
