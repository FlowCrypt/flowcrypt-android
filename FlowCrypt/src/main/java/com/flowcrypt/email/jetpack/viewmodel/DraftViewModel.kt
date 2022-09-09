/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.viewmodel

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.InitializationData
import com.flowcrypt.email.api.email.model.OutgoingMessageInfo
import jakarta.mail.Message
import jakarta.mail.Session
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Properties
import java.util.concurrent.TimeUnit

/**
 * @author Denis Bondarenko
 *         Date: 9/2/22
 *         Time: 3:45 PM
 *         E-mail: DenBond7@gmail.com
 */
class DraftViewModel(application: Application) : AccountViewModel(application) {
  private var draftId: String? = null
  private var lastMsgText: String = ""
  private var lastMsgSubject: String = ""
  private val lastToRecipients: MutableSet<String> = mutableSetOf()
  private val lastCcRecipients: MutableSet<String> = mutableSetOf()
  private val lastBccRecipients: MutableSet<String> = mutableSetOf()

  val draftRepeatableCheckingFlow: Flow<Long> = flow {
    while (viewModelScope.isActive) {
      delay(DELAY_TIMEOUT)
      emit(System.currentTimeMillis())
    }
  }.flowOn(Dispatchers.Default)

  fun processDraft(currentTimeMillis: Long, currentOutgoingMessageInfo: OutgoingMessageInfo) {
    viewModelScope.launch {
      var isSavingDraftNeeded = false
      val currentToRecipients = currentOutgoingMessageInfo.toRecipients.map { internetAddress ->
        internetAddress.address.lowercase()
      }.toSet()
      val currentCcRecipients = currentOutgoingMessageInfo.ccRecipients?.map { internetAddress ->
        internetAddress.address.lowercase()
      }?.toSet() ?: emptySet()
      val currentBccRecipients =
        currentOutgoingMessageInfo.bccRecipients?.map { internetAddress ->
          internetAddress.address.lowercase()
        }?.toSet() ?: emptySet()

      if (currentOutgoingMessageInfo.msg != lastMsgText
        || currentOutgoingMessageInfo.subject != lastMsgSubject
        || currentToRecipients != lastToRecipients
        || currentCcRecipients != lastCcRecipients
        || currentBccRecipients != lastBccRecipients
      ) {
        isSavingDraftNeeded = true
      }

      lastMsgText = currentOutgoingMessageInfo.msg ?: ""
      lastMsgSubject = currentOutgoingMessageInfo.subject
      lastToRecipients.clear()
      lastToRecipients.addAll(currentToRecipients)
      lastCcRecipients.clear()
      lastCcRecipients.addAll(currentCcRecipients)
      lastBccRecipients.clear()
      lastBccRecipients.addAll(currentBccRecipients)

      if (isSavingDraftNeeded) {
        uploadOrUpdateDraftOnRemoteServer(currentOutgoingMessageInfo)
      }
    }
  }

  private suspend fun uploadOrUpdateDraftOnRemoteServer(outgoingMessageInfo: OutgoingMessageInfo) =
    withContext(Dispatchers.IO) {
      try {
        val activeAccount = roomDatabase.accountDao().getActiveAccountSuspend()
        if (activeAccount == null) {
          return@withContext
        }

        draftId = GmailApiHelper.uploadDraft(
          context = getApplication(),
          account = activeAccount,
          mimeMessage = prepareMimeMessage(outgoingMessageInfo),
          draftId = draftId
        )
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }

  private fun prepareMimeMessage(outgoingMessageInfo: OutgoingMessageInfo): Message {
    return MimeMessage(Session.getInstance(Properties())).apply {
      subject = outgoingMessageInfo.subject
      setFrom(outgoingMessageInfo.from)
      setRecipients(Message.RecipientType.TO, outgoingMessageInfo.toRecipients.toTypedArray())
      setRecipients(Message.RecipientType.CC, outgoingMessageInfo.ccRecipients?.toTypedArray())
      setRecipients(Message.RecipientType.BCC, outgoingMessageInfo.bccRecipients?.toTypedArray())
      setContent(MimeMultipart().apply {
        addBodyPart(MimeBodyPart().apply {
          setText(outgoingMessageInfo.msg)
        })
      })
    }
  }

  fun setupWithInitializationData(
    initializationData: InitializationData
  ) {
    lastMsgText = initializationData.body ?: ""
    lastMsgSubject = initializationData.subject ?: ""
    lastToRecipients.addAll(initializationData.toAddresses.map { it.lowercase() })
    lastCcRecipients.addAll(initializationData.ccAddresses.map { it.lowercase() })
    lastBccRecipients.addAll(initializationData.bccAddresses.map { it.lowercase() })
  }

  companion object {
    val DELAY_TIMEOUT = TimeUnit.SECONDS.toMillis(5)
  }
}
