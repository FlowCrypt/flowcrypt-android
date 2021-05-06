/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.model

import androidx.annotation.Keep
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.entity.KeyEntity
import org.pgpainless.key.protection.SecretKeyRingProtector

@Keep
interface KeysStorage {

  fun getAllPgpPrivateKeys(): List<KeyEntity>

  fun getPgpPrivateKey(longId: String?): KeyEntity?

  /**
   * if 2 keys requested and only one found, will return list of 1: [KeyEntity]
   */
  fun getFilteredPgpPrivateKeys(longIds: Array<String>): List<KeyEntity>

  /**
   * Get [List] of [KeyEntity] where each key has [PgpContact] with the given email.
   *
   * Note: this method returns a list of not-expired [KeyEntity] only
   */
  fun getPgpPrivateKeysByEmail(email: String?): List<KeyEntity>

  /**
   * Get [List] of [NodeKeyDetails] where each key has [PgpContact] with the given email.
   *
   * Note: this method returns a list of not-expired [NodeKeyDetails] only
   */
  fun getNodeKeyDetailsListByEmail(email: String?): List<NodeKeyDetails>

  fun getSecretKeyRingProtector(): SecretKeyRingProtector

  fun updateStateOfPassPhrasesInRAM()
}


