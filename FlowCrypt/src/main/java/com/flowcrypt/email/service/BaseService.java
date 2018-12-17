/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service;

import android.app.Service;
import android.os.Messenger;

import com.flowcrypt.email.api.email.sync.SyncErrorTypes;

/**
 * The base {@link Service} class for a between threads communication.
 *
 * @author Denis Bondarenko
 * Date: 16.02.2018
 * Time: 16:30
 * E-mail: DenBond7@gmail.com
 */

public abstract class BaseService extends Service {
  public static final int REPLY_OK = 0;
  public static final int REPLY_ERROR = 1;
  public static final int REPLY_ACTION_PROGRESS = 2;

  public interface OnServiceCallback {
    /**
     * In this method we can handle response after run some action via {@link BaseService}
     *
     * @param requestCode The unique request code for identifies the some action. Must be unique
     *                    over all project.
     * @param resultCode  The result code of a run action.
     * @param obj         The object which returned from the service.
     */
    void onReplyReceived(int requestCode, int resultCode, Object obj);

    /**
     * In this method we can handle a progress state after run some action via {@link BaseService}
     *
     * @param requestCode The unique request code for identifies the some action. Must be unique
     *                    over all project.
     * @param resultCode  The result code of a run action.
     * @param obj         The object which returned from the service.
     */
    void onProgressReplyReceived(int requestCode, int resultCode, Object obj);

    /**
     * In this method we can handle en error after run some action via {@link BaseService}
     *
     * @param requestCode The unique request code for identifies the some action. Must be unique
     *                    over all project.
     * @param errorType   The {@link SyncErrorTypes}.
     * @param e           The exception which occurred.
     */
    void onErrorHappened(int requestCode, int errorType, Exception e);
  }

  /**
   * This class can be used to create a new action for {@link BaseService}
   */
  public static class Action {
    private String ownerKey;
    private int requestCode;
    private Object object;

    /**
     * The constructor.
     *
     * @param ownerKey    The name of reply to {@link Messenger}
     * @param requestCode The unique request code which identify some action
     * @param object      The object which will be passed to {@link BaseService}.
     */
    public Action(String ownerKey, int requestCode, Object object) {
      this.ownerKey = ownerKey;
      this.requestCode = requestCode;
      this.object = object;
    }

    @Override
    public String toString() {
      return "Action{" +
          "ownerKey='" + ownerKey + '\'' +
          ", requestCode=" + requestCode +
          ", object=" + object +
          '}';
    }

    public String getOwnerKey() {
      return ownerKey;
    }

    public int getRequestCode() {
      return requestCode;
    }

    public Object getObject() {
      return object;
    }
  }
}
