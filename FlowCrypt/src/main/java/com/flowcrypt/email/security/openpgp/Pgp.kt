/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.openpgp

import com.flowcrypt.email.api.retrofit.response.model.node.Algo
import com.flowcrypt.email.api.retrofit.response.model.node.KeyId
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import org.bouncycastle.bcpg.ArmoredInputStream
import org.bouncycastle.bcpg.ArmoredOutputStream
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags
import org.bouncycastle.openpgp.PGPException
import org.bouncycastle.openpgp.PGPKeyRing
import org.bouncycastle.openpgp.PGPPublicKey
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.bouncycastle.openpgp.PGPSignature
import org.bouncycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection
import org.bouncycastle.openpgp.jcajce.JcaPGPSecretKeyRingCollection
import org.pgpainless.PGPainless
import org.pgpainless.algorithm.KeyFlag
import org.pgpainless.key.generation.KeySpec
import org.pgpainless.key.generation.type.KeyType
import org.pgpainless.key.generation.type.ecc.EllipticCurve
import org.pgpainless.key.generation.type.eddsa.EdDSACurve
import org.pgpainless.key.generation.type.rsa.RSA
import org.pgpainless.key.generation.type.rsa.RsaLength
import org.pgpainless.key.generation.type.xdh.XDHCurve
import org.pgpainless.key.protection.UnprotectedKeysProtector
import org.pgpainless.key.util.KeyRingUtils
import org.pgpainless.util.selection.userid.SelectUserId
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.lang.System.identityHashCode
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.*
import javax.mail.internet.InternetAddress
import kotlin.collections.ArrayList
import kotlin.collections.set
import kotlin.experimental.and

/**
 * @author Denis Bondarenko
 *         Date: 3/2/21
 *         Time: 9:46 AM
 *         E-mail: DenBond7@gmail.com
 */
@Suppress("unused")
enum class KeypairType {
  // NOTE: must remain in this case exactly
  openpgp, x509
}

object Pgp {

  class ParseError(override val message: String, override val cause: Throwable? = null) : Exception(message, cause)

  //    private val version = get<String>(FlowCryptQualifiers.VERSION)
  data class Pair(var prv: ArmoredKey, var pub: ArmoredKey) {
    init {
      if (prv.parsed.id != pub.parsed.id) {
        throw ParseError(
            "Submitted private key fingerprint ${prv.parsed.id}" +
                " does not match public key ${pub.parsed.id}"
        )
      }
    }
    // companion object
  }

  @Suppress("unused")
  data class Uid(val email: String, val name: String)

  data class Key(
      val type: KeypairType, //  'openpgp' | 'x509' for now only openpgp supported
      val id: String, // This is a fingerprint for OpenPGP keys and Serial Number for X.509 keys.
      val allIds: Array<String>,
      val uids: Array<String>, // a list of fingerprints for OpenPGP key or a Serial Number for X.509 keys.
      val expiration: Instant?, // some kt native date format
      val created: Instant,
      val lastModified: Instant,
      val revocation: Instant?,
      val uidEmails: Array<String>,
      val identities: Array<String>,
      val isPrivate: Boolean,
      val fullyDecrypted: Boolean?,
      val algorithmId: Int,
      val bitStrength: Int?
  ) {
    val algorithmName: String = when (algorithmId) {
      PublicKeyAlgorithmTags.RSA_GENERAL -> "RSA"
      PublicKeyAlgorithmTags.RSA_ENCRYPT -> "RSA (Encryption only)"
      PublicKeyAlgorithmTags.RSA_SIGN -> "RSA (Signing only)"
      PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT -> "ElGamal (Encryption only)"
      PublicKeyAlgorithmTags.DSA -> "DSA"
      PublicKeyAlgorithmTags.ECDH -> "ECDH"
      PublicKeyAlgorithmTags.ECDSA -> "ECDSA"
      PublicKeyAlgorithmTags.ELGAMAL_GENERAL -> "ElGamal"
      PublicKeyAlgorithmTags.DIFFIE_HELLMAN -> "Diffie-Hellman X9.42"
      PublicKeyAlgorithmTags.EDDSA -> "EdDSA"
      else -> "Other"
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      other as Key
      if (type != other.type) return false
      if (id != other.id) return false
      if (!uids.contentEquals(other.uids)) return false
      if (expiration != other.expiration) return false
      if (created != other.created) return false
      if (lastModified != other.lastModified) return false
      if (revocation != other.revocation) return false
      if (!uidEmails.contentEquals(other.uidEmails)) return false
      if (!identities.contentEquals(other.identities)) return false
      if (isPrivate != other.isPrivate) return false
      if (fullyDecrypted != other.fullyDecrypted) return false
      if (algorithmId != other.algorithmId) return false
      if (bitStrength != other.bitStrength) return false
      return true
    }

    override fun hashCode(): Int {
      var result = type.hashCode()
      result = 31 * result + id.hashCode()
      result = 31 * result + uids.contentHashCode()
      result = 31 * result + (expiration?.hashCode() ?: 0)
      result = 31 * result + created.hashCode()
      result = 31 * result + lastModified.hashCode()
      result = 31 * result + (revocation?.hashCode() ?: 0)
      result = 31 * result + uidEmails.contentHashCode()
      result = 31 * result + identities.contentHashCode()
      result = 31 * result + isPrivate.hashCode()
      result = 31 * result + (fullyDecrypted?.hashCode() ?: 0)
      result = 31 * result + algorithmId.hashCode()
      result = 31 * result + (bitStrength?.hashCode() ?: 0)
      return result
    }
  }

