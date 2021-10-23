/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity.relation

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import com.flowcrypt.email.database.entity.ContactEntity
import com.flowcrypt.email.database.entity.PublicKeyEntity

/**
 * @author Denis Bondarenko
 *         Date: 10/21/21
 *         Time: 11:29 AM
 *         E-mail: DenBond7@gmail.com
 */
data class RecipientWithPubKeys(
  @Embedded val contact: ContactEntity,
  @Relation(
    parentColumn = "email",
    entityColumn = "recipient",
    entity = PublicKeyEntity::class,
    projection = ["fingerprint", "public_key"]
  )
  val publicKeys: List<PublicKeyEntity>
) : Parcelable {
  constructor(parcel: Parcel) : this(
    requireNotNull(parcel.readParcelable(ContactEntity::class.java.classLoader)),
    requireNotNull(parcel.createTypedArrayList(PublicKeyEntity))
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeParcelable(contact, flags)
    parcel.writeTypedList(publicKeys)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object CREATOR : Parcelable.Creator<RecipientWithPubKeys> {
    override fun createFromParcel(parcel: Parcel) = RecipientWithPubKeys(parcel)
    override fun newArray(size: Int): Array<RecipientWithPubKeys?> = arrayOfNulls(size)
  }
}
