/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.jakarta.mail

import jakarta.mail.Multipart
import jakarta.mail.internet.ContentType

/**
 * @author Denys Bondarenko
 */
fun Multipart.baseContentType(): String? {
  return try {
    ContentType(contentType).baseType
  } catch (e: Exception) {
    null
  }
}