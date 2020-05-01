/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.viewmodel.CheckGmailTokenViewModel
import com.flowcrypt.email.jobscheduler.ForwardedAttachmentsDownloaderJobService
import com.flowcrypt.email.jobscheduler.MessagesSenderJobService
import com.flowcrypt.email.security.KeysStorageImpl
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
  private lateinit var checkGmailTokenViewModel: CheckGmailTokenViewModel

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
    FeedbackJobIntentService.enqueueWork(this)

    setupCheckGmailTokenViewModel()
  }

  override fun onNodeStateChanged(isReady: Boolean) {
    super.onNodeStateChanged(isReady)
    if (isAccountInfoReceived) {
      if (activeAccount != null) {
        if (AccountEntity.ACCOUNT_TYPE_GOOGLE == activeAccount?.accountType && activeAccount?.isRestoreAccessRequired == true) {
          activeAccount?.let { checkGmailTokenViewModel.checkToken(it) }
        } else {
          showEmailManagerActivity()
        }
      } else {
        showSignInActivity()
      }
    }
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    if (activeAccount != null && isNodeReady) {
      if (AccountEntity.ACCOUNT_TYPE_GOOGLE == activeAccount?.accountType && activeAccount?.isRestoreAccessRequired == true) {
        activeAccount?.let { checkGmailTokenViewModel.checkToken(it) }
      } else {
        showEmailManagerActivity()
      }
    }
  }

  private fun setupCheckGmailTokenViewModel() {
    checkGmailTokenViewModel = ViewModelProvider(this).get(CheckGmailTokenViewModel::class.java)
    checkGmailTokenViewModel.tokenLiveData.observe(this, Observer {
      if (it != null) {
        if (UserRecoverableAuthExceptionActivity.isRunEnabled()) {
          startActivity(UserRecoverableAuthExceptionActivity.newIntent(this, it))
        }
      } else {
        showEmailManagerActivity()
      }
    })
  }

  private fun showSignInActivity() {
    startActivity(Intent(this, SignInActivity::class.java))
    finish()
  }

  private fun showEmailManagerActivity() {
    if (KeysStorageImpl.getInstance(application).hasKeys()) {
      val isCheckKeysNeeded = SharedPreferencesHelper.getBoolean(PreferenceManager
          .getDefaultSharedPreferences(this), Constants.PREF_KEY_IS_CHECK_KEYS_NEEDED, true)

      if (isCheckKeysNeeded) {
        roomBasicViewModel.addActionToQueue(EncryptPrivateKeysIfNeededAction(0, activeAccount!!.email, 0))
      }

      EmailSyncService.startEmailSyncService(this)
      EmailManagerActivity.runEmailManagerActivity(this)
      finish()
    }
  }
}
