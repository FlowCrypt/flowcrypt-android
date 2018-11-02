/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.flowcrypt.email.service.CheckClipboardToFindKeyService;

/**
 * This activity describes a logic of checking the clipboard in the background and find the private
 * keys. We examine the clipboard every time the user is coming back to the app (if the
 * app was in the background), as long as email auth is already done but key is not yet set up.
 *
 * @author Denis Bondarenko
 * Date: 27.07.2017
 * Time: 11:13
 * E-mail: DenBond7@gmail.com
 */

public abstract class BaseCheckClipboardBackStackActivity extends BaseBackStackActivity implements
    ServiceConnection {
  protected boolean isServiceBound;
  protected CheckClipboardToFindKeyService checkClipboardToFindKeyService;

  public abstract boolean isPrivateKeyChecking();

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    bindService(new Intent(this, CheckClipboardToFindKeyService.class),
        this, Context.BIND_AUTO_CREATE);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (isServiceBound) {
      unbindService(this);
      isServiceBound = false;
    }
  }

  @Override
  public void onServiceConnected(ComponentName name, IBinder service) {
    CheckClipboardToFindKeyService.LocalBinder binder =
        (CheckClipboardToFindKeyService.LocalBinder) service;
    checkClipboardToFindKeyService = binder.getService();
    checkClipboardToFindKeyService.setMustBePrivateKey(isPrivateKeyChecking());
    isServiceBound = true;
  }

  @Override
  public void onServiceDisconnected(ComponentName name) {
    isServiceBound = false;
  }
}
