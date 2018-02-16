/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.js;

import android.content.Context;

import com.flowcrypt.email.api.email.model.IncomingMessageInfo;

/**
 * This class can be used for communication with {@link JsInBackgroundManager}
 *
 * @author DenBond7
 *         Date: 15.02.2018
 *         Time: 13:18
 *         E-mail: DenBond7@gmail.com
 */
public interface JsListener {

    /**
     * Get the service context.
     *
     * @return The service context
     */
    Context getContext();

    /**
     * This method will be called when {@link Js} decrypted some message.
     *
     * @param ownerKey            The name of the reply to {@link android.os.Messenger}.
     * @param requestCode         The unique request code for the reply to {@link android.os.Messenger}.
     * @param incomingMessageInfo A decrypted message.
     */
    void onMessageDecrypted(String ownerKey, int requestCode, IncomingMessageInfo incomingMessageInfo);

    /**
     * Handle an error.
     *
     * @param errorType   The error type code.
     * @param e           The exception that occurred during running some action.
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     */
    void onError(int errorType, Exception e, String ownerKey, int requestCode);

    /**
     * This method can be used for debugging. Using this method we can identify a progress of some operation.
     *
     * @param ownerKey    The name of the reply to {@link android.os.Messenger}.
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     * @param resultCode  The unique result code for the reply which identifies the progress of some request.
     */
    void onActionProgress(String ownerKey, int requestCode, int resultCode);
}
