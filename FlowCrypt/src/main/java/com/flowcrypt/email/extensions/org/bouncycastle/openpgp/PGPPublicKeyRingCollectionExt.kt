/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.org.bouncycastle.openpgp

import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.security.pgp.PgpArmor
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import java.io.IOException

/**
 * @author Denys Bondarenko
 */
@Throws(IOException::class)
fun PGPPublicKeyRingCollection.armor(
  hideArmorMeta: Boolean = false,
  headers: List<Pair<String, String>>? = PgpArmor.FLOWCRYPT_HEADERS
): String {
  return SecurityUtils.armor(hideArmorMeta, headers) { this.encode(it) }
}
