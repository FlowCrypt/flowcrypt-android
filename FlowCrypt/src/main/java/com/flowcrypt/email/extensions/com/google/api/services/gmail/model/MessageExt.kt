/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.com.google.api.services.gmail.model

import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.extensions.kotlin.asContentTypeOrNull
import com.google.api.services.gmail.model.Message

/**
 * @author Denys Bondarenko
 */
fun Message.isEncrypted(): Boolean {
  val baseContentType = payload?.headers?.firstOrNull {
    it.name == "Content-Type"
  }?.value?.asContentTypeOrNull()

  /**
   * based on https://datatracker.ietf.org/doc/html/rfc3156#section-4
   */
  val isPgpMime = payload?.parts?.size == 2
      && "multipart/encrypted" == baseContentType?.baseType?.lowercase()
      && baseContentType.getParameter("protocol")?.lowercase() == "application/pgp-encrypted"

  val hasEncryptedParts = payload?.parts?.any { it.isEncrypted() } ?: false

  return EmailUtil.hasEncryptedData(snippet) || isPgpMime || hasEncryptedParts
}