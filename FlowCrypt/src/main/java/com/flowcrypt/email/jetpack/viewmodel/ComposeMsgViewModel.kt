/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import android.content.ContentResolver
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.extensions.kotlin.isValidEmail
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.ui.adapter.RecipientChipRecyclerViewAdapter.RecipientItem
import com.flowcrypt.email.util.RecipientLookUpManager
import jakarta.mail.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InvalidObjectException

/**
 * @author Denys Bondarenko
 */
class ComposeMsgViewModel(isCandidateToEncrypt: Boolean, application: Application) :
  AccountViewModel(application) {
  private val recipientLookUpManager = RecipientLookUpManager(
    application = application,
    roomDatabase = roomDatabase
  ) { recipientInfo ->
    val recipientItem = RecipientItem(
      recipientInfo.recipientType,
      recipientInfo.recipientWithPubKeys,
      recipientInfo.creationTime,
      recipientInfo.isUpdating,
      recipientInfo.isUpdateFailed,
      recipientInfo.isModifyingEnabled,
    )

    replaceRecipient(Message.RecipientType.TO, recipientItem)
    replaceRecipient(Message.RecipientType.CC, recipientItem)
    replaceRecipient(Message.RecipientType.BCC, recipientItem)
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

  private val recipientsToMutableStateFlow: MutableStateFlow<MutableMap<String, RecipientItem>> =
    MutableStateFlow(mutableMapOf())
  val recipientsToStateFlow: StateFlow<Map<String, RecipientItem>> =
    recipientsToMutableStateFlow.asStateFlow()

  private val recipientsCcMutableStateFlow: MutableStateFlow<MutableMap<String, RecipientItem>> =
    MutableStateFlow(mutableMapOf())
  val recipientsCcStateFlow: StateFlow<Map<String, RecipientItem>> =
    recipientsCcMutableStateFlow.asStateFlow()

  private val recipientsBccMutableStateFlow: MutableStateFlow<MutableMap<String, RecipientItem>> =
    MutableStateFlow(mutableMapOf())
  val recipientsBccStateFlow: StateFlow<Map<String, RecipientItem>> =
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
  val recipientsTo: Map<String, RecipientItem>
    get() = recipientsToStateFlow.value
  val recipientsCc: Map<String, RecipientItem>
    get() = recipientsCcStateFlow.value
  val recipientsBcc: Map<String, RecipientItem>
    get() = recipientsBccStateFlow.value
  val allRecipients: Map<String, RecipientItem>
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

  private val initSignatureMutableStateFlow: MutableStateFlow<Result<AccountEntity?>> =
    MutableStateFlow(Result.none())
  val initSignatureStateFlow: StateFlow<Result<AccountEntity?>> =
    initSignatureMutableStateFlow.asStateFlow()

  fun addAttachments(attachments: List<AttachmentInfo>) {
    attachmentsMutableStateFlow.update { existingAttachments ->
      existingAttachments.toMutableList().apply {
        addAll(attachments)
      }
    }
  }

  fun initSignature() {
    viewModelScope.launch {
      val activeAccount = roomDatabase.accountDao().getActiveAccountSuspend()
      initSignatureMutableStateFlow.value = Result.success(activeAccount)
    }
  }

  fun markSignatureUsed() {
    initSignatureMutableStateFlow.value = Result.none()
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
        val recipientItem = RecipientItem(recipientType, it)
        when (recipientType) {
          Message.RecipientType.TO -> recipientsToMutableStateFlow
          Message.RecipientType.CC -> recipientsCcMutableStateFlow
          Message.RecipientType.BCC -> recipientsBccMutableStateFlow
          else -> throw InvalidObjectException("unknown RecipientType: $recipientType")
        }.update { map ->
          map.toMutableMap().apply { put(normalizedEmail, RecipientItem(recipientType, it)) }
        }

        recipientLookUpManager.enqueue(recipientItem.toRecipientInfo())
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
        recipientLookUpManager.enqueue(entry.value.toRecipientInfo())
      }
    }
  }

  fun callLookUpForRecipientIfNeeded(email: String?) {
    viewModelScope.launch {
      allRecipients.entries
        .firstOrNull { it.key.equals(email?.lowercase(), ignoreCase = true) }
        ?.value?.let { recipientLookUpManager.enqueue(it.toRecipientInfo()) }
    }
  }

  private fun replaceRecipient(
    recipientType: Message.RecipientType,
    recipientItem: RecipientItem
  ) {
    viewModelScope.launch {
      val normalizedEmail = recipientItem.recipientWithPubKeys.recipient.email
      when (recipientType) {
        Message.RecipientType.TO -> recipientsToMutableStateFlow
        Message.RecipientType.CC -> recipientsCcMutableStateFlow
        Message.RecipientType.BCC -> recipientsBccMutableStateFlow
        else -> throw InvalidObjectException("unknown RecipientType: $recipientType")
      }.update { map ->
        map.toMutableMap().apply { replace(normalizedEmail, recipientItem) }
      }
    }
  }
}
