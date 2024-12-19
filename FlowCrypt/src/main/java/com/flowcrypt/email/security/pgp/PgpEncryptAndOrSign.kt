/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.extensions.kotlin.toPGPPublicKeyRingCollection
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection
import org.pgpainless.PGPainless
import org.pgpainless.algorithm.DocumentSignatureType
import org.pgpainless.encryption_signing.EncryptionOptions
import org.pgpainless.encryption_signing.EncryptionResult
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
 * @author Denys Bondarenko
 */
object PgpEncryptAndOrSign {
  fun encryptAndOrSignMsg(
    msg: String,
    pubKeys: List<String>? = null,
    protectedPubKeys: List<String>? = null,
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
      protectedPubKeys = protectedPubKeys,
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
    pubKeys: List<String>? = null,
    protectedPubKeys: List<String>? = null,
    prvKeys: List<String>? = null,
    secretKeyRingProtector: SecretKeyRingProtector? = null,
    doArmor: Boolean = false,
    hideArmorMeta: Boolean = false,
    passphrase: Passphrase? = null,
    fileName: String? = null,
    generateDetachedSignatures: Boolean = false,
  ): EncryptionResult {
    val pgpPublicKeyRingCollection = pubKeys?.toPGPPublicKeyRingCollection()
    val protectedPgpPublicKeyRingCollection = protectedPubKeys?.toPGPPublicKeyRingCollection()

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

    return encryptAndOrSign(
      srcInputStream = srcInputStream,
      destOutputStream = destOutputStream,
      pgpPublicKeyRingCollection = pgpPublicKeyRingCollection,
      protectedPgpPublicKeyRingCollection = protectedPgpPublicKeyRingCollection,
      pgpSecretKeyRingCollection = pgpSecretKeyRingCollection,
      secretKeyRingProtector = secretKeyRingProtector,
      doArmor = doArmor,
      hideArmorMeta = hideArmorMeta,
      passphrase = passphrase,
      fileName = fileName,
      generateDetachedSignatures = generateDetachedSignatures
    )
  }

  @Throws(IOException::class)
  fun encryptAndOrSign(
    srcInputStream: InputStream,
    destOutputStream: OutputStream,
    pgpPublicKeyRingCollection: PGPPublicKeyRingCollection? = null,
    protectedPgpPublicKeyRingCollection: PGPPublicKeyRingCollection? = null,
    pgpSecretKeyRingCollection: PGPSecretKeyRingCollection? = null,
    secretKeyRingProtector: SecretKeyRingProtector? = null,
    doArmor: Boolean = false,
    hideArmorMeta: Boolean = false,
    passphrase: Passphrase? = null,
    fileName: String? = null,
    generateDetachedSignatures: Boolean = false,
  ): EncryptionResult {
    srcInputStream.use { srcStream ->
      return encryptAndOrSign(
        destOutputStream = destOutputStream,
        pgpPublicKeyRingCollection = pgpPublicKeyRingCollection,
        protectedPgpPublicKeyRingCollection = protectedPgpPublicKeyRingCollection,
        pgpSecretKeyRingCollection = pgpSecretKeyRingCollection,
        secretKeyRingProtector = secretKeyRingProtector,
        doArmor = doArmor,
        hideArmorMeta = hideArmorMeta,
        passphrase = passphrase,
        fileName = fileName,
        generateDetachedSignatures = generateDetachedSignatures,
      ) { encryptionStream ->
        srcStream.copyTo(encryptionStream)
      }
    }
  }

  @Throws(IOException::class)
  fun encryptAndOrSign(
    destOutputStream: OutputStream,
    pgpPublicKeyRingCollection: PGPPublicKeyRingCollection? = null,
    protectedPgpPublicKeyRingCollection: PGPPublicKeyRingCollection? = null,
    pgpSecretKeyRingCollection: PGPSecretKeyRingCollection? = null,
    secretKeyRingProtector: SecretKeyRingProtector? = null,
    doArmor: Boolean = false,
    hideArmorMeta: Boolean = false,
    passphrase: Passphrase? = null,
    fileName: String? = null,
    generateDetachedSignatures: Boolean = false,
    action: (outputStream: OutputStream) -> Unit
  ): EncryptionResult {
    return genEncryptionStreamInternal(
      destOutputStream = destOutputStream,
      pgpPublicKeyRingCollection = pgpPublicKeyRingCollection,
      protectedPgpPublicKeyRingCollection = protectedPgpPublicKeyRingCollection,
      pgpSecretKeyRingCollection = pgpSecretKeyRingCollection,
      secretKeyRingProtector = secretKeyRingProtector,
      doArmor = doArmor,
      hideArmorMeta = hideArmorMeta,
      passphrase = passphrase,
      fileName = fileName,
      generateDetachedSignatures = generateDetachedSignatures,
    ).apply {
      this.use { encryptionStream ->
        action.invoke(encryptionStream)
      }
    }.result
  }

  private fun genEncryptionStreamInternal(
    destOutputStream: OutputStream,
    pgpPublicKeyRingCollection: PGPPublicKeyRingCollection?,
    protectedPgpPublicKeyRingCollection: PGPPublicKeyRingCollection?,
    pgpSecretKeyRingCollection: PGPSecretKeyRingCollection?,
    secretKeyRingProtector: SecretKeyRingProtector?,
    doArmor: Boolean,
    hideArmorMeta: Boolean = false,
    passphrase: Passphrase? = null,
    fileName: String? = null,
    generateDetachedSignatures: Boolean = false,
  ): EncryptionStream {
    val encOpt = EncryptionOptions().apply {
      passphrase?.let { addMessagePassphrase(passphrase) }
      pgpPublicKeyRingCollection?.forEach {
        addRecipient(it)
      }

      protectedPgpPublicKeyRingCollection?.forEach {
        addHiddenRecipient(it)
      }
    }

    val producerOptions: ProducerOptions =
      if (passphrase == null && pgpSecretKeyRingCollection?.any() == true) {
        ProducerOptions.signAndEncrypt(encOpt, SigningOptions().apply {
          pgpSecretKeyRingCollection.forEach { pgpSecretKeyRing ->
            secretKeyRingProtector?.let { protector ->
              if (generateDetachedSignatures) {
                addDetachedSignature(
                  protector,
                  pgpSecretKeyRing,
                  DocumentSignatureType.BINARY_DOCUMENT
                )
              } else {
                addInlineSignature(
                  protector,
                  pgpSecretKeyRing,
                  DocumentSignatureType.BINARY_DOCUMENT
                )
              }
            }
          }
        })
      } else {
        ProducerOptions.encrypt(encOpt)
      }

    producerOptions.setAsciiArmor(doArmor)
    producerOptions.setHideArmorHeaders(doArmor && hideArmorMeta)

    fileName?.let { producerOptions.setFileName(it) }

    return PGPainless.encryptAndOrSign()
      .onOutputStream(destOutputStream)
      .withOptions(producerOptions)
  }
}
