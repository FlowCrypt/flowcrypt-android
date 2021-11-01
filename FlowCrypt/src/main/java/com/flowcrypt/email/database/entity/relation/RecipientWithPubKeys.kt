/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity.relation

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.database.entity.RecipientEntity

/**
 * @author Denis Bondarenko
 *         Date: 10/21/21
 *         Time: 11:29 AM
 *         E-mail: DenBond7@gmail.com
 */
data class RecipientWithPubKeys(
  @Embedded val recipient: RecipientEntity,
  @Relation(
    parentColumn = "email",
    entityColumn = "recipient",
    entity = PublicKeyEntity::class,
    projection = ["fingerprint", "public_key"]
  )
  val publicKeys: List<PublicKeyEntity>
) : Parcelable {
  constructor(parcel: Parcel) : this(
    requireNotNull(parcel.readParcelable(RecipientEntity::class.java.classLoader)),
    requireNotNull(parcel.createTypedArrayList(PublicKeyEntity))
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeParcelable(recipient, flags)
    parcel.writeTypedList(publicKeys)
  }

  override fun describeContents(): Int {
    return 0
  }

  fun hasPgp(): Boolean {
    return publicKeys.isNotEmpty()
  }

  fun hasNotExpiredKeys(): Boolean {
    return publicKeys.any { it.pgpKeyDetails?.isExpired?.not() ?: false }
  }

  fun hasUsableKeys(): Boolean {
    return publicKeys.any { if (it.isNotUsable != null) it.isNotUsable?.not() ?: false else true }
  }

  companion object CREATOR : Parcelable.Creator<RecipientWithPubKeys> {
    override fun createFromParcel(parcel: Parcel) = RecipientWithPubKeys(parcel)
    override fun newArray(size: Int): Array<RecipientWithPubKeys?> = arrayOfNulls(size)
  }
}
