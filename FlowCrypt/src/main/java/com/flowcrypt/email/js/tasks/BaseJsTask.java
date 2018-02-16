/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js.tasks;

import com.flowcrypt.email.api.email.sync.SyncErrorTypes;
import com.flowcrypt.email.js.JsListener;
import com.sun.mail.util.MailConnectException;

/**
 * A base implementation of {@link JsTask}
 *
 * @author Denis Bondarenko
 *         Date: 16.02.2018
 *         Time: 10:39
 *         E-mail: DenBond7@gmail.com
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
            int errorType;

            if (e instanceof MailConnectException) {
                errorType = SyncErrorTypes.CONNECTION_TO_STORE_IS_LOST;
            } else {
                errorType = SyncErrorTypes.TASK_RUNNING_ERROR;
            }

            jsListener.onError(errorType, e, ownerKey, requestCode);
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
