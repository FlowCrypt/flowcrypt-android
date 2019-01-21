/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import android.text.TextUtils;
import android.util.Log;

import com.flowcrypt.email.js.PgpKey;
import com.flowcrypt.email.js.core.Js;
import com.flowcrypt.email.model.KeyDetails;
import com.flowcrypt.email.model.KeyImportModel;
import com.flowcrypt.email.util.exception.ExceptionUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;

import androidx.annotation.Nullable;

/**
 * This service will be used to do checking clipboard to find a valid key while the
 * service running.
 *
 * @author Denis Bondarenko
 * Date: 27.07.2017
 * Time: 9:07
 * E-mail: DenBond7@gmail.com
 */

public class CheckClipboardToFindKeyService extends Service implements ClipboardManager.OnPrimaryClipChangedListener {
  public static final String TAG = CheckClipboardToFindKeyService.class.getSimpleName();

  private volatile Looper serviceWorkerLooper;
  private volatile ServiceWorkerHandler serviceWorkerHandler;

  private KeyImportModel keyImportModel;
  private IBinder localBinder;
  private ClipboardManager clipboardManager;
  private Messenger replyMessenger;
  private boolean isPrivateKeyMode;

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
    serviceWorkerHandler = new ServiceWorkerHandler(serviceWorkerLooper, this);

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

  public KeyImportModel getKeyImportModel() {
    return keyImportModel;
  }

  public boolean isPrivateKeyMode() {
    return isPrivateKeyMode;
  }

  public void setPrivateKeyMode(boolean isPrivateKeyMode) {
    this.isPrivateKeyMode = isPrivateKeyMode;
  }

  private void checkClipboard() {
    keyImportModel = null;
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
    private final WeakReference<CheckClipboardToFindKeyService> weakRef;

    ReplyHandler(CheckClipboardToFindKeyService checkClipboardToFindKeyService) {
      this.weakRef = new WeakReference<>(checkClipboardToFindKeyService);
    }

    @Override
    public void handleMessage(Message message) {
      switch (message.what) {
        case MESSAGE_WHAT:
          if (weakRef.get() != null) {
            CheckClipboardToFindKeyService checkClipboardToFindKeyService = weakRef.get();

            String key = (String) message.obj;

            checkClipboardToFindKeyService.keyImportModel = new KeyImportModel(null, key,
                weakRef.get().isPrivateKeyMode, KeyDetails.Type.CLIPBOARD);
            Log.d(TAG, "Found a valid private key in clipboard");
          }
          break;
      }
    }
  }

  /**
   * This handler will be used by the instance of {@link HandlerThread} to receive message from
   * the UI thread.
   */
  private static final class ServiceWorkerHandler extends Handler {
    static final int MESSAGE_WHAT = 1;
    private final WeakReference<CheckClipboardToFindKeyService> weakRef;
    private Js js;

    ServiceWorkerHandler(Looper looper, CheckClipboardToFindKeyService checkClipboardToFindKeyService) {
      super(looper);
      this.weakRef = new WeakReference<>(checkClipboardToFindKeyService);
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MESSAGE_WHAT:
          if (js == null) {
            try {
              if (weakRef.get() != null) {
                js = new Js(weakRef.get(), null);
              }
            } catch (IOException e) {
              e.printStackTrace();
              ExceptionUtil.handleError(e);
            }
          }

          if (js != null) {
            sendReply(msg);
          }
          break;
      }
    }

    private void sendReply(Message msg) {
      String clipboardText = (String) msg.obj;

      try {
        String normalizedArmoredKey = js.crypto_key_normalize(clipboardText);
        PgpKey pgpKey = js.crypto_key_read(normalizedArmoredKey);

        if (weakRef.get() != null && js.is_valid_key(pgpKey, weakRef.get().isPrivateKeyMode)) {
          try {
            Messenger messenger = msg.replyTo;
            messenger.send(Message.obtain(null, ReplyHandler.MESSAGE_WHAT, clipboardText));
          } catch (RemoteException e) {
            e.printStackTrace();
            ExceptionUtil.handleError(e);
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
        ExceptionUtil.handleError(e);
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
}
