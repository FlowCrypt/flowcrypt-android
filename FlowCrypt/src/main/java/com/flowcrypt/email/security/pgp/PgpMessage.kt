/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.extensions.kotlin.toHex
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.getDecryptionKeys
import com.flowcrypt.email.extensions.org.bouncycastle.openpgp.isDecrypted
import java.io.InputStream
import org.bouncycastle.bcpg.PacketTags
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags
import org.bouncycastle.openpgp.PGPCompressedData
import org.bouncycastle.openpgp.PGPEncryptedDataList
import org.bouncycastle.openpgp.PGPLiteralData
import org.bouncycastle.openpgp.PGPMarker
import org.bouncycastle.openpgp.PGPObjectFactory
import org.bouncycastle.openpgp.PGPOnePassSignatureList
import org.bouncycastle.openpgp.PGPPBEEncryptedData
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSignatureList
import org.bouncycastle.openpgp.operator.bc.BcPBEDataDecryptorFactory
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator

class PgpMessage(val packets: List<AbstractPacketWrapper>) {

  abstract class AbstractPacketWrapper(val packetType: Int)

  @Suppress("unused")
  class SignaturePacketWrapper(
      val signatures: PGPSignatureList
  ) : AbstractPacketWrapper(PacketTags.SIGNATURE)

  @Suppress("unused")
  class SecretKeyPacketWrapper(
      val secretKeyRing: PGPSecretKeyRing
  ) : AbstractPacketWrapper(PacketTags.SECRET_KEY)

  @Suppress("unused")
  class PublicKeyPacketWrapper(
      val publicKeyRing: PGPPublicKeyRing
  ) : AbstractPacketWrapper(PacketTags.PUBLIC_KEY)

  @Suppress("unused")
  class PublicSubKeyPacketWrapper(
      val publicKey: PGPPublicKey
  ) : AbstractPacketWrapper(PacketTags.PUBLIC_KEY)

  @Suppress("unused")
  class CompressedDataPacketWrapper(
      val compressedData: PGPCompressedData
  ) : AbstractPacketWrapper(PacketTags.COMPRESSED_DATA)

  @Suppress("unused")
  class LiteralDataPacketWrapper(
      val literalData: PGPLiteralData
  ) : AbstractPacketWrapper(PacketTags.LITERAL_DATA)

  @Suppress("unused")
  class PublicKeyEncSessionPacketWrapper(
      val encryptedData: PGPEncryptedDataList
  ) : AbstractPacketWrapper(PacketTags.PUBLIC_KEY_ENC_SESSION)

  @Suppress("unused")
  class SymmetricKeyEncSessionPacketWrapper(
      val encryptedData: PGPEncryptedDataList
  ) : AbstractPacketWrapper(PacketTags.SYMMETRIC_KEY_ENC_SESSION)

  @Suppress("unused")
  class OnePassSignaturePacketWrapper(
      val signatures: PGPOnePassSignatureList
  ) : AbstractPacketWrapper(PacketTags.ONE_PASS_SIGNATURE)

  @Suppress("unused")
  class MarkerPacketWrapper(
      val signatures: PGPMarker
  ) : AbstractPacketWrapper(PacketTags.MARKER)

  companion object {
    private val fingerprintCalculator = JcaKeyFingerprintCalculator()

    fun read(inputStream: InputStream): PgpMessage {
      val packets = mutableListOf<AbstractPacketWrapper>()
      val factory = PGPObjectFactory(inputStream, fingerprintCalculator)
      // Meanwhile, with BCPG we can read here only one object
      val obj = factory.nextObject()
      if (obj != null) {
        val p = when (obj) {
          is PGPSignatureList -> SignaturePacketWrapper(obj)
          is PGPSecretKeyRing -> SecretKeyPacketWrapper(obj)
          is PGPPublicKeyRing -> PublicKeyPacketWrapper(obj)
          is PGPPublicKey -> PublicSubKeyPacketWrapper(obj)
          is PGPCompressedData -> CompressedDataPacketWrapper(obj)
          is PGPLiteralData -> LiteralDataPacketWrapper(obj)
          is PGPOnePassSignatureList -> OnePassSignaturePacketWrapper(obj)
          is PGPEncryptedDataList -> {
            if (obj[0] is PGPPublicKeyEncryptedData) {
              PublicKeyEncSessionPacketWrapper(obj)
            } else {
              SymmetricKeyEncSessionPacketWrapper(obj)
            }
          }
          is PGPMarker -> MarkerPacketWrapper(obj)
          else -> {
            throw IllegalArgumentException("Unsupported PGP object of type ${obj.javaClass.name}")
          }
        }
        packets.add(p)
      }
      return PgpMessage(packets)
    }
  }

  class DecryptionError(message: String) : RuntimeException(message)

