/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.junit.filters

import com.flowcrypt.email.ReadyForCIAnnotation
import org.junit.runner.Description
import org.junit.runner.manipulation.Filter

/**
 * @author Denis Bondarenko
 *         Date: 2/17/21
 *         Time: 5:24 PM
 *         E-mail: DenBond7@gmail.com
 */
open class ReadyForCIFilter : Filter() {
  override fun shouldRun(description: Description?): Boolean {
    return description?.getAnnotation(ReadyForCIAnnotation::class.java) != null
  }

  override fun describe() = "Filter tests that are ready to be run on a CI server"
}