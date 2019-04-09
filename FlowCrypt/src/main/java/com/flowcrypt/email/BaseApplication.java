/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email;

import android.app.Application;
import android.app.job.JobScheduler;
import android.content.Context;

import com.flowcrypt.email.jobscheduler.JobIdManager;
import com.flowcrypt.email.jobscheduler.SyncJobService;
import com.flowcrypt.email.node.Node;
import com.flowcrypt.email.security.KeysStorageImpl;
import com.flowcrypt.email.ui.NotificationChannelManager;
import com.flowcrypt.email.util.GeneralUtil;

import androidx.fragment.app.FragmentManager;

/**
 * The application class for FlowCrypt. Base class for maintaining global application state.
 *
 * @author Denis Bondarenko
 * Date: 2/1/19
 * Time: 4:53 PM
 * E-mail: DenBond7@gmail.com
 */
public abstract class BaseApplication extends Application {

  public abstract void initAcra();

  public abstract void initLeakCanary();

  @Override
  public void onCreate() {
    super.onCreate();
    KeysStorageImpl.init(this);
    NotificationChannelManager.registerNotificationChannels(this);

    initLeakCanary();
    FragmentManager.enableDebugLogging(GeneralUtil.isDebugBuild());

    JobScheduler scheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
    if (scheduler != null) {
      scheduler.cancel(JobIdManager.JOB_TYPE_SYNC);
    }
    SyncJobService.schedule(this);

    Node.init(this);
  }

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(base);
    initAcra();
  }
}

