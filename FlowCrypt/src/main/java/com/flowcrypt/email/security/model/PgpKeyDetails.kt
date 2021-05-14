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
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.util.exception.FlowCryptException
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*
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
data class PgpKeyDetails constructor(@Expose val isFullyDecrypted: Boolean,
                                     @Expose val isFullyEncrypted: Boolean,
                                     @Expose @SerializedName("private") val privateKey: String?,
                                     @Expose @SerializedName("public") val publicKey: String,
                                     @Expose val users: List<String>,
                                     @Expose val ids: List<KeyId>,
                                     @Expose val created: Long,
                                     @Expose val lastModified: Long,
                                     @Expose val expiration: Long? = null,
                                     @Expose val algo: Algo,
                                     var tempPassphrase: CharArray? = null) : Parcelable {

  val primaryPgpContact: PgpContact
    get() = determinePrimaryPgpContact()
  val pgpContacts: ArrayList<PgpContact>
    get() = determinePgpContacts()
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
      source.readString(),
      source.readString() ?: throw IllegalArgumentException("pubkey can't be null"),
      source.createStringArrayList() ?: throw NullPointerException(),
      source.createTypedArrayList(KeyId.CREATOR) ?: throw NullPointerException(),
      source.readLong(),
      source.readLong(),
      source.readValue(Long::class.java.classLoader) as Long?,
      source.readParcelable<Algo>(Algo::class.java.classLoader) ?: throw NullPointerException(),
      source.createCharArray()
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeValue(isFullyDecrypted)
    writeValue(isFullyEncrypted)
    writeString(privateKey)
    writeString(publicKey)
    writeStringList(users)
    writeTypedList(ids)
    writeLong(created)
    writeLong(lastModified)
    writeValue(expiration)
    writeParcelable(algo, 0)
    writeCharArray(tempPassphrase)
  }

  private fun determinePrimaryPgpContact(): PgpContact {
    val address = users.first()
    val fingerprintFromKeyId = ids.first().fingerprint
    var email: String? = null
    var name: String? = null
    try {
      val internetAddresses = InternetAddress.parse(address)
      email = internetAddresses.first().address
      name = internetAddresses.first().personal
    } catch (e: AddressException) {
      e.printStackTrace()
      val pattern = Patterns.EMAIL_ADDRESS
      val matcher = pattern.matcher(users.first())
      if (matcher.find()) {
        email = matcher.group()
        name = email
      }
    }

    if (email == null) {
      throw object : FlowCryptException("No user ids with mail address") {}
    }

    return PgpContact(
        email = email.toLowerCase(Locale.US),
        name = name,
        pubkey = publicKey,
        hasPgp = true,
        client = null,
        fingerprint = fingerprintFromKeyId
    )
  }

  private fun determinePgpContacts(): ArrayList<PgpContact> {
    val pgpContacts = ArrayList<PgpContact>()
    for (user in users) {
      try {
        val internetAddresses = InternetAddress.parse(user)

        for (internetAddress in internetAddresses) {
          val email = internetAddress.address.toLowerCase(Locale.US)
          val name = internetAddress.personal

          pgpContacts.add(PgpContact(email, name))
        }
      } catch (e: AddressException) {
        e.printStackTrace()
      }
    }

    return pgpContacts
  }

  private fun parseMimeAddresses(): List<InternetAddress> {
    val results = mutableListOf<InternetAddress>()

    for (user in users) {
      try {
        results.addAll(listOf(*InternetAddress.parse(user)))
      } catch (e: AddressException) {
        //do nothing
      }
    }

    return results
  }

  fun toKeyEntity(accountEntity: AccountEntity): KeyEntity {
    return KeyEntity(
        fingerprint = fingerprint,
        account = accountEntity.email.toLowerCase(Locale.US),
        accountType = accountEntity.accountType,
        source = PrivateKeySourceType.BACKUP.toString(),
        publicKey = publicKey.toByteArray(),
        privateKey = privateKey?.toByteArray()
            ?: throw NullPointerException("nodeKeyDetails.privateKey == null"),
        storedPassphrase = tempPassphrase?.let { String(it) })
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PgpKeyDetails

    if (isFullyDecrypted != other.isFullyDecrypted) return false
    if (isFullyEncrypted != other.isFullyEncrypted) return false
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

    return true
  }

  override fun hashCode(): Int {
    var result = isFullyDecrypted.hashCode()
    result = 31 * result + isFullyEncrypted.hashCode()
    result = 31 * result + (privateKey?.hashCode() ?: 0)
    result = 31 * result + publicKey.hashCode()
    result = 31 * result + users.hashCode()
    result = 31 * result + ids.hashCode()
    result = 31 * result + created.hashCode()
    result = 31 * result + lastModified.hashCode()
    result = 31 * result + (expiration?.hashCode() ?: 0)
    result = 31 * result + algo.hashCode()
    result = 31 * result + (tempPassphrase?.contentHashCode() ?: 0)
    return result
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<PgpKeyDetails> = object : Parcelable.Creator<PgpKeyDetails> {
      override fun createFromParcel(source: Parcel): PgpKeyDetails = PgpKeyDetails(source)
      override fun newArray(size: Int): Array<PgpKeyDetails?> = arrayOfNulls(size)
    }
  }
}
