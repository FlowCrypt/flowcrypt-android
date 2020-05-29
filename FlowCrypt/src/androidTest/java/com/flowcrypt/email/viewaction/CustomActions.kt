/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.viewaction

import androidx.test.espresso.ViewAction

/**
 * @author Denis Bondarenko
 *         Date: 5/21/20
 *         Time: 3:00 PM
 *         E-mail: DenBond7@gmail.com
 */
class CustomActions {
  companion object {
    fun doNothing(): ViewAction {
      return EmptyAction()
    }
  }
}