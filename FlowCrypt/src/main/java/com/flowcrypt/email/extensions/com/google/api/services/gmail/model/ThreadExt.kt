/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.com.google.api.services.gmail.model

import com.flowcrypt.email.extensions.kotlin.asInternetAddresses
import com.google.api.services.gmail.model.Thread
import jakarta.mail.internet.InternetAddress

/**
 * @author Denys Bondarenko
 */
//todo-denbond7 need to use ordering to show recipients in right order based on the conversation history
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