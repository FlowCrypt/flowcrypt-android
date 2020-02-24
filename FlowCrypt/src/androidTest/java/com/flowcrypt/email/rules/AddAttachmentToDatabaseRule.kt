/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.api.email.model.AttachmentInfo
import com.flowcrypt.email.database.dao.source.imap.AttachmentDao
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * @author Denis Bondarenko
 * Date: 3/14/19
 * Time: 5:54 PM
 * E-mail: DenBond7@gmail.com
 */
class AddAttachmentToDatabaseRule(val attInfo: AttachmentInfo) : BaseRule() {

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        saveAttToDatabase()
        base.evaluate()
      }
    }
  }

  private fun saveAttToDatabase() {
    AttachmentDao().addRow(targetContext, attInfo)
  }
}
