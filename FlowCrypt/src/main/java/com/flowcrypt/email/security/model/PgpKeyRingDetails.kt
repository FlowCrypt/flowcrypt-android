/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.security.model

import android.content.Context
import android.content.res.ColorStateList
import android.os.Parcelable
import android.util.Patterns
import androidx.core.content.ContextCompat
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.model.KeyImportDetails
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import jakarta.mail.internet.AddressException
import jakarta.mail.internet.InternetAddress
import kotlinx.parcelize.Parcelize
import org.pgpainless.algorithm.KeyFlag

/**
 * This class collects base info of [org.bouncycastle.openpgp.PGPKeyRing]
 * that can be used via [Parcelable] mechanism.
 *
 * @author Denys Bondarenko
 */
@Parcelize
data class PgpKeyRingDetails constructor(
  @Expose val isFullyDecrypted: Boolean,
  @Expose val isFullyEncrypted: Boolean,
  @Expose val isRevoked: Boolean,
  @Expose val usableForEncryption: Boolean,
  @Expose val usableForSigning: Boolean,
  @Expose @SerializedName("private") val privateKey: String?,
  @Expose @SerializedName("public") val publicKey: String,
  @Expose val users: List<String>,
  @Expose val primaryUserId: String?,
  @Expose val ids: List<KeyId>,
  @Expose val created: Long,
  @Expose val lastModified: Long,
  @Expose val expiration: Long? = null,
  @Expose val algo: Algo,
  @Expose val primaryKeyId: Long,
  @Expose val possibilities: Set<Int>,
  var tempPassphrase: CharArray? = null,
  var passphraseType: KeyEntity.PassphraseType? = null,
  var importSourceType: KeyImportDetails.SourceType? = null
) : Parcelable {
  val fingerprint: String
    get() = ids.first().fingerprint
  val isPrivate: Boolean
    get() = privateKey != null

  val isExpired: Boolean
    get() = expiration != null && (System.currentTimeMillis() > expiration)

  val mimeAddresses: List<InternetAddress>
    get() = parseMimeAddresses()

  val primaryMimeAddress: InternetAddress?
    get() = primaryUserId?.let {
      try {
        InternetAddress.parse(it).firstOrNull()
      } catch (e: Exception) {
        null
      }
    }

  val isPartiallyEncrypted: Boolean
    get() {
      return !isFullyDecrypted && !isFullyEncrypted
    }

  fun getUserIdsAsSingleString(): String {
    return mimeAddresses.joinToString { it.address }
  }

  fun getPrimaryInternetAddress(): InternetAddress? {
    return mimeAddresses.firstOrNull()
  }

  fun isNewerThan(pgpKeyRingDetails: PgpKeyRingDetails?): Boolean {
    val existingLastModified = lastModified
    val providedLastModified = pgpKeyRingDetails?.lastModified ?: 0
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
      privateKey = privateKey?.toByteArray()
        ?: throw NullPointerException("pgpKeyRingDetails.privateKey == null"),
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

  fun getColorStateListDependsOnStatus(context: Context): ColorStateList? {
    return ContextCompat.getColorStateList(
      context, when {
        usableForEncryption -> R.color.colorPrimary
        usableForSigning -> R.color.colorAccent
        isRevoked -> R.color.red
        isExpired || isPartiallyEncrypted -> R.color.orange
        else -> R.color.gray
      }
    )
  }

  fun getStatusIcon(): Int {
    return when {
      !isRevoked
          && !isExpired
          && !isPartiallyEncrypted -> R.drawable.ic_baseline_gpp_good_16

      else -> R.drawable.ic_outline_warning_amber_16
    }
  }

  fun getStatusText(context: Context): String {
    return when {
      isRevoked -> context.getString(R.string.revoked)
      isExpired -> context.getString(R.string.expired)
      isPartiallyEncrypted -> context.getString(R.string.not_valid)
      else -> context.getString(R.string.valid)
    }
  }

  fun hasPossibility(keyFlag: KeyFlag): Boolean {
    return possibilities.contains(keyFlag.flag)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PgpKeyRingDetails

    if (isFullyDecrypted != other.isFullyDecrypted) return false
    if (isFullyEncrypted != other.isFullyEncrypted) return false
    if (isRevoked != other.isRevoked) return false
    if (usableForEncryption != other.usableForEncryption) return false
    if (usableForSigning != other.usableForSigning) return false
    if (privateKey != other.privateKey) return false
    if (publicKey != other.publicKey) return false
    if (users != other.users) return false
    if (primaryUserId != other.primaryUserId) return false
    if (ids != other.ids) return false
    if (created != other.created) return false
    if (lastModified != other.lastModified) return false
    if (expiration != other.expiration) return false
    if (algo != other.algo) return false
    if (primaryKeyId != other.primaryKeyId) return false
    if (possibilities != other.possibilities) return false
    if (tempPassphrase != null) {
      if (other.tempPassphrase == null) return false
      if (!tempPassphrase.contentEquals(other.tempPassphrase)) return false
    } else if (other.tempPassphrase != null) return false
    if (passphraseType != other.passphraseType) return false
    if (importSourceType != other.importSourceType) return false

    return true
  }

  override fun hashCode(): Int {
    var result = isFullyDecrypted.hashCode()
    result = 31 * result + isFullyEncrypted.hashCode()
    result = 31 * result + isRevoked.hashCode()
    result = 31 * result + usableForEncryption.hashCode()
    result = 31 * result + usableForSigning.hashCode()
    result = 31 * result + (privateKey?.hashCode() ?: 0)
    result = 31 * result + publicKey.hashCode()
    result = 31 * result + users.hashCode()
    result = 31 * result + (primaryUserId?.hashCode() ?: 0)
    result = 31 * result + ids.hashCode()
    result = 31 * result + created.hashCode()
    result = 31 * result + lastModified.hashCode()
    result = 31 * result + (expiration?.hashCode() ?: 0)
    result = 31 * result + algo.hashCode()
    result = 31 * result + primaryKeyId.hashCode()
    result = 31 * result + possibilities.hashCode()
    result = 31 * result + (tempPassphrase?.contentHashCode() ?: 0)
    result = 31 * result + (passphraseType?.hashCode() ?: 0)
    result = 31 * result + (importSourceType?.hashCode() ?: 0)
    return result
  }
}