  private val digestCalculatorProvider = BcPGPDigestCalculatorProvider()

  private val defaultAlgorithms = setOf(
      SymmetricKeyAlgorithmTags.AES_256, // Old OpenPGP.js default fallback
      SymmetricKeyAlgorithmTags.AES_128, // RFC4880bis fallback
      SymmetricKeyAlgorithmTags.TRIPLE_DES, // RFC4880 fallback
      SymmetricKeyAlgorithmTags.CAST5 // Golang OpenPGP fallback
  )

  fun decrypt(
      secretKeys: List<PGPSecretKeyRing>?,
      passwords: List<CharArray>?
  ): List<Pair<AbstractPacketWrapper, InputStream>> {
    return when {
      passwords != null -> decryptSym(passwords)
      secretKeys != null -> decryptPk(secretKeys)
      else -> throw DecryptionError("No key or password specified")
    }
  }

  private fun decryptSym(
      passwords: List<CharArray>
  ): List<Pair<AbstractPacketWrapper, InputStream>> {
    val encrypted = packets.filter { it.packetType == PacketTags.SYMMETRIC_KEY_ENC_SESSION }
        .map { it as SymmetricKeyEncSessionPacketWrapper }.toList()
    if (encrypted.isEmpty()) {
      throw DecryptionError("No symmetrically encrypted session key packet found")
    }

    val result = mutableListOf<Pair<AbstractPacketWrapper, InputStream>>()
    val decrypted = BooleanArray(encrypted.size)

    for (password in passwords) {
      val decryptorFactory = BcPBEDataDecryptorFactory(password, digestCalculatorProvider)
      for (item in encrypted.withIndex()) {
        if (decrypted[item.index]) continue
        for (method in item.value.encryptedData.encryptedDataObjects) {
          if (method !is PGPPBEEncryptedData) {
            throw DecryptionError(
                "Unsupported encryption method in the symmetrically encrypted packet"
            )
          }
          val dataStream: InputStream
          try {
            dataStream = method.getDataStream(decryptorFactory)
          } catch (ex: Exception) {
            // likely password didn't match
            continue
          }
          result.add(Pair(item.value, dataStream))
          decrypted[item.index] = true
          break
        }
      }
    }

    if (result.isEmpty()) {
      throw DecryptionError("Decryption failed")
    }

    return result
  }

  private fun decryptPk(
      secretKeys: List<PGPSecretKeyRing>
  ): List<Pair<AbstractPacketWrapper, InputStream>> {
    var exception: Exception? = null

    val encrypted = packets.filter { it.packetType == PacketTags.PUBLIC_KEY_ENC_SESSION }
        .map { it as PublicKeyEncSessionPacketWrapper }.toList()
    if (encrypted.isEmpty()) {
      throw DecryptionError("No public key encrypted session key packet found")
    }

    val result = mutableListOf<Pair<AbstractPacketWrapper, InputStream>>()
    val decrypted = BooleanArray(encrypted.size)

    for (secretKey in secretKeys) {
      val publicKey = secretKey.publicKey
      val algorithms = mutableSetOf<Int>()
      algorithms.addAll(defaultAlgorithms)
      algorithms.addAll(
          publicKey.getSignaturesForKeyID(publicKey.keyID).asSequence()
              .filter { it.isCertification && it.hasSubpackets() }
              .map { it.hashedSubPackets.preferredSymmetricAlgorithms.asIterable() }
              .flatten()
      )

      for (item in encrypted.withIndex()) {
        if (decrypted[item.index]) continue
        for (method in item.value.encryptedData.encryptedDataObjects) {
          if (method !is PGPPublicKeyEncryptedData) {
            throw DecryptionError(
                "Unsupported encryption method in the public key encrypted packet"
            )
          }
          val decryptionKeys = secretKey.getDecryptionKeys(method.keyID)
          for (decryptionKey in decryptionKeys) {
            if (!decryptionKey.isDecrypted()) {
              throw DecryptionError("Private key ${decryptionKey.keyID.toHex()} is not decrypted")
            }
            val privateKey = decryptionKey.extractPrivateKey(null)
            val decryptorFactory = BcPublicKeyDataDecryptorFactory(privateKey)
            val dataStream: InputStream
            try {
              val alg = method.getSymmetricAlgorithm(decryptorFactory)
              if (!algorithms.contains(alg)) {
                exception = DecryptionError("A non-preferred symmetric algorithm was used")
                continue
              }
              dataStream = method.getDataStream(decryptorFactory)
            } catch (ex: Exception) {
              // likely private key didn't match
              continue
            }
            result.add(Pair(item.value, dataStream))
            decrypted[item.index] = true
            break
          }
          if (decrypted[item.index]) break
        }
      }
    }

    if (result.isEmpty()) {
      throw exception ?: DecryptionError("Decryption failed")
    }

    return result
  }
}
