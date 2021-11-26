/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

/**
 * @author Denis Bondarenko
 *         Date: 11/24/21
 *         Time: 4:34 PM
 *         E-mail: DenBond7@gmail.com
 */
interface SyncInterface {
  fun useIndependentConnection(): Boolean = false
}
