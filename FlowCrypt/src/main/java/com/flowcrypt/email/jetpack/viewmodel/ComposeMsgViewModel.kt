/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys
import com.flowcrypt.email.model.MessageEncryptionType
import jakarta.mail.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.io.InvalidObjectException

/**
 * @author Denis Bondarenko
 *         Date: 12/23/21
 *         Time: 10:44 AM
 *         E-mail: DenBond7@gmail.com
 */
class ComposeMsgViewModel(isCandidateToEncrypt: Boolean, application: Application) :
  BaseAndroidViewModel(application) {
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
  private val recipientsTo = mutableMapOf<String, Recipient>()
  private val recipientsCc = mutableMapOf<String, Recipient>()
  private val recipientsBcc = mutableMapOf<String, Recipient>()

  private val recipientsToMutableStateFlow: MutableStateFlow<List<Recipient>> =
    MutableStateFlow(emptyList())
  val recipientsToStateFlow: StateFlow<List<Recipient>> =
    recipientsToMutableStateFlow.asStateFlow()
  private val recipientsCcMutableStateFlow: MutableStateFlow<List<Recipient>> =
    MutableStateFlow(emptyList())
  val recipientsCcStateFlow: StateFlow<List<Recipient>> =
    recipientsCcMutableStateFlow.asStateFlow()
  private val recipientsBccMutableStateFlow: MutableStateFlow<List<Recipient>> =
    MutableStateFlow(emptyList())
  val recipientsBccStateFlow: StateFlow<List<Recipient>> =
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
  val recipientWithPubKeysTo: List<Recipient>
    get() = recipientsTo.values.toList()
  val recipientWithPubKeysCc: List<Recipient>
    get() = recipientsCc.values.toList()
  val recipientWithPubKeysBcc: List<Recipient>
    get() = recipientsBcc.values.toList()
  val recipientWithPubKeys: List<Recipient>
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
      else -> throw InvalidObjectException(
        "unknown RecipientType: $recipientType"
      )
    }

    existingRecipients.clear()
    existingRecipients.putAll(
      list.associateBy(
        { it.recipient.email },
        { Recipient(recipientType, it) })
    )

    notifyDataChanges(recipientType, existingRecipients)
  }

  fun removeRecipient(
    recipientType: Message.RecipientType,
    recipientWithPubKeys: RecipientWithPubKeys
  ) {
    val existingRecipients = when (recipientType) {
      Message.RecipientType.TO -> recipientsTo
      Message.RecipientType.CC -> recipientsCc
      Message.RecipientType.BCC -> recipientsBcc
      else -> throw InvalidObjectException(
        "unknown RecipientType: $recipientType"
      )
    }

    existingRecipients.remove(recipientWithPubKeys.recipient.email)
    notifyDataChanges(recipientType, existingRecipients)
  }

  private fun notifyDataChanges(
    recipientType: Message.RecipientType,
    recipients: MutableMap<String, Recipient>
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

  data class Recipient(
    val recipientType: Message.RecipientType,
    val recipientWithPubKeys: RecipientWithPubKeys,
    val creationTime: Long = System.currentTimeMillis()
  )
}
