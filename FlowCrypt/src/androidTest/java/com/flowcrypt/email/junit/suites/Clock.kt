/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.junit.suites

/**
 * @author Denys Bondarenko
 */
internal fun interface Clock {
  fun nanoTime(): Long
}