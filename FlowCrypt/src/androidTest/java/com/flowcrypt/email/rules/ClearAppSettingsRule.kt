/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.util.TestGeneralUtil
import java.io.IOException

/**
 * The rule which clears the application settings.
 *
 * @author Denis Bondarenko
 * Date: 27.12.2017
 * Time: 11:57
 * E-mail: DenBond7@gmail.com
 */

class ClearAppSettingsRule : BaseRule() {
  override fun execute() {
    clearApp()
  }

  /**
   * Clear the all application settings.
   *
   * @throws IOException Different errors can be occurred.
   */
  private fun clearApp() {
    TestGeneralUtil.clearApp(targetContext)
  }
}
