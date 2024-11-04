/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.com.google.api.services.gmail.model

import android.content.Context
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.extensions.kotlin.asInternetAddresses
import com.google.api.services.gmail.model.Thread
import jakarta.mail.internet.InternetAddress

/**
 * @author Denys Bondarenko
 */
fun Thread.getUniqueRecipients(account: String): List<InternetAddress> {
  return mutableListOf<InternetAddress>().apply {
    val filteredHeaders = messages?.flatMap { message ->
      if (message?.payload?.headers?.any {
          it.name in listOf(
            "From",
            "To",
            "Cc",
            "Delivered-To"
          ) && it.value.contains(account, true)
        } == true) {
        message.payload?.headers?.filter { header ->
          header.name == "From"
        } ?: emptyList()
      } else emptyList()
    }

    val mapOfUniqueRecipients = mutableMapOf<String, InternetAddress>()
    filteredHeaders?.forEach { header ->
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

fun Thread.getUniqueLabelsSet(): Set<String> {
  return messages?.flatMap { message ->
    message.labelIds ?: emptyList()
  }?.toSortedSet() ?: emptySet()
}

fun Thread.getDraftsCount(): Int {
  return messages?.filter { it.labelIds.contains(GmailApiHelper.LABEL_DRAFT) }?.size ?: 0
}

fun Thread.hasUnreadMessages(): Boolean {
  return messages?.any { message ->
    message.labelIds?.contains(GmailApiHelper.LABEL_UNREAD) == true
  } ?: false
}

fun Thread.hasAttachments(): Boolean {
  return messages?.any { message ->
    message.hasAttachments()
  } ?: false
}

fun Thread.hasPgp(): Boolean {
  return messages?.any { message ->
    message.hasPgp()
  } ?: false
}

fun Thread.extractSubject(context: Context, receiverEmail: String): String {
  return messages?.getOrNull(0)?.takeIf { message ->
    message.getRecipients("From").any { internetAddress ->
      internetAddress.address.equals(receiverEmail, true)
    } || (messages?.size ?: 0) == 1
  }?.getSubject()
    ?: messages.firstOrNull { message ->
      message.getRecipients("From").any { internetAddress ->
        internetAddress.address.equals(receiverEmail, true)
      }
    }?.getSubject()
    ?: messages?.getOrNull(0)?.getSubject()
    ?: context.getString(R.string.no_subject)
}