  data class ArmoredKey(
      val armored: ByteArray,
      val parsed: Key
  ) {
    override fun equals(other: Any?): Boolean {
      return other is ArmoredKey && this.parsed == other.parsed
    }

    override fun hashCode(): Int {
      var result = armored.contentHashCode()
      result = 31 * result + parsed.hashCode()
      return result
    }

    // We are putting this not in the toString() but here to prevent accidental leaks of a sensitive data.
    // But still it's natural to have some method that will make string from this object.
    fun asString(): String {
      return String(armored, StandardCharsets.UTF_8)
    }

    // Need to override to avoid leaking sensitive info, default one results too much details
    override fun toString(): String {
      return "ArmoredKey[@${Integer.toHexString(identityHashCode(this))}]"
    }

    //need to review this code
    fun toNodeKeyDetails(): NodeKeyDetails {
      return NodeKeyDetails(
          isFullyDecrypted = parsed.fullyDecrypted,
          isFullyEncrypted = true,
          privateKey = if (parsed.isPrivate) asString() else null,
          publicKey = if (parsed.isPrivate) parsePubFromPrv(armored).asString() else asString(),
          users = parsed.uids.toList(),
          ids = parsed.allIds.map { KeyId(it, it, it, it) },
          created = parsed.created.toEpochMilli(),
          lastModified = parsed.lastModified.toEpochMilli(),
          expiration = parsed.expiration?.toEpochMilli() ?: 0,
          algo = Algo(algorithm = parsed.algorithmName,
              algorithmId = parsed.algorithmId,
              bits = parsed.bitStrength ?: 0,
              curve = null),
          passphrase = null,
          errorMsg = null)
    }
  }

  fun parseKeyPair(armoredPub: ByteArray, armoredPrv: ByteArray, expectedId: String?): Pair {
    try {
      val prv = parse(armoredPrv, expectPrivate = true)
      if (expectedId != null && expectedId != prv.parsed.id) {
        throw ParseError(
            "Submitted private key has fingerprint ${prv.parsed.id}" +
                " does not match claimed id $expectedId"
        )
      }
      val pub = parse(armoredPub, expectPrivate = false)
      if (expectedId != null && expectedId != pub.parsed.id) {
        throw ParseError(
            "Submitted public key has fingerprint ${pub.parsed.id}" +
                " does not match claimed id $expectedId"
        )
      }
      if (prv.parsed.id != pub.parsed.id) {
        throw ParseError(
            "Provided Public Key id ${pub.parsed.id}" +
                " is different from provided Private Key id ${prv.parsed.id}"
        )
      }
      if (prv.parsed.lastModified != pub.parsed.lastModified) {
        throw ParseError(
            "Provided Public Key was last modified at ${pub.parsed.lastModified}" +
                " while provided private key was last modified at ${prv.parsed.lastModified}"
        )
      }
      return Pair(prv, pub)
    } catch (pgpEx: PGPException) {
      throw ParseError(pgpEx.message ?: "PGPException")
    } catch (ex: NoSuchElementException) {
      throw ParseError(ex.message ?: "NoSuchElementException")
    }
  }

  fun parse(armoredStr: String, expectPrivate: Boolean): ArmoredKey {
    return parse(armoredStr.toByteArray(StandardCharsets.UTF_8), expectPrivate)
  }

  fun parseKeys(armoredStr: String): Collection<ArmoredKey> {
    return try {
      listOf(parse(armoredStr, true))
    } catch (e: Exception) {
      listOf(parse(armoredStr, false))
    }
  }

