/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.com.google.api.services.gmail.model

import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.FoldersManager
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailApiHelper
import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.extensions.kotlin.asContentTypeOrNull
import com.flowcrypt.email.extensions.kotlin.asInternetAddresses
import com.google.api.services.gmail.model.Message
import com.google.api.services.gmail.model.MessagePartHeader
import jakarta.mail.internet.InternetAddress

/**
 * @author Denys Bondarenko
 */
fun Message?.hasPgp(): Boolean {
  val baseContentType = this?.payload?.headers?.firstOrNull {
    it?.name == "Content-Type"
  }?.value?.asContentTypeOrNull()

  /**
   * based on https://datatracker.ietf.org/doc/html/rfc3156#section-5
   */
  val isOpenPGPMimeSigned = this?.payload?.parts?.size == 2
      && "multipart/signed" == baseContentType?.baseType?.lowercase()
      && baseContentType.getParameter("protocol")?.lowercase() == "application/pgp-signature"
      && baseContentType.getParameter("micalg")?.lowercase()?.startsWith("pgp-") == true

  /**
   * based on https://datatracker.ietf.org/doc/html/rfc3156#section-4
   */
  val isOpenPGPMimeEncrypted = !isOpenPGPMimeSigned
      && this?.payload?.parts?.size == 2
      && "multipart/encrypted" == baseContentType?.baseType?.lowercase()
      && baseContentType.getParameter("protocol")?.lowercase() == "application/pgp-encrypted"

  val hasEncryptedParts = this?.payload?.parts?.any { it.hasPgp() } == true

  return EmailUtil.hasEncryptedData(this?.snippet)
      || EmailUtil.hasSignedData(this?.snippet)
      || isOpenPGPMimeSigned
      || isOpenPGPMimeEncrypted
      || hasEncryptedParts
}

fun Message?.getRecipients(vararg recipientType: String): List<InternetAddress> {
  return this?.payload?.headers?.firstOrNull { header ->
    header?.name in recipientType
  }?.value?.asInternetAddresses()?.toList() ?: emptyList()
}

fun Message?.getSubject(): String? {
  return this?.payload?.headers?.firstOrNull { header ->
    header?.name == "Subject"
  }?.value
}

fun Message?.getInReplyTo(): String? {
  return this?.payload?.headers?.firstOrNull { header ->
    header?.name == JavaEmailConstants.HEADER_IN_REPLY_TO
  }?.value
}

fun Message?.getMessageId(): String? {
  return this?.payload?.headers?.firstOrNull { header ->
    header?.name == JavaEmailConstants.HEADER_MESSAGE_ID
  }?.value
}

fun Message?.isDraft(): Boolean {
  return this?.labelIds?.contains(GmailApiHelper.LABEL_DRAFT) == true
}

fun Message?.hasAttachments(): Boolean {
  return this?.payload?.hasAttachments() == true
}

fun Message?.filterHeadersWithName(name: String): List<MessagePartHeader> {
  return this?.payload?.headers?.filter { header -> header?.name == name } ?: emptyList()
}

fun Message?.containsLabel(localFolder: LocalFolder?): Boolean? {
  return this?.labelIds?.contains(localFolder?.fullName)
}

fun Message?.isTrashed(): Boolean {
  return this?.labelIds?.contains(GmailApiHelper.LABEL_TRASH) == true
}

fun Message?.isSent(): Boolean {
  return this?.labelIds?.contains(GmailApiHelper.LABEL_SENT) == true
}

fun Message?.canBeUsed(localFolder: LocalFolder?): Boolean {
  return if (localFolder?.getFolderType() == FoldersManager.FolderType.TRASH) {
    isTrashed() == true
  } else {
    isTrashed().not() != false
  }
}