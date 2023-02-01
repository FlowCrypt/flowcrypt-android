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
import org.pgpainless.encryption_signing.EncryptionStream
import org.pgpainless.encryption_signing.ProducerOptions
import org.pgpainless.encryption_signing.SigningOptions
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.pgpainless.util.Passphrase
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
object PgpEncryptAndOrSign {
  fun encryptAndOrSignMsg(
    msg: String,
    pubKeys: List<String>,
    prvKeys: List<String>? = null,
    secretKeyRingProtector: SecretKeyRingProtector? = null,
    hideArmorMeta: Boolean = false,
    passphrase: Passphrase? = null
  ): String {
    val outputStreamForEncryptedSource = ByteArrayOutputStream()
    encryptAndOrSign(
      srcInputStream = ByteArrayInputStream(msg.toByteArray()),
      destOutputStream = outputStreamForEncryptedSource,
      pubKeys = pubKeys,
      prvKeys = prvKeys,
      secretKeyRingProtector = secretKeyRingProtector,
      doArmor = true,
      hideArmorMeta = hideArmorMeta,
      passphrase = passphrase
    )
    return String(outputStreamForEncryptedSource.toByteArray())
  }

  fun encryptAndOrSign(
    srcInputStream: InputStream,
    destOutputStream: OutputStream,
    pubKeys: List<String>,
    prvKeys: List<String>? = null,
    secretKeyRingProtector: SecretKeyRingProtector? = null,
    doArmor: Boolean = false,
    hideArmorMeta: Boolean = false,
    passphrase: Passphrase? = null,
    fileName: String? = null,
  ) {
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
      doArmor = doArmor,
      hideArmorMeta = hideArmorMeta,
      passphrase = passphrase,
      fileName = fileName,
    )
  }

  @Throws(IOException::class)
  fun encryptAndOrSign(
    srcInputStream: InputStream, destOutputStream: OutputStream,
    pgpPublicKeyRingCollection: PGPPublicKeyRingCollection,
    pgpSecretKeyRingCollection: PGPSecretKeyRingCollection? = null,
    secretKeyRingProtector: SecretKeyRingProtector? = null,
    doArmor: Boolean = false,
    hideArmorMeta: Boolean = false,
    passphrase: Passphrase? = null,
    fileName: String? = null,
  ) {
    srcInputStream.use { srcStream ->
      genEncryptionStream(
        destOutputStream = destOutputStream,
        pgpPublicKeyRingCollection = pgpPublicKeyRingCollection,
        pgpSecretKeyRingCollection = pgpSecretKeyRingCollection,
        secretKeyRingProtector = secretKeyRingProtector,
        doArmor = doArmor,
        hideArmorMeta = hideArmorMeta,
        passphrase = passphrase,
        fileName = fileName,
      ).use { encryptionStream ->
        srcStream.copyTo(encryptionStream)
      }
    }
  }

  private fun genEncryptionStream(
    destOutputStream: OutputStream,
    pgpPublicKeyRingCollection: PGPPublicKeyRingCollection,
    pgpSecretKeyRingCollection: PGPSecretKeyRingCollection?,
    secretKeyRingProtector: SecretKeyRingProtector?,
    doArmor: Boolean,
    hideArmorMeta: Boolean = false,
    passphrase: Passphrase? = null,
    fileName: String? = null,
  ): EncryptionStream {
    val encOpt = EncryptionOptions().apply {
      passphrase?.let { addPassphrase(passphrase) }
      pgpPublicKeyRingCollection.forEach {
        addRecipient(it)
      }
    }

    val producerOptions: ProducerOptions =
      if (passphrase == null && pgpSecretKeyRingCollection?.any() == true) {
        ProducerOptions.signAndEncrypt(encOpt, SigningOptions().apply {
          pgpSecretKeyRingCollection.forEach {
            addInlineSignature(
              secretKeyRingProtector, it, DocumentSignatureType.BINARY_DOCUMENT
            )
          }
        })
      } else {
        ProducerOptions.encrypt(encOpt)
      }

    producerOptions.isAsciiArmor = doArmor
    producerOptions.isHideArmorHeaders = doArmor && hideArmorMeta

    fileName?.let { producerOptions.fileName = it }

    return PGPainless.encryptAndOrSign()
      .onOutputStream(destOutputStream)
      .withOptions(producerOptions)
  }
}
