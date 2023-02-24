/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.accounts

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * @author Denys Bondarenko
 */
class FlowcryptAuthenticatorService : Service() {
  private lateinit var authenticator: FlowcryptAccountAuthenticator

  override fun onCreate() {
    super.onCreate()
    authenticator = FlowcryptAccountAuthenticator(applicationContext)
  }

  override fun onBind(intent: Intent?): IBinder? {
    return authenticator.iBinder
  }
}
