/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.api.email.sync

/**
 * This class contains types of synchronization errors.
 *
 * @author DenBond7
 * Date: 16.06.2017
 * Time: 9:18
 * E-mail: DenBond7@gmail.com
 */

class SyncErrorTypes {
  companion object {
    const val UNKNOWN_ERROR = 0
    const val ACTION_FAILED_SHOW_TOAST = 1
    const val TASK_RUNNING_ERROR = 2
    const val CONNECTION_TO_STORE_IS_LOST = 3
  }
}
