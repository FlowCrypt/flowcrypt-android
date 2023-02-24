/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.relation.RecipientWithPubKeys

/**
 * This [org.junit.Rule] can be used for saving [RecipientWithPubKeys] to the local database.
 *
 * @author Denys Bondarenko
 */
class AddRecipientsToDatabaseRule(val list: List<RecipientWithPubKeys>) : BaseRule() {
  override fun execute() {
    val roomDatabase = FlowCryptRoomDatabase.getDatabase(targetContext)
    roomDatabase.recipientDao().insert(list.map { it.recipient })
    roomDatabase.pubKeyDao().insert(list.map { it.publicKeys }.flatten())
  }
}
