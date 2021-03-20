/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.pgp

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.pgpainless.PGPainless

/**
 * @author Denis Bondarenko
 *         Date: 3/16/21
 *         Time: 3:28 PM
 *         E-mail: DenBond7@gmail.com
 */
object PgpEncrypt {
  @Throws(IOException::class)
  fun encryptFile(srcInputStream: InputStream, destOutputStream: OutputStream,
                  pgpPublicKeyRingCollection: PGPPublicKeyRingCollection) {
    srcInputStream.use { srcStream ->
      destOutputStream.use { outStream ->
        val builder = PGPainless.encryptAndOrSign()
            .onOutputStream(outStream)
            .toRecipients(pgpPublicKeyRingCollection)
            .usingSecureAlgorithms()

        builder.doNotSign().noArmor().use { encryptionStream ->
          srcStream.copyTo(encryptionStream)
        }
      }
    }
  }

  fun encryptFile(srcInputStream: InputStream, destOutputStream: OutputStream, pubKeys: List<String>) {
    val byteArrayInputStream = ByteArrayInputStream(pubKeys.joinToString(separator = "\n").toByteArray())
    val pgpPublicKeyRingCollection = byteArrayInputStream.use {
      ArmoredInputStream(it).use { armoredInputStream ->
        PGPainless.readKeyRing().publicKeyRingCollection(armoredInputStream)
      }
    }

    encryptFile(srcInputStream, destOutputStream, pgpPublicKeyRingCollection)
  }
}
