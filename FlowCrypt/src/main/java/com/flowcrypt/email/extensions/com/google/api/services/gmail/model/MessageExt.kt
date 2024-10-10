/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.com.google.api.services.gmail.model

import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.extensions.kotlin.asContentTypeOrNull
import com.flowcrypt.email.extensions.kotlin.asInternetAddresses
import com.google.api.services.gmail.model.Message
import jakarta.mail.internet.InternetAddress

/**
 * @author Denys Bondarenko
 */
fun Message.hasPgp(): Boolean {
  val baseContentType = payload?.headers?.firstOrNull {
    it.name == "Content-Type"
  }?.value?.asContentTypeOrNull()

  /**
   * based on https://datatracker.ietf.org/doc/html/rfc3156#section-5
   */
  val isOpenPGPMimeSigned = payload?.parts?.size == 2
      && "multipart/signed" == baseContentType?.baseType?.lowercase()
      && baseContentType.getParameter("protocol")?.lowercase() == "application/pgp-signature"
      && baseContentType.getParameter("micalg")?.lowercase()?.startsWith("pgp-") == true

  /**
   * based on https://datatracker.ietf.org/doc/html/rfc3156#section-4
   */
  val isOpenPGPMimeEncrypted = !isOpenPGPMimeSigned
      && payload?.parts?.size == 2
      && "multipart/encrypted" == baseContentType?.baseType?.lowercase()
      && baseContentType.getParameter("protocol")?.lowercase() == "application/pgp-encrypted"

  val hasEncryptedParts = payload?.parts?.any { it.hasPgp() } ?: false

  return EmailUtil.hasEncryptedData(snippet)
      || EmailUtil.hasSignedData(snippet)
      || isOpenPGPMimeSigned
      || isOpenPGPMimeEncrypted
      || hasEncryptedParts
}

fun Message.getRecipients(vararg recipientType: String): List<InternetAddress> {
  return payload?.headers?.firstOrNull { header ->
    header.name in recipientType
  }?.value?.asInternetAddresses()?.toList() ?: emptyList()
}

fun Message.getSubject(): String? {
  return payload?.headers?.firstOrNull { header ->
    header.name == "Subject"
  }?.value
}

fun Message.getInReplyTo(): String? {
  return payload?.headers?.firstOrNull { header ->
    header.name == JavaEmailConstants.HEADER_IN_REPLY_TO
  }?.value
}

fun Message.getMessageId(): String? {
  return payload?.headers?.firstOrNull { header ->
    header.name == JavaEmailConstants.HEADER_MESSAGE_ID
  }?.value
}

fun Message.isDraft(): Boolean {
  return labelIds?.contains(GmailApiHelper.LABEL_DRAFT) ?: false
}

fun Message.hasAttachments(): Boolean {
  return payload?.hasAttachments() ?: false
}