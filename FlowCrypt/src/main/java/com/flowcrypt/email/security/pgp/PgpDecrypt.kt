/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.pgp

import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.pgpainless.PGPainless
import org.pgpainless.key.protection.SecretKeyRingProtector
import java.io.InputStream
import java.io.OutputStream

/**
 * @author Denis Bondarenko
 *         Date: 5/11/21
 *         Time: 2:10 PM
 *         E-mail: DenBond7@gmail.com
 */
object PgpDecrypt {

  fun decrypt(
    srcInputStream: InputStream,
    destOutputStream: OutputStream,
    pgpSecretKeyRingCollection: PGPSecretKeyRingCollection,
    protector: SecretKeyRingProtector
  ) {
    srcInputStream.use { srcStream ->
      destOutputStream.use { outStream ->
        val decryptionStream = PGPainless.decryptAndOrVerify()
          .onInputStream(srcStream)
          .decryptWith(protector, pgpSecretKeyRingCollection)
          .doNotVerify()
          .build()

        decryptionStream.use { it.copyTo(outStream) }
      }
    }
  }
}