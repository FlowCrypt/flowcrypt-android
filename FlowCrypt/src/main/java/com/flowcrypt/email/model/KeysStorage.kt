/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import androidx.annotation.Keep
import com.flowcrypt.email.database.entity.KeyEntity

@Keep
interface KeysStorage {

  fun getAllPgpPrivateKeys(): List<KeyEntity>

  fun findPgpContact(longId: String?): PgpContact?

  // if two contacts requested and only one found, will still return list of 2:
  // eg [PgpContact, null] or [null, PgpContact] depending which one is missing
  fun findPgpContacts(longId: Array<String>): List<*>

  fun getPgpPrivateKey(longId: String?): KeyEntity?

  // if 2 keys requested and only one found, will return list of 1: [PgpKey]
  fun getFilteredPgpPrivateKeys(longIds: Array<String>): List<KeyEntity>
}


