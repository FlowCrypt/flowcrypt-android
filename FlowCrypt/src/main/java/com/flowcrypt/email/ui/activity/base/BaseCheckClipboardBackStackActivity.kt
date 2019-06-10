/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder

import com.flowcrypt.email.service.CheckClipboardToFindKeyService

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

abstract class BaseCheckClipboardBackStackActivity : BaseBackStackActivity(), ServiceConnection {
  @JvmField
  protected var isBound: Boolean = false
  protected lateinit var service: CheckClipboardToFindKeyService

  abstract val isPrivateKeyChecking: Boolean

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    bindService(Intent(this, CheckClipboardToFindKeyService::class.java), this, Context.BIND_AUTO_CREATE)
  }

  override fun onDestroy() {
    super.onDestroy()
    if (isBound) {
      unbindService(this)
      isBound = false
    }
  }

  override fun onServiceConnected(name: ComponentName, service: IBinder) {
    val binder = service as CheckClipboardToFindKeyService.LocalBinder
    this.service = binder.service
    this.service.isPrivateKeyMode = isPrivateKeyChecking
    isBound = true
  }

  override fun onServiceDisconnected(name: ComponentName) {
    isBound = false
  }
}
