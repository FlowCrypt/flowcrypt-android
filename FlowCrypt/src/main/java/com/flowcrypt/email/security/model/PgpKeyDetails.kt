/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.model

import android.os.Parcel
import android.os.Parcelable
import android.util.Patterns
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress

/**
 * This class collects base info of [org.bouncycastle.openpgp.PGPKeyRing]
 * that can be used via [Parcelable] mechanism.
 *
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 1:23 PM
 * E-mail: DenBond7@gmail.com
 */
data class PgpKeyDetails constructor(
  @Expose val isFullyDecrypted: Boolean,
  @Expose val isFullyEncrypted: Boolean,
  @Expose val isRevoked: Boolean,
  @Expose val usableForEncryption: Boolean,
  @Expose @SerializedName("private") val privateKey: String?,
  @Expose @SerializedName("public") val publicKey: String,
  @Expose val users: List<String>,
  @Expose val ids: List<KeyId>,
  @Expose val created: Long,
  @Expose val lastModified: Long? = null,
  @Expose val expiration: Long? = null,
  @Expose val algo: Algo,
  var tempPassphrase: CharArray? = null,
  var passphraseType: KeyEntity.PassphraseType? = null
) : Parcelable {
  val fingerprint: String
    get() = ids.first().fingerprint
  val isPrivate: Boolean
    get() = privateKey != null

  val isExpired: Boolean
    get() = expiration != null && (System.currentTimeMillis() > expiration)

  val mimeAddresses: List<InternetAddress>
    get() = parseMimeAddresses()

  val isPartiallyEncrypted: Boolean
    get() {
      return !isFullyDecrypted && !isFullyEncrypted
    }

  constructor(source: Parcel) : this(
    source.readValue(Boolean::class.java.classLoader) as Boolean,
    source.readValue(Boolean::class.java.classLoader) as Boolean,
    source.readValue(Boolean::class.java.classLoader) as Boolean,
    source.readValue(Boolean::class.java.classLoader) as Boolean,
    source.readString(),
    source.readString() ?: throw IllegalArgumentException("pubkey can't be null"),
    source.createStringArrayList() ?: throw NullPointerException(),
    source.createTypedArrayList(KeyId.CREATOR) ?: throw NullPointerException(),
    source.readLong(),
    source.readValue(Long::class.java.classLoader) as Long?,
    source.readValue(Long::class.java.classLoader) as Long?,
    source.readParcelable<Algo>(Algo::class.java.classLoader) ?: throw NullPointerException(),
    source.createCharArray(),
    source.readParcelable<KeyEntity.PassphraseType>(
      KeyEntity.PassphraseType::class.java.classLoader
    )
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeValue(isFullyDecrypted)
    writeValue(isFullyEncrypted)
    writeValue(isRevoked)
    writeValue(usableForEncryption)
    writeString(privateKey)
    writeString(publicKey)
    writeStringList(users)
    writeTypedList(ids)
    writeLong(created)
    writeValue(lastModified)
    writeValue(expiration)
    writeParcelable(algo, flags)
    writeCharArray(tempPassphrase)
    writeParcelable(passphraseType, flags)
  }

  fun getUserIdsAsSingleString(): String {
    return mimeAddresses.joinToString { it.address }
  }

  fun getPrimaryInternetAddress(): InternetAddress? {
    return mimeAddresses.firstOrNull()
  }

  fun isNewerThan(pgpKeyDetails: PgpKeyDetails?): Boolean {
    val existingLastModified = lastModified ?: 0
    val providedLastModified = pgpKeyDetails?.lastModified ?: 0
    return existingLastModified > providedLastModified
  }

  private fun parseMimeAddresses(): List<InternetAddress> {
    val results = mutableListOf<InternetAddress>()

    for (user in users) {
      try {
        results.addAll(listOf(*InternetAddress.parse(user)))
      } catch (e: AddressException) {
        e.printStackTrace()
        val pattern = Patterns.EMAIL_ADDRESS
        val matcher = pattern.matcher(user)
        if (matcher.find()) {
          results.add(InternetAddress(matcher.group()))
        }
      }
    }

    return results
  }

  fun toKeyEntity(accountEntity: AccountEntity): KeyEntity {
    return KeyEntity(
      fingerprint = fingerprint,
      account = accountEntity.email.lowercase(),
      accountType = accountEntity.accountType,
      source = PrivateKeySourceType.BACKUP.toString(),
      publicKey = publicKey.toByteArray(),
      privateKey = privateKey?.toByteArray()
        ?: throw NullPointerException("pgpKeyDetails.privateKey == null"),
      storedPassphrase = tempPassphrase?.let { String(it) },
      passphraseType = passphraseType
        ?: throw IllegalArgumentException("passphraseType is not defined")
    )
  }

  fun toRecipientEntity(): RecipientEntity? {
    val primaryAddress = getPrimaryInternetAddress() ?: return null
    return RecipientEntity(
      email = primaryAddress.address,
      name = primaryAddress.personal
    )
  }

  fun toPublicKeyEntity(recipient: String): PublicKeyEntity {
    return PublicKeyEntity(
      recipient = recipient,
      fingerprint = fingerprint,
      publicKey = publicKey.toByteArray()
    )
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PgpKeyDetails

    if (isFullyDecrypted != other.isFullyDecrypted) return false
    if (isFullyEncrypted != other.isFullyEncrypted) return false
    if (isRevoked != other.isRevoked) return false
    if (privateKey != other.privateKey) return false
    if (publicKey != other.publicKey) return false
    if (users != other.users) return false
    if (ids != other.ids) return false
    if (created != other.created) return false
    if (lastModified != other.lastModified) return false
    if (expiration != other.expiration) return false
    if (algo != other.algo) return false
    if (tempPassphrase != null) {
      if (other.tempPassphrase == null) return false
      if (!tempPassphrase.contentEquals(other.tempPassphrase)) return false
    } else if (other.tempPassphrase != null) return false
    if (passphraseType != other.passphraseType) return false

    return true
  }

  override fun hashCode(): Int {
    var result = isFullyDecrypted.hashCode()
    result = 31 * result + isFullyEncrypted.hashCode()
    result = 31 * result + isRevoked.hashCode()
    result = 31 * result + (privateKey?.hashCode() ?: 0)
    result = 31 * result + publicKey.hashCode()
    result = 31 * result + users.hashCode()
    result = 31 * result + ids.hashCode()
    result = 31 * result + created.hashCode()
    result = 31 * result + (lastModified?.hashCode() ?: 0)
    result = 31 * result + (expiration?.hashCode() ?: 0)
    result = 31 * result + algo.hashCode()
    result = 31 * result + (tempPassphrase?.contentHashCode() ?: 0)
    result = 31 * result + (passphraseType?.hashCode() ?: 0)
    return result
  }

  companion object CREATOR : Parcelable.Creator<PgpKeyDetails> {
    override fun createFromParcel(parcel: Parcel): PgpKeyDetails = PgpKeyDetails(parcel)
    override fun newArray(size: Int): Array<PgpKeyDetails?> = arrayOfNulls(size)
  }
}
