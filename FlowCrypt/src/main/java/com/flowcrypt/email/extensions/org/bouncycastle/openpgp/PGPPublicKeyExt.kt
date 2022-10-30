/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.extensions.org.bouncycastle.openpgp

import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.security.pgp.PgpArmor
import org.bouncycastle.openpgp.PGPPublicKey
import java.io.IOException

/**
 * @author Denis Bondarenko
 *         Date: 4/20/22
 *         Time: 10:53 AM
 *         E-mail: DenBond7@gmail.com
 */
@Throws(IOException::class)
fun PGPPublicKey.armor(
  hideArmorMeta: Boolean = false,
  headers: List<Pair<String, String>>? = PgpArmor.FLOWCRYPT_HEADERS
): String {
  return SecurityUtils.armor(hideArmorMeta, headers) { this.encode(it) }
}
