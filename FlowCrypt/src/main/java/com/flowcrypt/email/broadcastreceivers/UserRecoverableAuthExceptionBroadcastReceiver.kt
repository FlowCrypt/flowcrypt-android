/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flowcrypt.email.ui.activity.UserRecoverableAuthExceptionActivity
import com.flowcrypt.email.util.GeneralUtil

/**
 * This [BroadcastReceiver] can be used to run [UserRecoverableAuthExceptionActivity] if a user has
 * lost access to Gmail account from the app.
 *
 * @author Denis Bondarenko
 *         Date: 11/26/19
 *         Time: 3:42 PM
 *         E-mail: DenBond7@gmail.com
 */
class UserRecoverableAuthExceptionBroadcastReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    if (GeneralUtil.isAppForegrounded()) {
      val incomingIntent = intent.getParcelableExtra<Intent>(EXTRA_KEY_RECOVERABLE_INTENT)
      incomingIntent?.let {
        if (UserRecoverableAuthExceptionActivity.isRunEnabled()) {
          context.startActivity(UserRecoverableAuthExceptionActivity.newIntent(context, it))
        }
      }
    }
  }

  companion object {
    private val EXTRA_KEY_RECOVERABLE_INTENT = GeneralUtil.generateUniqueExtraKey(
        "EXTRA_KEY_RECOVERABLE_INTENT", UserRecoverableAuthExceptionBroadcastReceiver::class.java)

    fun newIntent(context: Context, incomingIntent: Intent): Intent {
      val intent = Intent(context, UserRecoverableAuthExceptionBroadcastReceiver::class.java)
      intent.putExtra(EXTRA_KEY_RECOVERABLE_INTENT, incomingIntent)
      return intent
    }
  }
}