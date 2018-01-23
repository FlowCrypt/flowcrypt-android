/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.service;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.flowcrypt.email.js.Js;
import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.util.exception.ManualHandledException;

import org.acra.ACRA;

import java.io.IOException;
import java.lang.ref.WeakReference;

/**
 * This service will be used to do checking clipboard to find a valid key while the
 * service running.
 *
 * @author Denis Bondarenko
 *         Date: 27.07.2017
 *         Time: 9:07
 *         E-mail: DenBond7@gmail.com
 */

public class CheckClipboardToFindKeyService extends Service implements ClipboardManager
        .OnPrimaryClipChangedListener {
    public static final String TAG = CheckClipboardToFindKeyService.class.getSimpleName();

    private volatile Looper serviceWorkerLooper;
    private volatile ServiceWorkerHandler serviceWorkerHandler;

    private KeyDetails keyDetails;
    private IBinder localBinder;
    private ClipboardManager clipboardManager;
    private Messenger replyMessenger;
    private boolean isMustBePrivateKey;

    public CheckClipboardToFindKeyService() {
        this.localBinder = new LocalBinder();
        this.replyMessenger = new Messenger(new ReplyHandler(this));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
            clipboardManager.addPrimaryClipChangedListener(this);
        }

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();

        serviceWorkerLooper = handlerThread.getLooper();
        serviceWorkerHandler = new ServiceWorkerHandler(serviceWorkerLooper);

        checkClipboard();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind:" + intent);
        return localBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");

        serviceWorkerLooper.quit();

        if (clipboardManager != null) {
            clipboardManager.removePrimaryClipChangedListener(this);
        }
    }

    @Override
    public void onPrimaryClipChanged() {
        checkClipboard();
    }

    public KeyDetails getKeyDetails() {
        return keyDetails;
    }

    public boolean isMustBePrivateKey() {
        return isMustBePrivateKey;
    }

    public void setMustBePrivateKey(boolean mustBePrivateKey) {
        isMustBePrivateKey = mustBePrivateKey;
    }

    private void checkClipboard() {
        keyDetails = null;
        if (clipboardManager.hasPrimaryClip()) {
            ClipData.Item item = clipboardManager.getPrimaryClip().getItemAt(0);
            CharSequence privateKeyFromClipboard = item.getText();
            if (!TextUtils.isEmpty(privateKeyFromClipboard)) {
                checkClipboardText(privateKeyFromClipboard.toString());
            }
        }
    }

    private void checkClipboardText(String clipboardText) {
        Message message = serviceWorkerHandler.obtainMessage();
        message.what = ServiceWorkerHandler.MESSAGE_WHAT;
        message.obj = clipboardText;
        message.replyTo = replyMessenger;
        serviceWorkerHandler.removeMessages(ServiceWorkerHandler.MESSAGE_WHAT);
        serviceWorkerHandler.sendMessage(message);
    }

    /**
     * The incoming handler realization. This handler will be used to communicate with current
     * service and the worker thread.
     */
    private static class ReplyHandler extends Handler {
        static final int MESSAGE_WHAT = 1;
        private final WeakReference<CheckClipboardToFindKeyService>
                checkClipboardToFindPrivateKeyServiceWeakReference;

        ReplyHandler(CheckClipboardToFindKeyService checkClipboardToFindKeyService) {
            this.checkClipboardToFindPrivateKeyServiceWeakReference = new WeakReference<>
                    (checkClipboardToFindKeyService);
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case MESSAGE_WHAT:
                    if (checkClipboardToFindPrivateKeyServiceWeakReference.get() != null) {
                        CheckClipboardToFindKeyService
                                checkClipboardToFindKeyService =
                                checkClipboardToFindPrivateKeyServiceWeakReference.get();

                        KeyDetails keyDetails = (KeyDetails) message.obj;

                        checkClipboardToFindKeyService.keyDetails
                                = new KeyDetails(null, keyDetails.getValue(), null,
                                KeyDetails.Type.CLIPBOARD, checkClipboardToFindKeyService
                                .isMustBePrivateKey(), keyDetails.getPgpContact());
                        Log.d(TAG, "Found a valid private key in clipboard");
                    }
                    break;
            }
        }
    }

    /**
     * The local binder realization.
     */
    public class LocalBinder extends Binder {
        public CheckClipboardToFindKeyService getService() {
            return CheckClipboardToFindKeyService.this;
        }
    }

    /**
     * This handler will be used by the instance of {@link HandlerThread} to receive message from
     * the UI thread.
     */
    private final class ServiceWorkerHandler extends Handler {
        static final int MESSAGE_WHAT = 1;
        private Js js;

        ServiceWorkerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_WHAT:
                    if (js == null) {
                        try {
                            js = new Js(getApplicationContext(), null);
                        } catch (IOException e) {
                            e.printStackTrace();
                            if (ACRA.isInitialised()) {
                                ACRA.getErrorReporter().handleException(new ManualHandledException(e));
                            }
                        }
                    }

                    if (js != null) {
                        String clipboardText = (String) msg.obj;

                        try {
                            String normalizedArmoredKey = js.crypto_key_normalize(clipboardText);
                            PgpKey pgpKey = js.crypto_key_read(normalizedArmoredKey);

                            if (js.is_valid_key(pgpKey, isMustBePrivateKey)) {
                                try {
                                    KeyDetails keyDetails = new KeyDetails(null, clipboardText, null,
                                            null,
                                            false, pgpKey.getPrimaryUserId());
                                    Messenger messenger = msg.replyTo;
                                    messenger.send(Message.obtain(null, ReplyHandler.MESSAGE_WHAT,
                                            keyDetails));
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                    if (ACRA.isInitialised()) {
                                        ACRA.getErrorReporter().handleException(new ManualHandledException(e));
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (ACRA.isInitialised()) {
                                ACRA.getErrorReporter().handleException(new ManualHandledException(e));
                            }
                        }
                    }
                    break;
            }
        }
    }
}
