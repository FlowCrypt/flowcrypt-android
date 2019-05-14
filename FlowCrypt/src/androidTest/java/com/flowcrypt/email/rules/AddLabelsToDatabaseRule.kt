/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.api.email.model.LocalFolder
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.imap.ImapLabelsDaoSource
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * @author Denis Bondarenko
 * Date: 17.08.2018
 * Time: 09:19
 * E-mail: DenBond7@gmail.com
 */
class AddLabelsToDatabaseRule(private val account: AccountDao, private val folders: List<LocalFolder>) : BaseRule() {

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      @Throws(Throwable::class)
      override fun evaluate() {
        saveLabelsToDatabase()
        base.evaluate()
      }
    }
  }

  private fun saveLabelsToDatabase() {
    ImapLabelsDaoSource().addRows(targetContext, account.email, folders)
  }
}

