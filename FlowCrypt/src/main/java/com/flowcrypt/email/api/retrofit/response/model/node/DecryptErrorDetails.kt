/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.retrofit.response.model.node

import android.os.Parcel
import android.os.Parcelable

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

/**
 * @author Denis Bondarenko
 * Date: 3/26/19
 * Time: 3:30 PM
 * E-mail: DenBond7@gmail.com
 */
data class DecryptErrorDetails(@Expose val type: Type?,
                               @Expose val message: String?) : Parcelable {
  constructor(source: Parcel) : this(
      source.readParcelable<Type>(Type::class.java.classLoader),
      source.readString()
  )

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
    writeParcelable(type, flags)
    writeString(message)
  }

  companion object {
    @JvmField
    val CREATOR: Parcelable.Creator<DecryptErrorDetails> = object : Parcelable.Creator<DecryptErrorDetails> {
      override fun createFromParcel(source: Parcel): DecryptErrorDetails = DecryptErrorDetails(source)
      override fun newArray(size: Int): Array<DecryptErrorDetails?> = arrayOfNulls(size)
    }
  }

  enum class Type : Parcelable {
    UNKNOWN,

    @SerializedName("key_mismatch")
    KEY_MISMATCH,

    @SerializedName("use_password")
    USE_PASSWORD,

    @SerializedName("wrong_password")
    WRONG_PASSWORD,

    @SerializedName("no_mdc")
    NO_MDC,

    @SerializedName("need_passphrase")
    NEED_PASSPHRASE,

    @SerializedName("format")
    FORMAT,

    @SerializedName("other")
    OTHER;

    override fun describeContents(): Int {
      return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
      dest.writeInt(ordinal)
    }

    companion object {
      @JvmField
      val CREATOR: Parcelable.Creator<Type> = object : Parcelable.Creator<Type> {
        override fun createFromParcel(source: Parcel): Type = values()[source.readInt()]
        override fun newArray(size: Int): Array<Type?> = arrayOfNulls(size)
      }
    }
  }
}
