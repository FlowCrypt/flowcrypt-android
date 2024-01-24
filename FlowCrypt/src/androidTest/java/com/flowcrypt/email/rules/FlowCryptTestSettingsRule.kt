/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.junit.annotations.FlowCryptTestSettings
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Based on [org.junit.rules.TestName] rule
 *
 * @author Denys Bondarenko
 */
class FlowCryptTestSettingsRule : TestWatcher() {
  @Volatile
  var flowCryptTestSettings: FlowCryptTestSettings? = null
    private set

  override fun starting(d: Description) {
    flowCryptTestSettings =
      d.getAnnotation(FlowCryptTestSettings::class.java) ?: d.testClass.getAnnotation(
        FlowCryptTestSettings::class.java
      )
  }
}