  fun parse(armored: ByteArray, expectPrivate: Boolean): ArmoredKey {
    try {
      if (expectPrivate) {
        val prvRing = readArmoredPrv(armored)
        if (prvRing != null) {
          val parsed = parse(keyRing = prvRing)
          if (!parsed.isPrivate) {
            throw ParseError("Expected a Private Key, but got Public Key")
          }
          /*if (parsed.fullyDecrypted == null || !parsed.fullyDecrypted) {
            throw ParseError("Expected Private Key to be fully decrypted")
          }*/
          return ArmoredKey(armored = armored, parsed = parsed)
        } else {
          throw ParseError("Could not parse armored data as an OpenPGP Private Key")
        }
      } else {
        val pubRing = readArmoredPub(armored)
        if (pubRing != null) {
          val parsed = parse(keyRing = pubRing)
          if (parsed.isPrivate) {
            throw ParseError("Expected a Public Key, but got Private Key")
          }
          return ArmoredKey(armored = armored, parsed = parsed)
        } else {
          throw ParseError("Could not parse armored data as an OpenPGP Public Key")
        }
      }
    } catch (e: ParseError) {
      throw e
    } catch (e: Exception) {
      throw ParseError("Could not parse OpenPGP key", e)
    }
  }

  fun parsePubFromPrv(armored: ByteArray): ArmoredKey {
    val prvRing = readArmoredPrv(armored)
    if (prvRing != null) {
      val publicKeys = prvRing.publicKeys
      if (!publicKeys.hasNext()) {
        throw IllegalArgumentException("There are no public keys")
      }
      val pkList = ArrayList<PGPPublicKey>()
      while (publicKeys.hasNext()) {
        pkList.add(publicKeys.next())
      }
      val pubRing = PGPPublicKeyRing(pkList)
      val parsed = parse(keyRing = pubRing)
      var armoredPub: ByteArray
      ByteArrayOutputStream().use { byteStream ->
        ArmoredOutputStream(byteStream).use { armoredStream ->
          pubRing.encode(armoredStream, true)
        }
        armoredPub = byteStream.toByteArray()
      }
      return ArmoredKey(armored = armoredPub, parsed = parsed)
    } else {
      throw ParseError("Could not parse armored data as an OpenPGP Private Key")
    }
  }

  @OptIn(ExperimentalUnsignedTypes::class)
  fun parse(keyRing: PGPKeyRing): Key {
    val key = keyRing.publicKey
    val id = bytesToHex(key.fingerprint)
    val emails = key.userIDs.asSequence().map {
      val address = InternetAddress(it)
      address.validate()
      address.address
    }.toList()
    val lastModified = key.signatures.asSequence()
        .map { (it as PGPSignature) }
        .filter { it.keyID == key.keyID }
        .map { it.creationTime.time }
        .fold(0L, Math::max)
    val revokedDate = key.getSignaturesOfType(PGPSignature.KEY_REVOCATION).iterator().asSequence()
        .map { (it as PGPSignature).creationTime }.firstOrNull()
    val isPrivate = when (keyRing) {
      is PGPSecretKeyRing -> true
      is PGPPublicKeyRing -> false
      else -> throw ParseError(
          "Expected PGPKeyRing to be either PGPSecretKeyRing or PGPPublicKeyRing," +
              " got ${keyRing::class.java.name}"
      )
    }
    val fullyDecrypted = if (keyRing is PGPSecretKeyRing) { // did the test again for Smart Cast
      // encrypt. algo. 0 means not encrypted https://tools.ietf.org/html/rfc4880#section-5.5.3
      keyRing.secretKeys.iterator().asSequence().all { it.keyEncryptionAlgorithm == 0 }
    } else {
      null // public key
    }
    if ((isPrivate && fullyDecrypted == null) || (!isPrivate && fullyDecrypted != null)) { // sanity check
      throw Exception("Unexpected combination: isPrivate=$isPrivate, fullyDecrypted=$fullyDecrypted")
    }
    val bitStrength = if (key.bitStrength != -1) {
      key.bitStrength
    } else {
      null
    }
    val idArray = key.userIDs.asSequence().toList().toTypedArray()
    return Key(
        type = KeypairType.openpgp,
        id = id,
        allIds = keyRing.publicKeys.iterator().asSequence().map { bytesToHex(it.fingerprint) }.toList().toTypedArray(),
        uids = idArray,
        expiration = if (key.validSeconds == 0L) null else Instant.ofEpochMilli(
            key.creationTime.time + key.validSeconds * 1000
        ),
        created = Instant.ofEpochMilli(key.creationTime.time),
        lastModified = Instant.ofEpochMilli(lastModified),
        revocation = if (revokedDate != null) Instant.ofEpochMilli(revokedDate.time) else null,
        uidEmails = emails.toList().toTypedArray(),
        identities = idArray,
        isPrivate = isPrivate,
        fullyDecrypted = fullyDecrypted,
        algorithmId = key.algorithm,
        bitStrength = bitStrength
    )
  }

