/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.javamail

import java.io.InputStream
import javax.mail.internet.InternetHeaders
import javax.mail.internet.MimeBodyPart

/**
 * @author Denis Bondarenko
 *         Date: 2/20/21
 *         Time: 11:46 AM
 *         E-mail: DenBond7@gmail.com
 */
class CustomMimeBodyPart(inputStream: InputStream, headers: InternetHeaders) : MimeBodyPart(headers, null) {
  init {
    contentStream = inputStream
  }
}