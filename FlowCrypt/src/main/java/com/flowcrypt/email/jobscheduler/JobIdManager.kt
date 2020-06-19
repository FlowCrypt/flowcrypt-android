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

    const val JOB_TYPE_SYNC = 1
    const val JOB_TYPE_ACTION_QUEUE = 2
    const val JOB_TYPE_EMAIL_AND_NAME_UPDATE = 3
    const val JOB_TYPE_PREPARE_OUT_GOING_MESSAGE = 4
    const val JOB_TYPE_SEND_MESSAGES = 5
    const val JOB_TYPE_DOWNLOAD_FORWARDED_ATTACHMENTS = 6
    const val JOB_TYPE_FEEDBACK = 6
    const val JOB_TYPE_MOVE_MESSAGES = 7
  }
}