  fun readArmoredPub(armored: ByteArray): PGPPublicKeyRing? {
    return readArmoredPub(armored.inputStream())
  }

  @Suppress("unused")
  fun dearmorPub(armoredPub: ByteArray): ByteArray {
    val decodedPub = readArmoredPub(armoredPub)
        ?: throw ParseError("No Public Key found after de-armoring")
    return decodedPub.getEncoded(true)
  }

  @Suppress("unused")
  fun parseBinaryPubs(binaryPub: ByteArray): List<Key> {
    try {
      return JcaPGPPublicKeyRingCollection(binaryPub).toList().map { parse(it) }
    } catch (e: IOException) {
      return listOf()
    }
  }

  private fun readArmoredPub(armored: InputStream): PGPPublicKeyRing? {
    val keyIn = ArmoredInputStream(armored)
    val pgpRing = JcaPGPPublicKeyRingCollection(keyIn)
    if (pgpRing.keyRings.hasNext()) {
      return pgpRing.keyRings.next()
    }
    return null
  }

  private fun readArmoredPrv(armored: ByteArray): PGPSecretKeyRing? {
    val keyIn = ArmoredInputStream(armored.inputStream())
    val pgpRing = JcaPGPSecretKeyRingCollection(keyIn)
    if (pgpRing.keyRings.hasNext()) {
      return pgpRing.keyRings.next()
    }
    return null
  }

  @kotlin.ExperimentalUnsignedTypes
  private fun bytesToHex(bytes: ByteArray): String {
    val hexChars = CharArray(bytes.size * 2)
    for (i in bytes.indices) {
      var hex = (bytes[i] and 0xFF.toByte()).toUByte().toString(16)
      hex = if (hex.length == 2) hex else "0$hex"
      hexChars[i * 2] = hex[0]
      hexChars[i * 2 + 1] = hex[1]
    }
    return String(hexChars).toUpperCase()
  }

  // NOTE: must remain in this case exactly
  enum class PgpKeyType { rsa2048, rsa3072, rsa4096, curve25519, p256 }

  @Suppress("unused")
  fun generateKey(users: Array<String>, type: PgpKeyType, expiration: Instant?): Pair {
    if (users.isEmpty()) {
      throw IllegalArgumentException("Expecting at least one User ID.")
    }

    @Suppress("deprecation")
    val encryptionKeyType = when (type) {
      PgpKeyType.rsa2048 -> RSA.withLength(RsaLength._2048)
      PgpKeyType.rsa3072 -> RSA.withLength(RsaLength._3072)
      PgpKeyType.rsa4096 -> RSA.withLength(RsaLength._4096)
      PgpKeyType.curve25519 -> KeyType.XDH(XDHCurve._X25519)
      PgpKeyType.p256 -> KeyType.ECDH(EllipticCurve._P256)
    }

    @Suppress("deprecation")
    val signingKeyType = when (type) {
      PgpKeyType.rsa2048 -> RSA.withLength(RsaLength._2048)
      PgpKeyType.rsa3072 -> RSA.withLength(RsaLength._3072)
      PgpKeyType.rsa4096 -> RSA.withLength(RsaLength._4096)
      PgpKeyType.curve25519 -> KeyType.EDDSA(EdDSACurve._Ed25519)
      PgpKeyType.p256 -> KeyType.ECDSA(EllipticCurve._P256)
    }

    val generate = PGPainless.generateKeyRing()
        .withSubKey(
            KeySpec.getBuilder(encryptionKeyType)
                .withKeyFlags(KeyFlag.ENCRYPT_COMMS, KeyFlag.ENCRYPT_STORAGE)
                .withDefaultAlgorithms()
        )
        .withMasterKey(
            KeySpec.getBuilder(signingKeyType)
                .withKeyFlags(KeyFlag.SIGN_DATA, KeyFlag.CERTIFY_OTHER)
                .withDefaultAlgorithms()
        )
        .withPrimaryUserId(users.first())

    for (user in users.drop(1)) {
      generate.withAdditionalUserId(user)
    }

    val prvKeyRing = generate.withoutPassphrase().build()
    if (expiration != null) {
//            val info = PGPainless.inspectKeyRing(prvKeyRing)
//            prvKeyRing = PGPainless.modifyKeyRing(prvKeyRing).setExpirationDate(info.fingerprint,
//                Date.from(expiration), UnprotectedKeysProtector()).done()
      // todo - https://github.com/pgpainless/pgpainless/issues/48
      throw NotImplementedError("Native Kotlin keygen expiration is not implemented yet")
    }

    val pubKeyRing = KeyRingUtils.publicKeyRingFrom(prvKeyRing)
    return parseKeyPair(
        armoredPub = armor(pubKeyRing),
        armoredPrv = armor(prvKeyRing),
        expectedId = null
    )
  }

