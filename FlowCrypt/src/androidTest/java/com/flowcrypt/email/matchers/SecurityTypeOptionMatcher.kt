/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers

import com.flowcrypt.email.api.email.model.SecurityType
import org.hamcrest.BaseMatcher
import org.hamcrest.Description

/**
 * @author Denys Bondarenko
 */
class SecurityTypeOptionMatcher(val option: SecurityType.Option) :
  BaseMatcher<SecurityType.Option>() {
  override fun describeTo(description: Description?) {
    description?.appendText("The input option = $option")
  }

  override fun matches(item: Any?): Boolean {
    return (item as? SecurityType)?.opt == option
  }
}
