/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.accounts

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * @author Denis Bondarenko
 *         Date: 8/12/20
 *         Time: 5:03 PM
 *         E-mail: DenBond7@gmail.com
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
