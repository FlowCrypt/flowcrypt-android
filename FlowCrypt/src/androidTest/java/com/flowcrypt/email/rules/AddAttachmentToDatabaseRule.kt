/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AttachmentEntity

/**
 * @author Denys Bondarenko
 */
class AddAttachmentToDatabaseRule(val attInfo: AttachmentInfo) : BaseRule() {
  override fun execute() {
    saveAttToDatabase()
  }

  private fun saveAttToDatabase() {
    AttachmentEntity.fromAttInfo(attInfo)?.let {
      FlowCryptRoomDatabase.getDatabase(targetContext).attachmentDao().insert(it)
    }
  }
}
