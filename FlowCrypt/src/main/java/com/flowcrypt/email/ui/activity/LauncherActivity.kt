/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.preference.PreferenceManager
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.ActionQueueDaoSource
import com.flowcrypt.email.jobscheduler.ForwardedAttachmentsDownloaderJobService
import com.flowcrypt.email.jobscheduler.MessagesMovingJobService
import com.flowcrypt.email.jobscheduler.MessagesSenderJobService
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.service.EmailSyncService
import com.flowcrypt.email.service.FeedbackJobIntentService
import com.flowcrypt.email.service.actionqueue.actions.EncryptPrivateKeysIfNeededAction
import com.flowcrypt.email.ui.activity.base.BaseActivity
import com.flowcrypt.email.util.SharedPreferencesHelper

/**
 * This is a launcher [Activity]
 *
 * @author Denis Bondarenko
 * Date: 3/5/19
 * Time: 9:57 AM
 * E-mail: DenBond7@gmail.com
 */
class LauncherActivity : BaseActivity() {
  private var account: AccountDao? = null

  override val isDisplayHomeAsUpEnabled: Boolean
    get() = false

  override val rootView: View
    get() = findViewById(R.id.layoutContent)

  override val contentViewResourceId: Int
    get() = R.layout.activity_launcher

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    PreferenceManager.setDefaultValues(this, R.xml.preferences_notifications_settings, false)
    ForwardedAttachmentsDownloaderJobService.schedule(applicationContext)
    MessagesSenderJobService.schedule(applicationContext)
    MessagesMovingJobService.schedule(applicationContext)
    FeedbackJobIntentService.enqueueWork(this)

    account = AccountDaoSource().getActiveAccountInformation(this)
    if (account != null && isNodeReady) {
      showEmailManagerActivity()
    }
  }

  override fun onNodeStateChanged(isReady: Boolean) {
    super.onNodeStateChanged(isReady)
    if (account != null) {
      showEmailManagerActivity()
    } else {
      showSignInActivity()
    }
  }


  private fun showSignInActivity() {
    startActivity(Intent(this, SignInActivity::class.java))
    finish()
  }

  private fun showEmailManagerActivity() {
    if (SecurityUtils.hasBackup(this)) {
      val isCheckKeysNeeded = SharedPreferencesHelper.getBoolean(PreferenceManager
          .getDefaultSharedPreferences(this), Constants.PREF_KEY_IS_CHECK_KEYS_NEEDED, true)

      if (isCheckKeysNeeded) {
        ActionQueueDaoSource().addAction(this, EncryptPrivateKeysIfNeededAction(0, account!!.email, 0))
      }

      EmailSyncService.startEmailSyncService(this)
      EmailManagerActivity.runEmailManagerActivity(this)
      finish()
    }
  }
}
