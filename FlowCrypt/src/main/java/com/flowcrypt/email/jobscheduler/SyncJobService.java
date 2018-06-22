/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jobscheduler;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.flowcrypt.email.util.exception.ExceptionUtil;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

/**
 * This is an implementation of {@link JobService}. Here we are going to do sync INBOX folder of an active account.
 *
 * @author Denis Bondarenko
 * Date: 20.06.2018
 * Time: 12:40
 * E-mail: DenBond7@gmail.com
 */
public class SyncJobService extends JobService {
    private static final long INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(15);
    private static final String TAG = SyncJobService.class.getSimpleName();

    public static void schedule(Context context) {
        ComponentName serviceName = new ComponentName(context, SyncJobService.class);
        JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(JobIdManager.JOB_TYPE_SYNC, serviceName)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(INTERVAL_MILLIS)
                .setPersisted(true);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            jobInfoBuilder.setRequiresBatteryNotLow(true);
        }

        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (scheduler != null) {
            int result = scheduler.schedule(jobInfoBuilder.build());
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "A job scheduled successfully");
            } else {
                String errorMessage = "Error. Can't schedule a job";
                Log.e(TAG, errorMessage);
                ExceptionUtil.handleError(new IllegalStateException(errorMessage));
            }
        }
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d(TAG, "onStartJob");
        new SyncJobTask(this).execute(jobParameters);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "onStopJob");
        jobFinished(jobParameters, true);
        return false;
    }

    /**
     * This is a worker. Here we will do sync in the background thread. If the sync will be failed we'll schedule it
     * again.
     */
    private static class SyncJobTask extends AsyncTask<JobParameters, Void, JobParameters> {
        private final WeakReference<SyncJobService> syncJobServiceWeakReference;

        SyncJobTask(SyncJobService syncJobService) {
            this.syncJobServiceWeakReference = new WeakReference<>(syncJobService);
        }

        @Override
        protected JobParameters doInBackground(JobParameters... params) {
            Log.d(TAG, "doInBackground");

            return params[0];
        }

        @Override
        protected void onPostExecute(JobParameters jobParameters) {
            Log.d(TAG, "onPostExecute");
            try {
                if (syncJobServiceWeakReference.get() != null) {
                    syncJobServiceWeakReference.get().jobFinished(jobParameters, false);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }
    }
}
