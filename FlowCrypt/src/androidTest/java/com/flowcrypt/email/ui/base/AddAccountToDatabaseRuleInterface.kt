/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.base

import com.flowcrypt.email.rules.AddAccountToDatabaseRule

/**
 * @author Denys Bondarenko
 */
interface AddAccountToDatabaseRuleInterface {
  val addAccountToDatabaseRule
    get() = AddAccountToDatabaseRule()
}