  fun armor(key: PGPKeyRing): ByteArray {
    val keyOut = ByteArrayOutputStream()
    val armoredOut = ArmoredOutputStream(keyOut)
    key.encode(armoredOut)
    armoredOut.close()
    keyOut.close()
    return keyOut.toByteArray()
  }

  @Deprecated(
      "prefer fingerprints over longid",
      ReplaceWith("com.flowcrypt.crypto.Pgp.fingerprint", "com.flowcrypt.crypto.Pgp")
  )
  fun longid(keyId: Long): String {
    return java.lang.Long.toHexString(keyId).toUpperCase().padStart(16, '0')
  }

  private interface SecretKeyRingModifier {
    fun modifySecretKeyRing(secretKeyRing: PGPSecretKeyRing): PGPSecretKeyRing
  }

  private object RevokeKey : SecretKeyRingModifier {
    override fun modifySecretKeyRing(secretKeyRing: PGPSecretKeyRing): PGPSecretKeyRing {
      return PGPainless.modifyKeyRing(secretKeyRing).revoke(UnprotectedKeysProtector()).done()
    }
  }

  fun revokeKey(armored: ByteArray): Pair {
    return modifySecretKey(armored, RevokeKey)
  }

  private class SetUids(newUidList: List<String>) : SecretKeyRingModifier {

    val newUids: Map<String, String?> = parseUids(newUidList.asSequence())

    companion object {
      private fun parseUids(src: Sequence<String>): Map<String, String?> {
        val result = mutableMapOf<String, String?>()
        for (uid in src) {
          val address = InternetAddress(uid)
          address.validate()
          result[address.address] = address.personal
        }
        return result
      }
    }

    override fun modifySecretKeyRing(secretKeyRing: PGPSecretKeyRing): PGPSecretKeyRing {
      val keyUids = parseUids(secretKeyRing.publicKey.userIDs.asSequence())
      val protector = UnprotectedKeysProtector()
      val editor = PGPainless.modifyKeyRing(secretKeyRing)

      for (uid in keyUids - newUids) {
        val email = uid.key as String
        editor.deleteUserIds(SelectUserId.containsEmailAddress(email), protector)
      }

      for (uid in newUids - keyUids) {
        val email = uid.key as String
        val fullUid = if (uid.value == null || uid.value!!.isEmpty()) "<$email>" else "${uid.value} <$email>"
        editor.addUserId(fullUid, protector)
      }

      return editor.done()
    }
  }

  fun setUids(armored: ByteArray, uids: List<String>): Pair {
    return modifySecretKey(armored, SetUids(uids))
  }

  private fun modifySecretKey(armored: ByteArray, modifier: SecretKeyRingModifier): Pair {
    val sourceSecretKey = PGPainless.readKeyRing().secretKeyRing(armored).secretKey
    val resultSecretKeyRing = modifier.modifySecretKeyRing(PGPSecretKeyRing(listOf(sourceSecretKey)))

    val publicKey = resultSecretKeyRing.publicKeys.next()
    val pubOutBytes = ByteArrayOutputStream()
    val pubOutStream = ArmoredOutputStream(pubOutBytes)
    publicKey?.encode(pubOutStream)
    pubOutStream.close()
    pubOutBytes.close()

    val secretKey = resultSecretKeyRing.secretKeys.next()
    val prvOutBytes = ByteArrayOutputStream()
    val prvOutStream = ArmoredOutputStream(prvOutBytes)
    secretKey?.encode(prvOutStream)
    prvOutStream.close()
    prvOutBytes.close()

    return parseKeyPair(pubOutBytes.toByteArray(), prvOutBytes.toByteArray(), null)
  }
}