/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.com.google.api.services.gmail.model

import com.google.api.services.gmail.model.MessagePart
import org.apache.commons.io.FilenameUtils

/**
 * @author Denys Bondarenko
 */
/**
 * This checking is based only on comparing file extensions.
 * It doesn't check the file content to be sure 100%
 */
fun MessagePart.hasPgp(): Boolean {
  return parts?.any { it.hasPgp() } ?: false
      || FilenameUtils.getExtension(filename)?.lowercase() in arrayOf("asc", "pgp", "gpg", "key")
}