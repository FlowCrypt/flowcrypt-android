/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

/**
 * This listener will be used to help debug idling issues for Instrumentation UI tests.
 *
 * @author Denis Bondarenko
 *         Date: 12/9/22
 *         Time: 11:00 AM
 *         E-mail: DenBond7@gmail.com
 */
interface IdlingCountListener {
  fun incrementIdlingCount()
  fun decrementIdlingCount()
}
