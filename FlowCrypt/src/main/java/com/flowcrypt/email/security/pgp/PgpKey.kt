/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: Ivan Pizhenko
 */

package com.flowcrypt.email.security.pgp

import com.flowcrypt.email.api.retrofit.response.model.node.MsgBlock
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.core.msg.MsgBlockParser
import com.flowcrypt.email.extensions.pgp.toNodeKeyDetails
import java.nio.charset.StandardCharsets
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.openpgp.PGPKeyRing
import org.bouncycastle.openpgp.PGPObjectFactory
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKey
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection
import org.bouncycastle.openpgp.jcajce.JcaPGPSecretKeyRingCollection
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator

object PgpKey {

  /**
   * Parses multiple keys, binary or armored.
   *
   * @return Pair.first  indicates armored (true) or binary (false) format
   *         Pair.second list of keys
   */
  fun parseKeys(source: ByteArray): Pair<Boolean, List<NodeKeyDetails>> {
    val blockType = PgpMsg.detectBlockType(source)
    if (blockType.second == MsgBlock.Type.UNKNOWN) {
      throw IllegalArgumentException("Unknown message type")
    }

    val allKeys = mutableListOf<NodeKeyDetails>()
    if (blockType.first) {
      // armored text format
      val blocks = MsgBlockParser.detectBlocks(String(source, StandardCharsets.UTF_8))
      for (block in blocks) {
        val content = block.content
        if (content != null) {
          val keys = parse(content)
          allKeys.addAll(keys)
        }
      }
    } else {
      // binary format
      val objectFactory = PGPObjectFactory(source.inputStream(), JcaKeyFingerprintCalculator())
      while (true) {
        val obj = objectFactory.nextObject() ?: break
        if (obj is PGPKeyRing) {
          allKeys.add(obj.toNodeKeyDetails())
        }
      }
    }

    return Pair(blockType.first, allKeys)
  }

  fun parse(armored: String): List<NodeKeyDetails>
    = parseAndNormalizeKeyRings(armored).map { it.toNodeKeyDetails() }.toList()

  private fun parseAndNormalizeKeyRings(armored: String): List<PGPKeyRing> {
    val normalizedArmored = PgpArmor.normalize(armored, MsgBlock.Type.UNKNOWN)
    val keys = mutableListOf<PGPKeyRing>()
    if (PgpArmor.ARMOR_HEADER_DICT_REGEX[MsgBlock.Type.PUBLIC_KEY]!!
            .beginRegexp.containsMatchIn(normalizedArmored)) {
      val keyRingCollection = JcaPGPPublicKeyRingCollection(
        ArmoredInputStream(normalizedArmored.toByteArray(StandardCharsets.UTF_8).inputStream())
      )
      // We have to use reflection because BouncyCastle declares "order" list as a private field
      // https://stackoverflow.com/a/1196207/1540501
      // Sent a request to BouncyCastle mailing list to make this possible in a better way.
      val orderField = keyRingCollection.javaClass.superclass!!.getDeclaredField("order")
      orderField.isAccessible = true
      keys.addAll(
          (orderField.get(keyRingCollection) as java.util.List<java.lang.Long>).map {
            keyRingCollection.getPublicKeyRing(it.toLong())!!
          }
      )
    } else if (PgpArmor.ARMOR_HEADER_DICT_REGEX[MsgBlock.Type.PRIVATE_KEY]!!
            .beginRegexp.containsMatchIn(normalizedArmored)) {
      val keyRingCollection = JcaPGPSecretKeyRingCollection(
          ArmoredInputStream(normalizedArmored.toByteArray(StandardCharsets.UTF_8).inputStream())
      )
      // Again, use reflection because BouncyCastle declares "order" list as a private field.
      val orderField = keyRingCollection.javaClass.superclass!!.getDeclaredField("order")
      orderField.isAccessible = true
      keys.addAll(
          (orderField.get(keyRingCollection) as java.util.List<java.lang.Long>).map {
            keyRingCollection.getSecretKeyRing(it.toLong())!!
          }
      )
    } else if (PgpArmor.ARMOR_HEADER_DICT_REGEX[MsgBlock.Type.ENCRYPTED_MSG]!!
            .beginRegexp.containsMatchIn(normalizedArmored)) {
      val objectFactory = PGPObjectFactory(
          ArmoredInputStream(normalizedArmored.toByteArray(StandardCharsets.UTF_8).inputStream()),
          JcaKeyFingerprintCalculator()
      )
      while (true) {
        val obj = objectFactory.nextObject() ?: break
        if (obj is PGPKeyRing) {
          keys.add(obj)
        }
      }
    }

    // Prevent key bloat by removing all non-self certifications
    for ((keyRingIndex, keyRing) in keys.withIndex()) {
      val primaryKeyID = keyRing.publicKey.keyID
      if (keyRing is PGPPublicKeyRing) {
        var replacementKeyRing: PGPPublicKeyRing = keyRing
        for (publicKey in keyRing.publicKeys) {
          var replacementKey = publicKey
          for (sig in publicKey.signatures.asSequence().map { it as PGPSignature }.filter {
            it.isCertification && it.keyID != primaryKeyID
          }) {
            replacementKey = PGPPublicKey.removeCertification(replacementKey, sig)
          }
          if (replacementKey !== publicKey) {
            replacementKeyRing = PGPPublicKeyRing.insertPublicKey(
                replacementKeyRing, replacementKey
            )
          }
        }
        if (replacementKeyRing !== keyRing) {
          keys[keyRingIndex] = replacementKeyRing
        }
      } else if (keyRing is PGPSecretKeyRing) {
        var replacementKeyRing: PGPSecretKeyRing = keyRing
        for (secretKey in keyRing.secretKeys) {
          val publicKey = secretKey.publicKey
          var replacementPublicKey = publicKey
          for (sig in publicKey.signatures.asSequence().map { it as PGPSignature }.filter {
            it.isCertification && it.keyID != primaryKeyID
          }) {
            replacementPublicKey = PGPPublicKey.removeCertification(replacementPublicKey, sig)
          }
          if (replacementPublicKey !== publicKey) {
            val replacementKey = PGPSecretKey.replacePublicKey(secretKey, replacementPublicKey)
            replacementKeyRing = PGPSecretKeyRing.insertSecretKey(
                replacementKeyRing, replacementKey
            )
          }
        }
        if (replacementKeyRing !== keyRing) {
          keys[keyRingIndex] = replacementKeyRing
        }
      }
    }

    return keys
  }
}
