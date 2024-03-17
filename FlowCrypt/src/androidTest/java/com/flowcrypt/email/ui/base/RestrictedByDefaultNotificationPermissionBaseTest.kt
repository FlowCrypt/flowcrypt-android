/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.base

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.flowcrypt.email.base.BaseTest

/**
 * @author Denys Bondarenko
 */
abstract class RestrictedByDefaultNotificationPermissionBaseTest : BaseTest() {
  protected fun disallowNotificationPermission() {
    val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    val disallowPermissions = device.findObject(
      UiSelector()
        .clickable(true)
        .checkable(false)
        .index(1)
    )

    if (disallowPermissions.exists()) {
      disallowPermissions.click()
    }
  }
}