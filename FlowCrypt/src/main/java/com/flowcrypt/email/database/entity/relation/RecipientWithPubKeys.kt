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
import com.flowcrypt.email.extensions.android.os.readParcelableViaExt

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
    entity = PublicKeyEntity::class
  )
  val publicKeys: List<PublicKeyEntity>
) : Parcelable {
  constructor(parcel: Parcel) : this(
    requireNotNull(parcel.readParcelableViaExt(RecipientEntity::class.java)),
    requireNotNull(parcel.createTypedArrayList(PublicKeyEntity))
  )

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeParcelable(recipient, flags)
    parcel.writeTypedList(publicKeys)
  }

  override fun describeContents(): Int {
    return 0
  }

  fun hasAtLeastOnePubKey(): Boolean {
    return publicKeys.isNotEmpty()
  }

  fun hasNotExpiredPubKey(): Boolean {
    return publicKeys.any { it.pgpKeyDetails?.isExpired?.not() ?: false }
  }

  fun hasNotRevokedPubKey(): Boolean {
    return publicKeys.any { it.pgpKeyDetails?.isRevoked?.not() ?: false }
  }

  fun hasUsablePubKey(): Boolean {
    return publicKeys.any { (it.isNotUsable ?: false).not() }
  }

  companion object CREATOR : Parcelable.Creator<RecipientWithPubKeys> {
    override fun createFromParcel(parcel: Parcel) = RecipientWithPubKeys(parcel)
    override fun newArray(size: Int): Array<RecipientWithPubKeys?> = arrayOfNulls(size)
  }
}
