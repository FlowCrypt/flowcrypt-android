/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.org.bouncycastle.openpgp

import com.flowcrypt.email.security.SecurityUtils
import org.bouncycastle.openpgp.api.OpenPGPCertificate
import org.pgpainless.bouncycastle.extensions.encode
import java.io.IOException

/**
 * @author Denys Bondarenko
 */
@Throws(IOException::class)
fun OpenPGPCertificate.armor(hideArmorMeta: Boolean = false): String =
  SecurityUtils.armor(hideArmorMeta) { this.encode(it) }
