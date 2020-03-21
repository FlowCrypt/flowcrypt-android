/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.os.Parcel
import android.os.Parcelable
import com.flowcrypt.email.security.model.PrivateKeySourceType

/**
 * This class describes a details about the key. The key can be one of three
 * different types:
 *
 *  * [KeyDetails.Type.EMAIL]
 *  * [KeyDetails.Type.FILE]
 *  * [KeyDetails.Type.CLIPBOARD]
 *
 *
 * @author Denis Bondarenko
 * Date: 24.07.2017
 * Time: 12:56
 * E-mail: DenBond7@gmail.com
 */

data class KeyDetails constructor(val keyName: String? = null,
                                  val value: String,
                                  val bornType: Type,
                                  val isPrivateKey: Boolean = false,
                                  val pgpContact: PgpContact? = null) : Parcelable {

  constructor(value: String, bornType: Type) : this(null, value, bornType, true)
  constructor(value: String, bornType: Type, isPrivateKey: Boolean) : this(null, value, bornType, isPrivateKey, null)

  /**
   * The key available types.
   */
  enum class Type : Parcelable {
    EMAIL, FILE, CLIPBOARD, NEW;

    companion object {
      @JvmField
      val CREATOR: Parcelable.Creator<Type> = object : Parcelable.Creator<Type> {
        override fun createFromParcel(source: Parcel): Type = values()[source.readInt()]
        override fun newArray(size: Int): Array<Type?> = arrayOfNulls(size)
      }
    }

    override fun describeContents(): Int {
      return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
      dest.writeInt(ordinal)
    }

    fun toPrivateKeySourceTypeString(): String {
      return when (this) {
        EMAIL -> PrivateKeySourceType.BACKUP
        FILE, CLIPBOARD -> PrivateKeySourceType.IMPORT
        NEW -> PrivateKeySourceType.NEW
      }.toString()
    }
  }

  constructor(source: Parcel) : this(
      source.readString(),
      source.readString()!!,
      source.readParcelable(Type::class.java.classLoader)!!,
      source.readInt() == 1,
      source.readParcelable(PgpContact::class.java.classLoader)!!
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) =
      with(dest) {
        writeString(keyName)
        writeString(value)
        writeParcelable(bornType, flags)
        writeInt((if (isPrivateKey) 1 else 0))
        writeParcelable(pgpContact, flags)
      }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<KeyDetails> = object : Parcelable.Creator<KeyDetails> {
      override fun createFromParcel(source: Parcel): KeyDetails = KeyDetails(source)
      override fun newArray(size: Int): Array<KeyDetails?> = arrayOfNulls(size)
    }
  }
}
