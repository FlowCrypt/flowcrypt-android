/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.flowcrypt.email.R;
import com.flowcrypt.email.model.results.LoaderResult;
import com.flowcrypt.email.service.BaseService;
import com.flowcrypt.email.service.JsBackgroundService;
import com.flowcrypt.email.util.GeneralUtil;
import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.loader.content.Loader;

/**
 * This is a base activity. This class describes a base logic for all activities.
 *
 * @author DenBond7
 * Date: 30.04.2017.
 * Time: 22:21.
 * E-mail: DenBond7@gmail.com
 */
public abstract class BaseActivity extends AppCompatActivity implements BaseService.OnServiceCallback {
  protected final String tag;

  protected Messenger jsServiceMessenger;
  protected Messenger jsServiceReplyMessenger;
  /**
   * Flag indicating whether we have called bind on the {@link JsBackgroundService}.
   */
  protected boolean isBoundToJsService;
  private Snackbar snackbar;
  private Toolbar toolbar;
  private AppBarLayout appBarLayout;
  private ServiceConnection serviceConnectionJsService = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      Log.d(tag, "Activity connected to " + name.getClassName());
      jsServiceMessenger = new Messenger(service);
      isBoundToJsService = true;

      registerReplyMessenger(JsBackgroundService.MESSAGE_ADD_REPLY_MESSENGER, jsServiceMessenger,
          jsServiceReplyMessenger);
      onJsServiceConnected();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      Log.d(tag, "Activity disconnected from " + name.getClassName());
      jsServiceMessenger = null;
      isBoundToJsService = false;
    }
  };

  public BaseActivity() {
    tag = getClass().getSimpleName();
    jsServiceReplyMessenger = new Messenger(new ReplyHandler(this));
  }

  /**
   * This method can used to change "HomeAsUpEnabled" behavior.
   *
   * @return true if we want to show "HomeAsUpEnabled", false otherwise.
   */
  public abstract boolean isDisplayHomeAsUpEnabled();

  /**
   * Get the content view resources id. This method must return an resources id of a layout
   * if we want to show some UI.
   *
   * @return The content view resources id.
   */
  public abstract int getContentViewResourceId();

  /**
   * Get root view which will be used for show Snackbar.
   */
  public abstract View getRootView();

  public abstract void onJsServiceConnected();

  @Override
  public void onReplyFromServiceReceived(int requestCode, int resultCode, Object obj) {

  }

  @Override
  public void onProgressReplyFromServiceReceived(int requestCode, int resultCode, Object obj) {

  }

  @Override
  public void onErrorFromServiceReceived(int requestCode, int errorType, Exception e) {

  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(tag, "onCreate");
    setContentView(getContentViewResourceId());
    initScreenViews();

    bindToService(JsBackgroundService.class, serviceConnectionJsService);
  }

  @Override
  public void onStart() {
    super.onStart();
    Log.d(tag, "onStart");
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(tag, "onResume");
  }

  @Override
  public void onStop() {
    super.onStop();
    Log.d(tag, "onStop");
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    Log.d(tag, "onDestroy");

    if (isBoundToJsService) {
      if (jsServiceMessenger != null) {
        unregisterReplyMessenger(JsBackgroundService.MESSAGE_REMOVE_REPLY_MESSENGER, jsServiceMessenger,
            jsServiceReplyMessenger);
      }

      unbindFromService(JsBackgroundService.class, serviceConnectionJsService);
      isBoundToJsService = false;
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        if (isDisplayHomeAsUpEnabled()) {
          finish();
          return true;
        } else return super.onOptionsItemSelected(item);

      default:
        return super.onOptionsItemSelected(item);
    }
  }

  public Toolbar getToolbar() {
    return toolbar;
  }

  public AppBarLayout getAppBarLayout() {
    return appBarLayout;
  }

  /**
   * Show information as Snackbar.
   *
   * @param view        The view to find a parent from.
   * @param messageText The text to show.  Can be formatted text.
   */
  public void showInfoSnackbar(View view, String messageText) {
    showInfoSnackbar(view, messageText, Snackbar.LENGTH_INDEFINITE);
  }

  /**
   * Show information as Snackbar.
   *
   * @param view        The view to find a parent from.
   * @param messageText The text to show.  Can be formatted text.
   * @param duration    How long to display the message.
   */
  public void showInfoSnackbar(View view, String messageText, int duration) {
    snackbar = Snackbar.make(view, messageText, duration)
        .setAction(android.R.string.ok, new View.OnClickListener() {
          @Override
          public void onClick(View v) {
          }
        });
    snackbar.show();
  }

  /**
   * Show some information as Snackbar with custom message, action button mame and listener.
   *
   * @param view            he view to find a parent from
   * @param messageText     The text to show.  Can be formatted text
   * @param buttonName      The text of the Snackbar button
   * @param onClickListener The Snackbar button click listener.
   */
  public void showSnackbar(View view, String messageText, String buttonName,
                           @NonNull View.OnClickListener onClickListener) {
    showSnackbar(view, messageText, buttonName, Snackbar.LENGTH_INDEFINITE, onClickListener);
  }

  /**
   * Show some information as Snackbar with custom message, action button mame and listener.
   *
   * @param view            he view to find a parent from
   * @param messageText     The text to show.  Can be formatted text
   * @param buttonName      The text of the Snackbar button
   * @param duration        How long to display the message.
   * @param onClickListener The Snackbar button click listener.
   */
  public void showSnackbar(View view, String messageText, String buttonName, int duration,
                           @NonNull View.OnClickListener onClickListener) {
    snackbar = Snackbar.make(view, messageText, duration)
        .setAction(buttonName, onClickListener);
    snackbar.show();
  }

  public Snackbar getSnackBar() {
    return snackbar;
  }

  public void dismissSnackBar() {
    if (snackbar != null) {
      snackbar.dismiss();
    }
  }

  public void handleLoaderResult(Loader loader, LoaderResult loaderResult) {
    if (loaderResult != null) {
      if (loaderResult.getResult() != null) {
        handleSuccessLoaderResult(loader.getId(), loaderResult.getResult());
      } else if (loaderResult.getException() != null) {
        handleFailureLoaderResult(loader.getId(), loaderResult.getException());
      } else {
        showInfoSnackbar(getRootView(), getString(R.string.unknown_error));
      }
    } else {
      showInfoSnackbar(getRootView(), getString(R.string.unknown_error));
    }
  }

  public void handleFailureLoaderResult(int loaderId, Exception e) {

  }

  public void handleSuccessLoaderResult(int loaderId, Object result) {

  }

  public String getReplyMessengerName() {
    return getClass().getSimpleName() + "_" + hashCode();
  }

  /**
   * Start a job to decrypt a raw MIME message.
   *
   * @param requestCode    The unique request code for identify the current action.
   * @param rawMimeMessage The raw MIME message.
   */
  public void decryptMessage(int requestCode, String rawMimeMessage) {
    if (checkServiceBound(isBoundToJsService)) return;

    BaseService.Action action = new BaseService.Action(getReplyMessengerName(), requestCode, rawMimeMessage);

    Message message = Message.obtain(null, JsBackgroundService.MESSAGE_DECRYPT_MESSAGE, 0, 0, action);

    message.replyTo = jsServiceReplyMessenger;
    try {
      jsServiceMessenger.send(message);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  /**
   * Start a job to decrypt a raw MIME message.
   */
  public void restartJsService() {
    if (checkServiceBound(isBoundToJsService)) return;

    BaseService.Action action = new BaseService.Action(getReplyMessengerName(),
        R.id.js_refresh_storage_connector, null);
    Message message = Message.obtain(null, JsBackgroundService.MESSAGE_REFRESH_STORAGE_CONNECTOR, 0, 0, action);
    message.replyTo = jsServiceReplyMessenger;
    try {
      jsServiceMessenger.send(message);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  /**
   * Check is current {@link Activity} connected to some service.
   *
   * @return true if current activity connected to the service, otherwise false.
   */
  protected boolean checkServiceBound(boolean isBound) {
    if (!isBound) {
      if (GeneralUtil.isDebugBuild()) {
        Log.d(tag, "Activity not connected to the service");
      }
      return true;
    }
    return false;
  }

  protected void bindToService(Class<?> cls, ServiceConnection serviceConnection) {
    bindService(new Intent(this, cls), serviceConnection, Context.BIND_AUTO_CREATE);
    Log.d(tag, "bind to " + cls.getSimpleName());
  }

  /**
   * Disconnect from a service
   */
  protected void unbindFromService(Class<?> cls, ServiceConnection serviceConnection) {
    unbindService(serviceConnection);
    Log.d(tag, "unbind from " + cls.getSimpleName());
  }

  /**
   * Register a reply {@link Messenger} to receive notifications from some service.
   *
   * @param what             A {@link Message#what}}
   * @param serviceMessenger A service {@link Messenger}
   * @param replyToMessenger A reply to {@link Messenger}
   */
  protected void registerReplyMessenger(int what, Messenger serviceMessenger, Messenger replyToMessenger) {
    BaseService.Action action = new BaseService.Action(getReplyMessengerName(), -1, null);

    Message message = Message.obtain(null, what, action);
    message.replyTo = replyToMessenger;
    try {
      serviceMessenger.send(message);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  /**
   * Unregister a reply {@link Messenger} from some service.
   *
   * @param what             A {@link Message#what}}
   * @param serviceMessenger A service {@link Messenger}
   * @param replyToMessenger A reply to {@link Messenger}
   */
  protected void unregisterReplyMessenger(int what, Messenger serviceMessenger, Messenger replyToMessenger) {
    BaseService.Action action = new BaseService.Action(getReplyMessengerName(), -1, null);

    Message message = Message.obtain(null, what, action);
    message.replyTo = replyToMessenger;
    try {
      serviceMessenger.send(message);
    } catch (RemoteException e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
  }

  private void initScreenViews() {
    appBarLayout = findViewById(R.id.appBarLayout);
    setupToolbarIfItExists();
  }

  private void setupToolbarIfItExists() {
    toolbar = findViewById(R.id.toolbar);
    if (toolbar != null) {
      setSupportActionBar(toolbar);
    }

    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(isDisplayHomeAsUpEnabled());
    }
  }

  /**
   * The incoming handler realization. This handler will be used to communicate with a service and other Android
   * components.
   */
  protected static class ReplyHandler extends Handler {
    private final WeakReference<BaseService.OnServiceCallback> onServiceCallbackWeakReference;

    ReplyHandler(BaseService.OnServiceCallback onServiceCallback) {
      this.onServiceCallbackWeakReference = new WeakReference<>(onServiceCallback);
    }

    @Override
    public void handleMessage(Message message) {
      if (onServiceCallbackWeakReference.get() != null) {
        BaseService.OnServiceCallback onServiceCallback = onServiceCallbackWeakReference.get();
        switch (message.what) {
          case BaseService.REPLY_OK:
            onServiceCallback.onReplyFromServiceReceived(message.arg1, message.arg2, message.obj);
            break;

          case BaseService.REPLY_ERROR:
            Exception exception = null;

            if (message.obj instanceof Exception) {
              exception = (Exception) message.obj;
            }

            onServiceCallback.onErrorFromServiceReceived(message.arg1, message.arg2, exception);
            break;

          case BaseService.REPLY_ACTION_PROGRESS:
            onServiceCallback.onProgressReplyFromServiceReceived(message.arg1, message.arg2, message.obj);
            break;
        }
      }
    }
  }
}
