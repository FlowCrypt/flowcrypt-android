/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules


import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.model.PgpContact
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * This [org.junit.Rule] can be used for saving [PgpContact] to the local database.
 *
 * @author Denis Bondarenko
 * Date: 2/20/19
 * Time: 5:16 PM
 * E-mail: DenBond7@gmail.com
 */
class AddContactsToDatabaseRule(val pgpContacts: List<PgpContact>) : BaseRule() {

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      override fun evaluate() {
        FlowCryptRoomDatabase.getDatabase(targetContext).contactsDao().insert(
            pgpContacts.map { it.toContactEntity() })
        base.evaluate()
      }
    }
  }
}
