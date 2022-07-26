/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
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
data class ServiceInfo constructor(
  val isToFieldEditable: Boolean = false,
  val isCcFieldEditable: Boolean = false,
  val isBccFieldEditable: Boolean = false,
  val isFromFieldEditable: Boolean = false,
  val isMsgEditable: Boolean = false,
  val isSubjectEditable: Boolean = false,
  val isMsgTypeSwitchable: Boolean = false,
  val hasAbilityToAddNewAtt: Boolean = false,
  val systemMsg: String? = null,
  val atts: List<AttachmentInfo>? = null
) : Parcelable {
  constructor(parcel: Parcel) : this(
    parcel.readByte() != 0.toByte(),
    parcel.readByte() != 0.toByte(),
    parcel.readByte() != 0.toByte(),
    parcel.readByte() != 0.toByte(),
    parcel.readByte() != 0.toByte(),
    parcel.readByte() != 0.toByte(),
    parcel.readByte() != 0.toByte(),
    parcel.readByte() != 0.toByte(),
    parcel.readString(),
    parcel.createTypedArrayList(AttachmentInfo.CREATOR)
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeByte(if (isToFieldEditable) 1 else 0)
    parcel.writeByte(if (isCcFieldEditable) 1 else 0)
    parcel.writeByte(if (isBccFieldEditable) 1 else 0)
    parcel.writeByte(if (isFromFieldEditable) 1 else 0)
    parcel.writeByte(if (isMsgEditable) 1 else 0)
    parcel.writeByte(if (isSubjectEditable) 1 else 0)
    parcel.writeByte(if (isMsgTypeSwitchable) 1 else 0)
    parcel.writeByte(if (hasAbilityToAddNewAtt) 1 else 0)
    parcel.writeString(systemMsg)
    parcel.writeTypedList(atts)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<ServiceInfo> {
    override fun createFromParcel(parcel: Parcel): ServiceInfo {
      return ServiceInfo(parcel)
    }

    override fun newArray(size: Int): Array<ServiceInfo?> {
      return arrayOfNulls(size)
    }
  }
}
