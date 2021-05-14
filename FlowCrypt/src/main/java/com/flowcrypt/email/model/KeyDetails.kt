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
 *  * [KeyDetails.SourceType.EMAIL]
 *  * [KeyDetails.SourceType.FILE]
 *  * [KeyDetails.SourceType.CLIPBOARD]
 *
 *
 * @author Denis Bondarenko
 * Date: 24.07.2017
 * Time: 12:56
 * E-mail: DenBond7@gmail.com
 */
data class KeyDetails constructor(val keyName: String? = null,
                                  val value: String,
                                  val sourceType: SourceType,
                                  val isPrivateKey: Boolean = false,
                                  val pgpContact: PgpContact? = null) : Parcelable {

  constructor(value: String, sourceType: SourceType) : this(null, value, sourceType, true)
  constructor(value: String, sourceType: SourceType, isPrivateKey: Boolean) : this(null, value, sourceType, isPrivateKey, null)

  /**
   * The key available types.
   */
  enum class SourceType : Parcelable {
    EMAIL, FILE, CLIPBOARD, NEW, MANUAL_ENTERING;

    companion object {
      @JvmField
      val CREATOR: Parcelable.Creator<SourceType> = object : Parcelable.Creator<SourceType> {
        override fun createFromParcel(source: Parcel): SourceType = values()[source.readInt()]
        override fun newArray(size: Int): Array<SourceType?> = arrayOfNulls(size)
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
        FILE, CLIPBOARD, MANUAL_ENTERING -> PrivateKeySourceType.IMPORT
        NEW -> PrivateKeySourceType.NEW
      }.toString()
    }
  }

  constructor(source: Parcel) : this(
      source.readString(),
      source.readString()!!,
      source.readParcelable(SourceType::class.java.classLoader)!!,
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
        writeParcelable(sourceType, flags)
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
