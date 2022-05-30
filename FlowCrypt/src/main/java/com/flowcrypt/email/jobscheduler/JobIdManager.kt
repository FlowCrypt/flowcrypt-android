/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jobscheduler

import android.app.job.JobInfo

/**
 * This class describes job id constants for [JobInfo.Builder].
 *
 * @author Denis Bondarenko
 * Date: 21.06.2018
 * Time: 16:27
 * E-mail: DenBond7@gmail.com
 */
class JobIdManager {
  companion object {
    /**
     * Note: increase this field if you are modifying this file
     */
    const val JOB_MAX_ID = 10

    const val JOB_TYPE_ACTION_QUEUE = 2
    const val JOB_TYPE_PREPARE_OUT_GOING_MESSAGE = 4
    const val JOB_TYPE_FEEDBACK = 5
  }
}
