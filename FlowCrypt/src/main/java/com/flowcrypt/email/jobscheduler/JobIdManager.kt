/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jobscheduler

import android.app.job.JobInfo

/**
 * This class describes job id constants for [JobInfo.Builder].
 *
 * @author Denys Bondarenko
 */
class JobIdManager {
  companion object {
    /**
     * Note: increase this field if you are modifying this file
     */
    const val JOB_MAX_ID = 10

    const val JOB_TYPE_PREPARE_OUT_GOING_MESSAGE = 4
  }
}
