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
 * @author Denis Bondarenko
 * Date: 17.08.2018
 * Time: 09:19
 * E-mail: DenBond7@gmail.com
 */
class AddLabelsToDatabaseRule(
  val account: AccountEntity, val folders: List<LocalFolder>
) : BaseRule() {
  override fun execute() {
    saveLabelsToDatabase()
  }

  private fun saveLabelsToDatabase() {
    val labels = mutableListOf<LabelEntity>()
    for (folder in folders) {
      labels.add(LabelEntity.genLabel(account, folder))
    }

    FlowCryptRoomDatabase.getDatabase(targetContext).labelDao().insert(labels)
  }
}
