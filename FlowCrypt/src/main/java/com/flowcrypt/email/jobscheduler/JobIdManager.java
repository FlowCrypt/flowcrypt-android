/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jobscheduler;

import android.app.job.JobInfo;

/**
 * This class describes job id constants for {@link JobInfo.Builder}.
 *
 * @author Denis Bondarenko
 *         Date: 21.06.2018
 *         Time: 16:27
 *         E-mail: DenBond7@gmail.com
 */
public class JobIdManager {
    public static final int JOB_TYPE_SYNC = 1;
    public static final int JOB_TYPE_ACTION_QUEUE = 2;
    public static final int JOB_TYPE_EMAIL_AND_NAME_UPDATE = 3;
    public static final int JOB_TYPE_PREPARE_OUT_GOING_MESSAGE = 4;
    public static final int JOB_TYPE_SEND_MESSAGES = 5;
}
