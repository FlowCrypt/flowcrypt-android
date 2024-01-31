/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.jakarta.mail

import com.flowcrypt.email.extensions.kotlin.asContentTypeOrNull
import jakarta.mail.Multipart
import jakarta.mail.internet.ContentType

/**
 * @author Denys Bondarenko
 */
fun Multipart.baseContentType(): String? {
  return contentType?.asContentTypeOrNull()?.baseType
}