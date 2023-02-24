/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.workmanager.sync

/**
 * @author Denys Bondarenko
 */
interface SyncInterface {
  fun useIndependentConnection(): Boolean = false
}
