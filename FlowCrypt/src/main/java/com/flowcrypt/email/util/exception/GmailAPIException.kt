/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import java.io.IOException

/**
 * @author Denys Bondarenko
 */
class GmailAPIException(cause: GoogleJsonResponseException?) : IOException(cause) {
  override val message: String?
    get() = (cause as? GoogleJsonResponseException)?.details?.message

  val code: Int = cause?.details?.code ?: -1

  companion object{
    const val ENTITY_NOT_FOUND = "Requested entity was not found."
  }
}
