/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.flowcrypt.email.api.retrofit.response.model.DecryptError
import com.flowcrypt.email.api.retrofit.response.model.DecryptErrorDetails
import com.flowcrypt.email.security.pgp.PgpDecryptAndOrVerify

/**
 * @author Denys Bondarenko
 */
class DecryptionException(
  val decryptionErrorType: PgpDecryptAndOrVerify.DecryptionErrorType,
  val e: Exception,
  val fingerprints: List<String> = emptyList()
) : FlowCryptException(e) {

  override fun toString(): String {
    return super.toString() + ", DecryptionErrorType = " + decryptionErrorType
  }

  fun toDecryptError(): DecryptError {
    return DecryptError(DecryptErrorDetails(decryptionErrorType, e.message), fingerprints, true)
  }
}
