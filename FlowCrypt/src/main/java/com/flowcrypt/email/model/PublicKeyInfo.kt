/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys

/**
 * This class describes information about some public key.
 *
 * @author Denis Bondarenko
 * Date: 13.05.2018
 * Time: 10:22
 * E-mail: DenBond7@gmail.com
 */
data class PublicKeyInfo constructor(
  val fingerprint: String,
  val keyOwner: String,
  var recipientWithPubKeys: RecipientWithPubKeys? = null,
  val publicKey: String
) {
  fun hasPgp(): Boolean {
    return recipientWithPubKeys?.publicKeys?.isNotEmpty() == true
  }

  fun toRecipientEntity(): RecipientEntity {
    return RecipientEntity(
      email = keyOwner.lowercase()
    )
  }

  fun toPgpContact(): PgpContact {
    return PgpContact(
      email = keyOwner,
      name = null,
      pubkey = publicKey,
      hasPgp = true,
      client = null,
      fingerprint = fingerprint,
      lastUse = 0
    )
  }
}
