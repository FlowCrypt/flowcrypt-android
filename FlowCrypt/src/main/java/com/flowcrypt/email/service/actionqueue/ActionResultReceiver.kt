/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service.actionqueue;

import android.app.IntentService;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import com.flowcrypt.email.service.actionqueue.actions.Action;
import com.flowcrypt.email.util.GeneralUtil;

/**
 * A custom implementation of {@link ResultReceiver} for a communication with {@link ActionQueueIntentService}
 *
 * @author Denis Bondarenko
 * Date: 31.01.2018
 * Time: 10:11
 * E-mail: DenBond7@gmail.com
 */

public class ActionResultReceiver extends ResultReceiver {
  public static final int RESULT_CODE_OK = 1;
  public static final int RESULT_CODE_ERROR = 0;

  private static final String EXTRA_KEY_ACTION = GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_ACTION",
      ActionResultReceiver.class);
  private static final String EXTRA_KEY_EXCEPTION = GeneralUtil.generateUniqueExtraKey("EXTRA_KEY_EXCEPTION",
      ActionResultReceiver.class);

  private ResultReceiverCallBack resultReceiverCallBack;

  /**
   * Create a new ResultReceive to receive results.  Your
   * {@link #onReceiveResult} method will be called from the thread running
   * <var>handler</var> if given, or from an arbitrary thread if null.
   *
   * @param handler It will receive results from the {@link IntentService}
   */
  public ActionResultReceiver(Handler handler) {
    super(handler);
  }

  public static Bundle generateSuccessBundle(Action action) {
    Bundle bundle = new Bundle();
    bundle.putParcelable(EXTRA_KEY_ACTION, action);
    return bundle;
  }

  public static Bundle generateErrorBundle(Action action, Exception e) {
    Bundle bundle = generateSuccessBundle(action);
    bundle.putSerializable(EXTRA_KEY_EXCEPTION, e);
    return bundle;
  }

  @Override
  protected void onReceiveResult(int resultCode, Bundle resultData) {
    if (resultReceiverCallBack != null) {
      Action action = resultData.getParcelable(EXTRA_KEY_ACTION);
      Exception e = (Exception) resultData.getSerializable(EXTRA_KEY_EXCEPTION);
      switch (resultCode) {
        case RESULT_CODE_OK:
          resultReceiverCallBack.onSuccess(action);
          break;

        case RESULT_CODE_ERROR:
          resultReceiverCallBack.onError(e, action);
          break;
      }
    }
  }

  public void setResultReceiverCallBack(ResultReceiverCallBack resultReceiverCallBack) {
    this.resultReceiverCallBack = resultReceiverCallBack;
  }

  /**
   * A callback for handling results from the {@link ActionQueueIntentService}
   */
  public interface ResultReceiverCallBack {
    /**
     * This method will be called if {@link Action} success.
     *
     * @param action An input {@link Action}
     */
    void onSuccess(Action action);

    /**
     * This method will be called if an error will happen when we try to run some {@link Action}.
     *
     * @param exception A happened exception
     * @param action    An input {@link Action}
     */
    void onError(Exception exception, Action action);
  }
}
