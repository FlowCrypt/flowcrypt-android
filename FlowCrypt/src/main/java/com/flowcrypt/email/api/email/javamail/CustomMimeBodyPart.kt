/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.javamail

import jakarta.mail.internet.InternetHeaders
import jakarta.mail.internet.MimeBodyPart
import java.io.InputStream

/**
 * @author Denys Bondarenko
 */
class CustomMimeBodyPart(inputStream: InputStream, headers: InternetHeaders) :
  MimeBodyPart(headers, null) {
  init {
    contentStream = inputStream
  }
}
