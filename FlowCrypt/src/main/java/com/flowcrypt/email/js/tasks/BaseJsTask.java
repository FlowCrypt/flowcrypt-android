/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js.tasks;

import com.flowcrypt.email.js.JsListener;

/**
 * A base implementation of {@link JsTask}
 *
 * @author Denis Bondarenko
 * Date: 16.02.2018
 * Time: 10:39
 * E-mail: DenBond7@gmail.com
 */

public abstract class BaseJsTask implements JsTask {
  String ownerKey;
  int requestCode;

  public BaseJsTask(String ownerKey, int requestCode) {
    this.ownerKey = ownerKey;
    this.requestCode = requestCode;
  }

  @Override
  public void handleException(Exception e, JsListener jsListener) {
    if (jsListener != null) {
      jsListener.onError(JsErrorTypes.TASK_RUNNING_ERROR, e, ownerKey, requestCode);
    }
  }

  @Override
  public String getOwnerKey() {
    return ownerKey;
  }

  @Override
  public int getRequestCode() {
    return requestCode;
  }
}
