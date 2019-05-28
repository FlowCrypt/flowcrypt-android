/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import android.content.Context

import androidx.annotation.Keep

@Keep
interface KeysStorage {

  fun getAllPgpPrivateKeys(): List<PgpKeyInfo>

  fun findPgpContact(longid: String): PgpContact?

  // if two contacts requested and only one found, will still return list of 2:
  // eg [PgpContact, null] or [null, PgpContact] depending which one is missing
  fun findPgpContacts(longid: Array<String>): List<*>

  fun getPgpPrivateKey(longid: String): PgpKeyInfo?

  // if 2 keys requested and only one found, will return list of 1: [PgpKey]
  fun getFilteredPgpPrivateKeys(longid: Array<String>): List<PgpKeyInfo>

  fun getPassphrase(longid: String): String?

  fun refresh(context: Context)

}


