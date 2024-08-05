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
fun Thread.getUniqueRecipients(): List<InternetAddress> {
  return mutableListOf<InternetAddress>().apply {
    val filteredHeaders = messages?.flatMap { message ->
      message?.payload?.headers?.filter { header ->
        //need to check this line and test
        header.name in listOf(
          "From",
          "To"
        )//maybe need to add Cc. need to check
      } ?: emptyList()
    }

    val mapOfUniqueRecipients = mutableMapOf<String, String>()
    filteredHeaders?.forEach { header ->
      header.value.asInternetAddresses().forEach { internetAddress ->
        val address = internetAddress.address.lowercase()

        if (!mapOfUniqueRecipients.contains(address)
          || mapOfUniqueRecipients[address].isNullOrEmpty()
        ) {
          add(internetAddress)
          mapOfUniqueRecipients[address] = internetAddress.personal
        }
      }
    }
  }
}