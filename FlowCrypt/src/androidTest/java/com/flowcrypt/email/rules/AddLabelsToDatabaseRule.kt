/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.LabelEntity

/**
 * @author Denys Bondarenko
 */
class AddLabelsToDatabaseRule(
  val account: AccountEntity, val folders: List<LocalFolder>
) : BaseRule() {
  override fun execute() {
    saveLabelsToDatabase()
  }

  private fun saveLabelsToDatabase() {
    val labels = folders.map { LabelEntity.genLabel(account, it) }
    FlowCryptRoomDatabase.getDatabase(targetContext).labelDao().insert(labels)
  }
}
