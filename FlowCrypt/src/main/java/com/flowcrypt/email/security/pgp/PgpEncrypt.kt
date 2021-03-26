/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.pgp

import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.pgpainless.PGPainless
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * @author Denis Bondarenko
 *         Date: 3/16/21
 *         Time: 3:28 PM
 *         E-mail: DenBond7@gmail.com
 */
object PgpEncrypt {
  fun encryptMsg(msg: String, pubKeys: List<String>): String {
    val outputStreamForEncryptedSource = ByteArrayOutputStream()
    encrypt(
        srcInputStream = ByteArrayInputStream(msg.toByteArray()),
        destOutputStream = outputStreamForEncryptedSource,
        pubKeys = pubKeys,
        doArmor = true)
    return String(outputStreamForEncryptedSource.toByteArray())
  }

  fun encrypt(srcInputStream: InputStream, destOutputStream: OutputStream,
              pubKeys: List<String>, doArmor: Boolean = false) {
    val byteArrayInputStream = ByteArrayInputStream(pubKeys.joinToString(separator = "\n").toByteArray())
    val pgpPublicKeyRingCollection = byteArrayInputStream.use {
      ArmoredInputStream(it).use { armoredInputStream ->
        PGPainless.readKeyRing().publicKeyRingCollection(armoredInputStream)
      }
    }

    encrypt(srcInputStream, destOutputStream, pgpPublicKeyRingCollection, doArmor)
  }

  @Throws(IOException::class)
  fun encrypt(srcInputStream: InputStream, destOutputStream: OutputStream,
              pgpPublicKeyRingCollection: PGPPublicKeyRingCollection, doArmor: Boolean = false) {
    srcInputStream.use { srcStream ->
      destOutputStream.use { outStream ->
        val builder = PGPainless.encryptAndOrSign()
            .onOutputStream(outStream)
            .toRecipients(pgpPublicKeyRingCollection)
            .usingSecureAlgorithms()

        val out = if (doArmor) builder.doNotSign().asciiArmor() else builder.doNotSign().noArmor()
        out.use { encryptionStream ->
          srcStream.copyTo(encryptionStream)
        }
      }
    }
  }
}
