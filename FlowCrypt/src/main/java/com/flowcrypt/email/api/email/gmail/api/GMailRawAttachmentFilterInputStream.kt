/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.api.email.gmail.api

import java.io.InputStream

/**
 * @author Denys Bondarenko
 */
class GMailRawAttachmentFilterInputStream(inputStream: InputStream) :
  GMailRawResponseFilterInputStream(inputStream, JSON_PREFIX_LENGTH) {
  private companion object {
    const val JSON_PREFIX_LENGTH = 9L
  }
}
