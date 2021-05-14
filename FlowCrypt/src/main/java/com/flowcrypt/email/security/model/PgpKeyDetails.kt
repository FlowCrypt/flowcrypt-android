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
                                     var passphrase: String?,
                                     var errorMsg: String?) : Parcelable {

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
      source.readString(),
      source.readString()
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
    writeString(passphrase)
    writeString(errorMsg)
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
        storedPassphrase = passphrase)
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<PgpKeyDetails> = object : Parcelable.Creator<PgpKeyDetails> {
      override fun createFromParcel(source: Parcel): PgpKeyDetails = PgpKeyDetails(source)
      override fun newArray(size: Int): Array<PgpKeyDetails?> = arrayOfNulls(size)
    }
  }
}
