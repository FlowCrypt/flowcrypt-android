/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.model

import android.os.Parcel
import android.os.Parcelable

/**
 * This class describes service info details. Can be used when create a new messages.
 *
 * @author Denis Bondarenko
 * Date: 24.11.2017
 * Time: 10:57
 * E-mail: DenBond7@gmail.com
 */

data class ServiceInfo constructor(val isToFieldEditable: Boolean = false,
                                   val isFromFieldEditable: Boolean = false,
                                   val isMsgEditable: Boolean = false,
                                   val isSubjectEditable: Boolean = false,
                                   val isMsgTypeSwitchable: Boolean = false,
                                   val hasAbilityToAddNewAtt: Boolean = false,
                                   val systemMsg: String? = null,
                                   val atts: List<AttachmentInfo>? = null) : Parcelable {

  constructor(parcel: Parcel) : this(parcel.readByte() != 0.toByte(),
      parcel.readByte() != 0.toByte(),
      parcel.readByte() != 0.toByte(),
      parcel.readByte() != 0.toByte(),
      parcel.readByte() != 0.toByte(),
      parcel.readByte() != 0.toByte(),
      parcel.readString(),
      mutableListOf<AttachmentInfo>().apply { parcel.readTypedList(this, AttachmentInfo.CREATOR) })

  override fun describeContents(): Int {
    return 0
  }

  override fun writeToParcel(dest: Parcel, flags: Int) {
    with(dest) {
      writeByte(if (isToFieldEditable) 1.toByte() else 0.toByte())
      writeByte(if (isFromFieldEditable) 1.toByte() else 0.toByte())
      writeByte(if (isMsgEditable) 1.toByte() else 0.toByte())
      writeByte(if (isSubjectEditable) 1.toByte() else 0.toByte())
      writeByte(if (isMsgTypeSwitchable) 1.toByte() else 0.toByte())
      writeByte(if (hasAbilityToAddNewAtt) 1.toByte() else 0.toByte())
      writeString(systemMsg)
      writeTypedList(atts)
    }
  }

  fun hasAbilityToAddNewAtt(): Boolean {
    return hasAbilityToAddNewAtt
  }

  companion object {
    @JvmField
    @Suppress("unused")
    val CREATOR: Parcelable.Creator<ServiceInfo> = object : Parcelable.Creator<ServiceInfo> {
      override fun createFromParcel(source: Parcel): ServiceInfo = ServiceInfo(source)
      override fun newArray(size: Int): Array<ServiceInfo?> = arrayOfNulls(size)
    }
  }
}
