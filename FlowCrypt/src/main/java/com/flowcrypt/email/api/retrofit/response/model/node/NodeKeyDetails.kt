/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *  DenBond7
 *  Ivan Pizhenko
 */

package com.flowcrypt.email.api.retrofit.response.model.node

import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.Patterns
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.security.model.PrivateKeySourceType
import com.flowcrypt.email.util.exception.FlowCryptException
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress

/**
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 1:23 PM
 * E-mail: DenBond7@gmail.com
 */
data class NodeKeyDetails constructor(@Expose val isFullyDecrypted: Boolean?,
                                      @Expose val isFullyEncrypted: Boolean?,
                                      @Expose @SerializedName("private") val privateKey: String?,
                                      @Expose @SerializedName("public") val publicKey: String?,
                                      @Expose val users: List<String>?,
                                      @Expose val ids: List<KeyId>?,
                                      @Expose val created: Long,
                                      @Expose val lastModified: Long,
                                      @Expose val expiration: Long,
                                      @Expose val algo: Algo?,
                                      var passphrase: String?,
                                      var errorMsg: String?) : Parcelable {

  val primaryPgpContact: PgpContact
    get() = determinePrimaryPgpContact()
  val pgpContacts: ArrayList<PgpContact>
    get() = determinePgpContacts()
  val fingerprint: String?
    get() = ids?.first()?.fingerprint
  val isPrivate: Boolean
    get() = !TextUtils.isEmpty(privateKey)

  val isExpired: Boolean
    get() = expiration > 0 && (System.currentTimeMillis() / 1000 > expiration)

  val mimeAddresses: List<InternetAddress>
    get() = parseMimeAddresses()

  val isPartiallyEncrypted: Boolean
    get() {
      return isFullyDecrypted == false && isFullyEncrypted == false
    }

  constructor(source: Parcel) : this(
      source.readValue(Boolean::class.java.classLoader) as Boolean?,
      source.readValue(Boolean::class.java.classLoader) as Boolean?,
      source.readString(),
      source.readString(),
      source.createStringArrayList(),
      source.createTypedArrayList(KeyId.CREATOR),
      source.readLong(),
      source.readLong(),
      source.readLong(),
      source.readParcelable<Algo>(Algo::class.java.classLoader),
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
    writeLong(expiration)
    writeParcelable(algo, 0)
    writeString(passphrase)
    writeString(errorMsg)
  }

  private fun determinePrimaryPgpContact(): PgpContact {
    val address = users?.first()

    address?.let {
      val fingerprintFromKeyId = ids?.first()?.fingerprint
      var email: String? = null
      var name: String? = null
      try {
        val internetAddresses = InternetAddress.parse(it)
        email = internetAddresses.first().address
        name = internetAddresses.first().personal
      } catch (e: AddressException) {
        e.printStackTrace()
        val pattern = Patterns.EMAIL_ADDRESS
        val matcher = pattern.matcher(users!!.first())
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
          hasPgp = !TextUtils.isEmpty(publicKey),
          client = null,
          fingerprint = fingerprintFromKeyId
      )
    }

    return PgpContact("", "")
  }

  private fun determinePgpContacts(): ArrayList<PgpContact> {
    val pgpContacts = ArrayList<PgpContact>()

    users?.let {
      for (user in it) {
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
    }

    return pgpContacts
  }

  private fun parseMimeAddresses(): List<InternetAddress> {
    val results = mutableListOf<InternetAddress>()

    for (user in users ?: emptyList()) {
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
        fingerprint = fingerprint
            ?: throw NullPointerException("nodeKeyDetails.fingerprint == null"),
        account = accountEntity.email.toLowerCase(Locale.US),
        accountType = accountEntity.accountType,
        source = PrivateKeySourceType.BACKUP.toString(),
        publicKey = publicKey?.toByteArray()
            ?: throw NullPointerException("nodeKeyDetails.publicKey == null"),
        privateKey = privateKey?.toByteArray()
            ?: throw NullPointerException("nodeKeyDetails.privateKey == null"),
        storedPassphrase = passphrase)
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<NodeKeyDetails> = object : Parcelable.Creator<NodeKeyDetails> {
      override fun createFromParcel(source: Parcel): NodeKeyDetails = NodeKeyDetails(source)
      override fun newArray(size: Int): Array<NodeKeyDetails?> = arrayOfNulls(size)
    }
  }
}
