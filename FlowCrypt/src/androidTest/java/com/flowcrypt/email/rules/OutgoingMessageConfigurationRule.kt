/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.rules

import com.flowcrypt.email.junit.annotations.OutgoingMessageConfiguration
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Based on [org.junit.rules.TestName] rule
 *
 * @author Denys Bondarenko
 */
class OutgoingMessageConfigurationRule : TestWatcher() {
  @Volatile
  var outgoingMessageConfiguration: OutgoingMessageConfiguration? = null
    private set

  override fun starting(d: Description) {
    outgoingMessageConfiguration =
      d.getAnnotation(OutgoingMessageConfiguration::class.java) ?: d.testClass.getAnnotation(
        OutgoingMessageConfiguration::class.java
      )
  }
}