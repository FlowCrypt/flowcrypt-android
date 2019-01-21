/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js.tasks;

import com.flowcrypt.email.js.JsInBackgroundManager;
import com.flowcrypt.email.js.JsListener;
import com.flowcrypt.email.js.core.Js;

/**
 * The task which will be run by {@link JsInBackgroundManager}
 *
 * @author DenBond7
 * Date: 15.02.2018
 * Time: 13:16
 * E-mail: DenBond7@gmail.com
 */

public interface JsTask {

  /**
   * Run the current task.
   *
   * @param js         An instance of {@link Js}
   * @param jsListener An instance of {@link JsListener}
   * @throws Exception An exception can be caused when the current action is running.
   */
  void runAction(Js js, JsListener jsListener) throws Exception;

  /**
   * This method will be called when an exception occurred while current task running.
   *
   * @param e          An occurred exception.
   * @param jsListener A {@link JsListener} object.
   */
  void handleException(Exception e, JsListener jsListener);

  /**
   * Get the task owner key.
   *
   * @return The task owner key. Can be used to identify who created this task.
   */
  String getOwnerKey();

  /**
   * Get the task request code.
   *
   * @return The task request id. Can be used to identify current task between other.
   */
  int getRequestCode();
}
