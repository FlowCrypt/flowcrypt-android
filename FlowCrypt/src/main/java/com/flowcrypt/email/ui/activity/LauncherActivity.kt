/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.preference.PreferenceManager
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.viewmodel.CheckGmailTokenViewModel
import com.flowcrypt.email.jetpack.viewmodel.LauncherViewModel
import com.flowcrypt.email.jetpack.workmanager.ForwardedAttachmentsDownloaderWorker
import com.flowcrypt.email.jetpack.workmanager.MessagesSenderWorker
import com.flowcrypt.email.node.Node
import com.flowcrypt.email.service.FeedbackJobIntentService
import com.flowcrypt.email.service.IdleService
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
  private val checkGmailTokenViewModel: CheckGmailTokenViewModel by viewModels()
  private val launcherViewModel: LauncherViewModel by viewModels()

  override val isDisplayHomeAsUpEnabled: Boolean
    get() = false

  override val rootView: View
    get() = findViewById(R.id.layoutContent)

  override val contentViewResourceId: Int
    get() = R.layout.activity_launcher

  override fun onCreate(savedInstanceState: Bundle?) {
    setupLauncherViewModel()
    super.onCreate(savedInstanceState)
    PreferenceManager.setDefaultValues(this, R.xml.preferences_notifications_settings, false)
    ForwardedAttachmentsDownloaderWorker.enqueue(applicationContext)
    MessagesSenderWorker.enqueue(applicationContext)
    FeedbackJobIntentService.enqueueWork(this)

    setupCheckGmailTokenViewModel()
  }

  override fun onNodeStateChanged(nodeInitResult: Node.NodeInitResult) {
    super.onNodeStateChanged(nodeInitResult)
    launcherViewModel.isNodeInfoReceivedLiveData.value = true
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    launcherViewModel.isAccountInfoReceivedLiveData.value = true
  }

  private fun setupLauncherViewModel() {
    launcherViewModel.mediatorLiveData.observe(this, {
      if (launcherViewModel.isAccountInfoReceivedLiveData.value == true
          && launcherViewModel.isNodeInfoReceivedLiveData.value == true) {
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
    })
  }

  private fun setupCheckGmailTokenViewModel() {
    checkGmailTokenViewModel.tokenLiveData.observe(this, {
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
    val isCheckKeysNeeded = SharedPreferencesHelper.getBoolean(PreferenceManager
        .getDefaultSharedPreferences(this), Constants.PREF_KEY_IS_CHECK_KEYS_NEEDED, true)

    if (isCheckKeysNeeded) {
      roomBasicViewModel.addActionToQueue(EncryptPrivateKeysIfNeededAction(0, activeAccount!!.email, 0))
    }

    IdleService.start(this)
    EmailManagerActivity.runEmailManagerActivity(this)
    finish()
  }
}
