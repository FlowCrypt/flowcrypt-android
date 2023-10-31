/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.database.entity.relation

import android.os.Parcelable
import androidx.room.Embedded
import androidx.room.Relation
import com.flowcrypt.email.database.entity.PublicKeyEntity
import com.flowcrypt.email.database.entity.RecipientEntity
import kotlinx.parcelize.Parcelize

/**
 * @author Denys Bondarenko
 */
@Parcelize
data class RecipientWithPubKeys(
  @Embedded val recipient: RecipientEntity,
  @Relation(
    parentColumn = "email",
    entityColumn = "recipient",
    entity = PublicKeyEntity::class
  )
  val publicKeys: List<PublicKeyEntity>
) : Parcelable {
  fun hasAtLeastOnePubKey(): Boolean {
    return publicKeys.isNotEmpty()
  }

  fun hasNotExpiredPubKey(): Boolean {
    return publicKeys.any { it.pgpKeyRingDetails?.isExpired?.not() ?: false }
  }

  fun hasNotRevokedPubKey(): Boolean {
    return publicKeys.any { it.pgpKeyRingDetails?.isRevoked?.not() ?: false }
  }

  fun hasUsablePubKey(): Boolean {
    return publicKeys.any { (it.isNotUsable ?: false).not() }
  }
}
