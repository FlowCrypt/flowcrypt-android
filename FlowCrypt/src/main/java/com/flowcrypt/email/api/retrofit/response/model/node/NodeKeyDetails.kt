/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node

import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import android.util.Patterns
import com.flowcrypt.email.model.PgpContact
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*
import java.util.concurrent.TimeUnit
import javax.mail.internet.AddressException
import javax.mail.internet.InternetAddress

/**
 * @author Denis Bondarenko
 * Date: 2/11/19
 * Time: 1:23 PM
 * E-mail: DenBond7@gmail.com
 */
data class NodeKeyDetails constructor(@Expose val isDecrypted: Boolean?,
                                      @Expose @SerializedName("private") val privateKey: String?,
                                      @Expose @SerializedName("public") val publicKey: String?,
                                      @Expose val users: List<String>?,
                                      @Expose val ids: List<KeyId>?,
                                      @Expose val created: Long,
                                      @Expose val algo: Algo?) : Parcelable {

  val primaryPgpContact: PgpContact = determinePrimaryPgpContact()
  val pgpContacts: ArrayList<PgpContact> = determinePgpContacts()
  val longId: String? = ids?.first()?.longId
  val fingerprint: String? = ids?.first()?.fingerprint
  val keywords: String? = ids?.first()?.keywords
  val isPrivate: Boolean = !TextUtils.isEmpty(privateKey)

  fun getCreatedInMilliseconds(): Long {
    return TimeUnit.MILLISECONDS.convert(created, TimeUnit.SECONDS)
  }

  constructor(source: Parcel) : this(
      source.readValue(Boolean::class.java.classLoader) as Boolean?,
      source.readString(),
      source.readString(),
      source.createStringArrayList(),
      source.createTypedArrayList(KeyId.CREATOR),
      source.readLong(),
      source.readParcelable<Algo>(Algo::class.java.classLoader)
  )

  override fun describeContents() = 0

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeValue(isDecrypted)
    writeString(privateKey)
    writeString(publicKey)
    writeStringList(users)
    writeTypedList(ids)
    writeLong(created)
    writeParcelable(algo, 0)
  }

  private fun determinePrimaryPgpContact(): PgpContact {
    val address = users?.first()

    address?.let {
      val (fingerprint1, longId1, _, keywords1) = ids!!.first()
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

      return PgpContact(email!!, name, publicKey, !TextUtils.isEmpty(publicKey), null,
          fingerprint1, longId1, keywords1, 0)
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
            val email = internetAddress.address
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

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<NodeKeyDetails> = object : Parcelable.Creator<NodeKeyDetails> {
      override fun createFromParcel(source: Parcel): NodeKeyDetails = NodeKeyDetails(source)
      override fun newArray(size: Int): Array<NodeKeyDetails?> = arrayOfNulls(size)
    }
  }
}
