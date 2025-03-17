/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.com.google.api.services.gmail.model

import android.content.Context
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.gmail.GmailApiHelper.Companion.LABEL_DRAFT
import com.flowcrypt.email.api.email.gmail.model.GmailThreadInfo
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.kotlin.asInternetAddresses
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.Thread
import jakarta.mail.internet.InternetAddress

/**
 * @author Denys Bondarenko
 */
fun Thread.getUniqueRecipients(account: String, localFolder: LocalFolder?): List<InternetAddress> {
  return mutableListOf<InternetAddress>().apply {
    val filteredMessages = messages?.filter {
      it.canBeUsed(localFolder)
    }?.takeIf {
      it.isNotEmpty()
    } ?: return@apply
    val fromHeaderName = "From"

    val filteredHeaders = if (filteredMessages.size > 1) {
      //if we have more than one message in a conversation,
      //firstly we will try to filter only active recipients
      filteredMessages.flatMap { message ->
        val listOfAcceptedHeaders = listOf(
          fromHeaderName,
          "To",
          "Cc",
          "Delivered-To"
        )
        if (message.payload?.headers?.any {
            it.name in listOfAcceptedHeaders && it.value.contains(account, true)
          } == true) {
          message.filterHeadersWithName(fromHeaderName)
        } else emptyList()
      }.ifEmpty {
        //otherwise we will use all recipients
        filteredMessages.flatMap { it.filterHeadersWithName(fromHeaderName) }
      }
    } else {
      filteredMessages.first().filterHeadersWithName(fromHeaderName)
    }

    val mapOfUniqueRecipients = mutableMapOf<String, InternetAddress>()
    filteredHeaders.forEach { header ->
      header.value.asInternetAddresses().forEach { internetAddress ->
        val address = internetAddress.address.lowercase()

        if (!mapOfUniqueRecipients.contains(address)
          || mapOfUniqueRecipients[address]?.personal.isNullOrEmpty()
        ) {
          mapOfUniqueRecipients[address] = internetAddress
        }
      }
    }

    addAll(mapOfUniqueRecipients.values)
  }
}

fun Thread.getUniqueLabelsSet(localFolder: LocalFolder?): Set<String> {
  return messages?.filter {
    it.canBeUsed(localFolder)
  }?.flatMap {
    it.labelIds ?: emptyList()
  }?.toSortedSet() ?: emptySet()
}

fun Thread.getDraftsCount(localFolder: LocalFolder?): Int {
  return messages?.filter {
    it.canBeUsed(localFolder) && it.labelIds.contains(LABEL_DRAFT)
  }?.size ?: 0
}

fun Thread.hasUnreadMessages(localFolder: LocalFolder?): Boolean {
  return messages?.filter {
    it.canBeUsed(localFolder)
  }?.any {
    it.labelIds?.contains(GmailApiHelper.LABEL_UNREAD) == true
  } == true
}

fun Thread.hasAttachments(localFolder: LocalFolder?): Boolean {
  return messages?.filter { it.canBeUsed(localFolder) }?.any { it.hasAttachments() } == true
}

fun Thread.hasPgp(localFolder: LocalFolder?): Boolean {
  return messages?.filter { it.canBeUsed(localFolder) }?.any { it.hasPgp() } == true
}

fun Thread.extractSubject(
  context: Context,
  receiverEmail: String,
  localFolder: LocalFolder?
): String {
  val filteredMessages = messages?.filter { it.canBeUsed(localFolder) }

  return filteredMessages?.getOrNull(0)?.takeIf { message ->
    (message.getRecipients("From").any { internetAddress ->
      internetAddress.address.equals(receiverEmail, true)
    } || (filteredMessages.size) == 1) && !message.isDraft()
  }?.getSubject()
    ?: filteredMessages?.firstOrNull { message ->
      message.getRecipients("From").any { internetAddress ->
        internetAddress.address.equals(receiverEmail, true)
      } && !message.isDraft()
    }?.getSubject()
    ?: filteredMessages?.getOrNull(0)?.getSubject()
    ?: context.getString(R.string.no_subject)
}

fun Thread.filteredMessages(localFolder: LocalFolder?): List<Message> {
  return messages?.filter { it.canBeUsed(localFolder) } ?: emptyList()
}

fun Thread.toThreadInfo(
  context: Context,
  accountEntity: AccountEntity,
  localFolder: LocalFolder? = null
): GmailThreadInfo {
  val receiverEmail = accountEntity.email
  val lastMessage = messages?.lastOrNull {
    !it.labelIds.contains(LABEL_DRAFT) && it.canBeUsed(localFolder)
  } ?: messages?.first()
  val gmailThreadInfo = GmailThreadInfo(
    id = id,
    lastMessage = requireNotNull(lastMessage),
    messagesCount = messages?.filter { it.canBeUsed(localFolder) }?.size ?: 0,
    draftsCount = getDraftsCount(localFolder),
    recipients = getUniqueRecipients(receiverEmail, localFolder),
    subject = extractSubject(context, receiverEmail, localFolder),
    labels = getUniqueLabelsSet(localFolder),
    hasAttachments = hasAttachments(localFolder),
    hasPgpThings = hasPgp(localFolder),
    hasUnreadMessages = hasUnreadMessages(localFolder)
  )
  return gmailThreadInfo
}