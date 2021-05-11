/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.exception

import com.flowcrypt.email.security.pgp.PgpDecrypt

/**
 * @author Denis Bondarenko
 *         Date: 5/11/21
 *         Time: 6:51 PM
 *         E-mail: DenBond7@gmail.com
 */
class DecryptionException(
  val decryptionErrorType: PgpDecrypt.DecryptionErrorType,
  val e: Exception
) : FlowCryptException(e)