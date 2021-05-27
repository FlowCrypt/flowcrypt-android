/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.pgp

import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.pgpainless.PGPainless
import org.pgpainless.algorithm.DocumentSignatureType
import org.pgpainless.encryption_signing.EncryptionOptions
import org.pgpainless.encryption_signing.ProducerOptions
import org.pgpainless.encryption_signing.SigningOptions
import org.pgpainless.key.protection.SecretKeyRingProtector
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
  fun encryptAndOrSignMsg(msg: String,
                          pubKeys: List<String>,
                          prvKeys: List<String>? = null,
                          secretKeyRingProtector: SecretKeyRingProtector? = null): String {
    val outputStreamForEncryptedSource = ByteArrayOutputStream()
    encryptAndOrSign(
        srcInputStream = ByteArrayInputStream(msg.toByteArray()),
        destOutputStream = outputStreamForEncryptedSource,
        pubKeys = pubKeys,
        prvKeys = prvKeys,
        secretKeyRingProtector = secretKeyRingProtector,
        doArmor = true)
    return String(outputStreamForEncryptedSource.toByteArray())
  }

  fun encryptAndOrSign(srcInputStream: InputStream,
                       destOutputStream: OutputStream,
                       pubKeys: List<String>,
                       prvKeys: List<String>? = null,
                       secretKeyRingProtector: SecretKeyRingProtector? = null,
                       doArmor: Boolean = false) {
    val pubKeysStream = ByteArrayInputStream(pubKeys.joinToString(separator = "\n").toByteArray())
    val pgpPublicKeyRingCollection = pubKeysStream.use {
      ArmoredInputStream(it).use { armoredInputStream ->
        PGPainless.readKeyRing().publicKeyRingCollection(armoredInputStream)
      }
    }

    var pgpSecretKeyRingCollection: PGPSecretKeyRingCollection? = null
    if (prvKeys?.isNotEmpty() == true) {
      requireNotNull(secretKeyRingProtector)
      val prvKeysStream = ByteArrayInputStream(prvKeys.joinToString(separator = "\n").toByteArray())
      pgpSecretKeyRingCollection = prvKeysStream.use {
        ArmoredInputStream(it).use { armoredInputStream ->
          PGPainless.readKeyRing().secretKeyRingCollection(armoredInputStream)
        }
      }
    }

    encryptAndOrSign(
        srcInputStream = srcInputStream,
        destOutputStream = destOutputStream,
        pgpPublicKeyRingCollection = pgpPublicKeyRingCollection,
        pgpSecretKeyRingCollection = pgpSecretKeyRingCollection,
        secretKeyRingProtector = secretKeyRingProtector,
        doArmor = doArmor
    )
  }

  @Throws(IOException::class)
  fun encryptAndOrSign(
    srcInputStream: InputStream, destOutputStream: OutputStream,
    pgpPublicKeyRingCollection: PGPPublicKeyRingCollection,
    pgpSecretKeyRingCollection: PGPSecretKeyRingCollection? = null,
    secretKeyRingProtector: SecretKeyRingProtector? = null,
    doArmor: Boolean = false
  ) {
    srcInputStream.use { srcStream ->
      destOutputStream.use { outStream ->
        val encOpt = EncryptionOptions()
        pgpPublicKeyRingCollection.forEach {
          encOpt.addRecipient(it)
        }

        var signOpt: SigningOptions? = null
        pgpSecretKeyRingCollection?.let { collection ->
          signOpt = SigningOptions()
          collection.forEach {
            signOpt?.addInlineSignature(
              secretKeyRingProtector, it, DocumentSignatureType.BINARY_DOCUMENT
            )
          }
        }

        PGPainless.encryptAndOrSign()
          .onOutputStream(outStream)
          .withOptions(
            ProducerOptions
              .signAndEncrypt(encOpt, signOpt)
              .setAsciiArmor(doArmor)
          ).use { encryptionStream ->
            srcStream.copyTo(encryptionStream)
          }
      }
    }
  }
}
