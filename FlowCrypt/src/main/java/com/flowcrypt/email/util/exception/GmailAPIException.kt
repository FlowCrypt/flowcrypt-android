/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.google.api.client.googleapis.json.GoogleJsonResponseException
import java.io.IOException

/**
 * @author Denis Bondarenko
 *         Date: 2/8/21
 *         Time: 7:49 PM
 *         E-mail: DenBond7@gmail.com
 */
class GmailAPIException(cause: GoogleJsonResponseException?) : IOException(cause) {
  override val message: String?
    get() = (cause as? GoogleJsonResponseException)?.details?.message
}
