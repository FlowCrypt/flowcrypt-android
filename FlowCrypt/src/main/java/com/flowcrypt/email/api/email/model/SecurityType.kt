/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt

/**
 * This class describes settings for some security type.
 *
 * @author Denis Bondarenko
 * Date: 13.09.2017
 * Time: 14:35
 * E-mail: DenBond7@gmail.com
 */
data class SecurityType constructor(
  val name: String = "",
  val opt: Option = Option.SSL_TLS,
  val defImapPort: Int = 993,
  val defSmtpPort: Int = 465
) : Parcelable {

  constructor(parcel: Parcel) : this(
    parcel.readString()!!,
    parcel.readParcelableViaExt(Option::class.java)!!,
    parcel.readInt(),
    parcel.readInt()
  )

  override fun toString(): String {
    return name
  }

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    dest.writeString(this.name)
    dest.writeParcelable(opt, flags)
    dest.writeInt(this.defImapPort)
    dest.writeInt(this.defSmtpPort)
  }

  enum class Option : Parcelable {
    NONE, SSL_TLS, STARTLS;

    companion object {
      @JvmField
      @Suppress("unused")
      val CREATOR: Parcelable.Creator<Option> = object : Parcelable.Creator<Option> {
        override fun createFromParcel(source: Parcel): Option = values()[source.readInt()]
        override fun newArray(size: Int): Array<Option?> = arrayOfNulls(size)
      }
    }

    override fun describeContents(): Int {
      return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
      dest.writeInt(ordinal)
    }
  }

  companion object {
    @JvmField
    @Suppress("unused")
    val CREATOR: Parcelable.Creator<SecurityType> = object : Parcelable.Creator<SecurityType> {
      override fun createFromParcel(source: Parcel): SecurityType = SecurityType(source)
      override fun newArray(size: Int): Array<SecurityType?> = arrayOfNulls(size)
    }

    /**
     * Generate a list which contains all available [SecurityType].
     *
     * @return The list of all available [SecurityType].
     */
    fun generateSecurityTypes(context: Context): MutableList<SecurityType> {
      val securityTypes = mutableListOf<SecurityType>()
      securityTypes.add(
        SecurityType(
          context.getString(R.string.ssl_tls), Option.SSL_TLS,
          JavaEmailConstants.SSL_IMAP_PORT, JavaEmailConstants.SSL_SMTP_PORT
        )
      )
      securityTypes.add(
        SecurityType(
          context.getString(R.string.startls), Option.STARTLS,
          JavaEmailConstants.DEFAULT_IMAP_PORT, JavaEmailConstants.STARTTLS_SMTP_PORT
        )
      )
      securityTypes.add(
        SecurityType(
          context.getString(R.string.none), Option.NONE,
          JavaEmailConstants.DEFAULT_IMAP_PORT, JavaEmailConstants.DEFAULT_SMTP_PORT
        )
      )
      return securityTypes
    }
  }
}
