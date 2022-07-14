/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.retrofit.ApiRepository
import com.flowcrypt.email.api.retrofit.FlowcryptApiRepository
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.model.MessageEncryptionType
import com.flowcrypt.email.ui.adapter.RecipientChipRecyclerViewAdapter.RecipientInfo
import jakarta.mail.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.io.InvalidObjectException

/**
 * @author Denis Bondarenko
 *         Date: 12/23/21
 *         Time: 10:44 AM
 *         E-mail: DenBond7@gmail.com
 */
class ComposeMsgViewModel(isCandidateToEncrypt: Boolean, application: Application) :
  RoomBasicViewModel(application) {
  private val apiRepository: ApiRepository = FlowcryptApiRepository()
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

  //session cache for recipients
  private val recipientsTo = mutableMapOf<String, RecipientInfo>()
  private val recipientsCc = mutableMapOf<String, RecipientInfo>()
  private val recipientsBcc = mutableMapOf<String, RecipientInfo>()

  private val recipientsToMutableStateFlow: MutableStateFlow<List<RecipientInfo>> =
    MutableStateFlow(emptyList())
  val recipientsToStateFlow: StateFlow<List<RecipientInfo>> =
    recipientsToMutableStateFlow.asStateFlow()
  private val recipientsCcMutableStateFlow: MutableStateFlow<List<RecipientInfo>> =
    MutableStateFlow(emptyList())
  val recipientsCcStateFlow: StateFlow<List<RecipientInfo>> =
    recipientsCcMutableStateFlow.asStateFlow()
  private val recipientsBccMutableStateFlow: MutableStateFlow<List<RecipientInfo>> =
    MutableStateFlow(emptyList())
  val recipientsBccStateFlow: StateFlow<List<RecipientInfo>> =
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
  val recipientWithPubKeysTo: List<RecipientInfo>
    get() = recipientsTo.values.toList()
  val recipientWithPubKeysCc: List<RecipientInfo>
    get() = recipientsCc.values.toList()
  val recipientWithPubKeysBcc: List<RecipientInfo>
    get() = recipientsBcc.values.toList()
  val recipientWithPubKeys: List<RecipientInfo>
    get() = recipientWithPubKeysTo + recipientWithPubKeysCc + recipientWithPubKeysBcc

  fun switchMessageEncryptionType(messageEncryptionType: MessageEncryptionType) {
    messageEncryptionTypeMutableStateFlow.value = messageEncryptionType
  }

  fun setWebPortalPassword(webPortalPassword: CharSequence = "") {
    webPortalPasswordMutableStateFlow.value = webPortalPassword
  }

  fun replaceRecipients(recipientType: Message.RecipientType, list: List<RecipientWithPubKeys>) {
    val existingRecipients = when (recipientType) {
      Message.RecipientType.TO -> recipientsTo
      Message.RecipientType.CC -> recipientsCc
      Message.RecipientType.BCC -> recipientsBcc
      else -> throw InvalidObjectException("unknown RecipientType: $recipientType")
    }

    existingRecipients.clear()
    existingRecipients.putAll(
      list.associateBy(
        { it.recipient.email },
        { RecipientInfo(recipientType, it) })
    )

    notifyDataChanges(recipientType, existingRecipients)
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
        val existingRecipients = when (recipientType) {
          Message.RecipientType.TO -> recipientsTo
          Message.RecipientType.CC -> recipientsCc
          Message.RecipientType.BCC -> recipientsBcc
          else -> throw InvalidObjectException("unknown RecipientType: $recipientType")
        }

        existingRecipients[it.recipient.email] = RecipientInfo(recipientType, it)
        notifyDataChanges(recipientType, existingRecipients)
      }
    }
  }

  fun removeRecipient(
    recipientType: Message.RecipientType,
    recipientEmail: String
  ) {
    val normalizedEmail = recipientEmail.lowercase()

    val existingRecipients = when (recipientType) {
      Message.RecipientType.TO -> recipientsTo
      Message.RecipientType.CC -> recipientsCc
      Message.RecipientType.BCC -> recipientsBcc
      else -> throw InvalidObjectException("unknown RecipientType: $recipientType")
    }

    existingRecipients.remove(normalizedEmail)
    notifyDataChanges(recipientType, existingRecipients)
  }

  private fun notifyDataChanges(
    recipientType: Message.RecipientType,
    recipients: MutableMap<String, RecipientInfo>
  ) {
    when (recipientType) {
      Message.RecipientType.TO -> recipientsToMutableStateFlow
      Message.RecipientType.CC -> recipientsCcMutableStateFlow
      Message.RecipientType.BCC -> recipientsBccMutableStateFlow
      else -> throw InvalidObjectException(
        "Attempt to resolve unknown RecipientType: $recipientType"
      )
    }.value = recipients.values.toList()
  }
}
