/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.flowcrypt.email.api.email.model.IncomingMessageInfo;
import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.JsInBackgroundManager;
import com.flowcrypt.email.js.JsListener;
import com.flowcrypt.email.util.exception.ExceptionUtil;

import org.acra.ACRA;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * This service will be run after the application started. This service creates a background thread where we will
 * initialize an instance of {@link Js} which will be used for long operations.
 *
 * @author Denis Bondarenko
 *         Date: 15.02.2018
 *         Time: 13:00
 *         E-mail: DenBond7@gmail.com
 */

public class JsBackgroundService extends BaseService implements JsListener {
    public static final int REPLY_RESULT_CODE_ACTION_OK = 0;

    public static final int MESSAGE_ADD_REPLY_MESSENGER = 1;
    public static final int MESSAGE_REMOVE_REPLY_MESSENGER = 2;
    public static final int MESSAGE_DECRYPT_MESSAGE = 3;

    private static final String TAG = JsBackgroundService.class.getSimpleName();
    /**
     * This {@link Messenger} is responsible for the receive intents from other client and
     * handles them.
     */
    private Messenger messenger;

    private Map<String, Messenger> replyToMessengers;

    private JsInBackgroundManager jsInBackgroundManager;

    private boolean isServiceStarted;

    public JsBackgroundService() {
        this.replyToMessengers = new HashMap<>();
    }

    /**
     * This method can bu used to start {@link JsBackgroundService}.
     *
     * @param context Interface to global information about an application environment.
     */
    public static void start(Context context) {
        context.startService(new Intent(context, JsBackgroundService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        jsInBackgroundManager = new JsInBackgroundManager();
        jsInBackgroundManager.setJsListener(this);

        messenger = new Messenger(new JsBackgroundService.IncomingHandler(jsInBackgroundManager, replyToMessengers));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand |intent =" + intent + "|flags = " + flags + "|startId = " + startId);
        isServiceStarted = true;

        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                default:
                    jsInBackgroundManager.init();
                    break;
            }
        } else {
            jsInBackgroundManager.init();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        if (jsInBackgroundManager != null) {
            jsInBackgroundManager.stop();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind:" + intent);
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(TAG, "onRebind:" + intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind:" + intent);

        if (!isServiceStarted) {
            JsBackgroundService.start(this);
        }
        return messenger.getBinder();
    }

    @Override
    public Context getContext() {
        return this.getApplicationContext();
    }

    @Override
    public void onMessageDecrypted(String ownerKey, int requestCode, IncomingMessageInfo incomingMessageInfo) {
        try {
            sendReply(ownerKey, requestCode, REPLY_RESULT_CODE_ACTION_OK, incomingMessageInfo);
        } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    @Override
    public void onError(int errorType, Exception e, String ownerKey, int requestCode) {
        Log.e(TAG, "onError: errorType" + errorType + "| e =" + e);
        try {
            if (replyToMessengers.containsKey(ownerKey)) {
                Messenger messenger = replyToMessengers.get(ownerKey);
                messenger.send(Message.obtain(null, REPLY_ERROR, requestCode, errorType, e));
                if (ExceptionUtil.isErrorHandleWithACRA(e)) {
                    if (ACRA.isInitialised()) {
                        ACRA.getErrorReporter().handleException(new Exception("JsBackgroundService.onError", e));
                    }
                }
            }
        } catch (RemoteException remoteException) {
            remoteException.printStackTrace();
        }
    }

    @Override
    public void onActionProgress(String ownerKey, int requestCode, int resultCode) {
        Log.d(TAG, "onActionProgress:" + "ownerKey =" + ownerKey + "| requestCode =" + requestCode);
        try {
            if (replyToMessengers.containsKey(ownerKey)) {
                Messenger messenger = replyToMessengers.get(ownerKey);
                messenger.send(Message.obtain(null, REPLY_ACTION_PROGRESS, requestCode, resultCode));
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
        }
    }

    /**
     * Send a reply to the called component.
     *
     * @param key         The key which identify the reply to {@link Messenger}
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     * @param resultCode  The result code of the some action. Can take the following values:
     *                    <ul>
     *                    <li>{@link JsBackgroundService#REPLY_RESULT_CODE_ACTION_OK}</li>
     *                    </ul>
     *                    and different errors.
     * @throws RemoteException
     */
    private void sendReply(String key, int requestCode, int resultCode) throws RemoteException {
        sendReply(key, requestCode, resultCode, null);
    }

    /**
     * Send a reply to the called component.
     *
     * @param key         The key which identify the reply to {@link Messenger}
     * @param requestCode The unique request code for the reply to {@link android.os.Messenger}.
     * @param resultCode  The result code of the some action. Can take the following values:
     *                    <ul>
     *                    <li>{@link JsBackgroundService#REPLY_RESULT_CODE_ACTION_OK}</li>
     *                    </ul>
     *                    and different errors.
     * @param obj         The object which will be send to the request {@link Messenger}.
     * @throws RemoteException
     */
    private void sendReply(String key, int requestCode, int resultCode, Object obj) throws RemoteException {
        if (replyToMessengers.containsKey(key)) {
            Messenger messenger = replyToMessengers.get(key);
            messenger.send(Message.obtain(null, REPLY_OK, requestCode, resultCode, obj));
        }
    }

    /**
     * The incoming handler realization. This handler will be used to communicate with current
     * service and other Android components.
     */
    private static class IncomingHandler extends Handler {
        private final WeakReference<JsInBackgroundManager> jsInBackgroundManagerWeakReference;
        private final WeakReference<Map<String, Messenger>> replyToMessengersWeakReference;

        IncomingHandler(JsInBackgroundManager jsInBackgroundManager, Map<String, Messenger>
                replyToMessengersWeakReference) {
            this.jsInBackgroundManagerWeakReference = new WeakReference<>(jsInBackgroundManager);
            this.replyToMessengersWeakReference = new WeakReference<>(replyToMessengersWeakReference);
        }

        @Override
        public void handleMessage(Message message) {
            if (jsInBackgroundManagerWeakReference.get() != null) {
                JsInBackgroundManager jsInBackgroundManager = jsInBackgroundManagerWeakReference.get();
                JsBackgroundService.Action action = null;

                if (message.obj instanceof JsBackgroundService.Action) {
                    action = (JsBackgroundService.Action) message.obj;
                }

                switch (message.what) {
                    case MESSAGE_ADD_REPLY_MESSENGER:
                        Map<String, Messenger> replyToMessengersForAdd = replyToMessengersWeakReference.get();

                        if (replyToMessengersForAdd != null && action != null) {
                            replyToMessengersForAdd.put(action.getOwnerKey(), message.replyTo);
                        }
                        break;

                    case MESSAGE_REMOVE_REPLY_MESSENGER:
                        Map<String, Messenger> replyToMessengersForRemove = replyToMessengersWeakReference.get();

                        if (replyToMessengersForRemove != null && action != null) {
                            replyToMessengersForRemove.remove(action.getOwnerKey());
                        }
                        break;

                    case MESSAGE_DECRYPT_MESSAGE:
                        if (jsInBackgroundManager != null && action != null) {
                            String rawMessage = (String) action.getObject();

                            jsInBackgroundManager.decryptMessage(action.getOwnerKey(), action.getRequestCode(),
                                    rawMessage);
                        }
                        break;

                    default:
                        super.handleMessage(message);
                }
            }
        }
    }

    /**
     * This class can be used to create a new action for {@link JsBackgroundService}
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
         * @param object      The object which will be passed to {@link JsBackgroundService}.
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