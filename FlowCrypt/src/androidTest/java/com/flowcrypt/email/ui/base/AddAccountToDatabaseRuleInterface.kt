/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.base

import com.flowcrypt.email.rules.AddAccountToDatabaseRule

/**
 * @author Denis Bondarenko
 *         Date: 10/18/21
 *         Time: 5:27 PM
 *         E-mail: DenBond7@gmail.com
 */
interface AddAccountToDatabaseRuleInterface {
  val addAccountToDatabaseRule
    get() = AddAccountToDatabaseRule()
}
