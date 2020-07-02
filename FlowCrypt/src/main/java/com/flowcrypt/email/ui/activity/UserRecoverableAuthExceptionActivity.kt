/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.jobscheduler.MessagesSenderWorker
import com.flowcrypt.email.service.EmailSyncService
import com.flowcrypt.email.ui.activity.base.BaseActivity
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity
import com.flowcrypt.email.util.GeneralUtil
import kotlinx.coroutines.launch

/**
 * This [Activity] helps a user recover access to Gmail account for the app
 *
 * @author Denis Bondarenko
 *         Date: 11/26/19
 *         Time: 3:39 PM
 *         E-mail: DenBond7@gmail.com
 */
class UserRecoverableAuthExceptionActivity : BaseActivity(), View.OnClickListener {
  private val recoverableIntent: Intent? by lazy { intent.getParcelableExtra<Intent>(EXTRA_KEY_RECOVERABLE_INTENT) }
  private lateinit var textViewExplanation: TextView

  init {
    lastCallTime = System.currentTimeMillis()
    isRunEnabled = false
  }

  override val isDisplayHomeAsUpEnabled: Boolean
    get() = false

  override val contentViewResourceId: Int
    get() = R.layout.activity_user_recoverable_auth_exception

  override val rootView: View
    get() = findViewById(R.id.layoutContentView)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initViews()
  }

  override fun onDestroy() {
    super.onDestroy()
    lastCallTime = 0
    isRunEnabled = true
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_RUN_RECOVERABLE_INTENT -> {
        when (resultCode) {
          Activity.RESULT_OK -> {
            lifecycleScope.launch {
              val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)
              activeAccount?.copy(isRestoreAccessRequired = false)?.let {
                roomDatabase.accountDao().updateAccountSuspend(it)
              }
              roomDatabase.msgDao().changeMsgsStateSuspend(
                  activeAccount?.email, JavaEmailConstants.FOLDER_OUTBOX, MessageState.AUTH_FAILURE.value,
                  MessageState.QUEUED.value)
              MessagesSenderWorker.enqueue(applicationContext)
              EmailManagerActivity.runEmailManagerActivity(this@UserRecoverableAuthExceptionActivity)
              finish()
            }
          }

          Activity.RESULT_CANCELED -> {
            Toast.makeText(this, getString(R.string.access_was_not_granted), Toast.LENGTH_SHORT).show()
          }
        }
      }
      else -> super.onActivityResult(requestCode, resultCode, data)
    }

  }

  override fun onClick(v: View?) {
    when (v?.id) {
      R.id.buttonSignInWithGmail -> {
        recoverableIntent?.let { startActivityForResult(it, REQUEST_CODE_RUN_RECOVERABLE_INTENT) }
      }

      R.id.buttonLogout -> {
        logout()
      }

      R.id.buttonPrivacy -> GeneralUtil.openCustomTab(this, Constants.FLOWCRYPT_PRIVACY_URL)

      R.id.buttonTerms -> GeneralUtil.openCustomTab(this, Constants.FLOWCRYPT_TERMS_URL)

      R.id.buttonSecurity ->
        startActivity(HtmlViewFromAssetsRawActivity.newIntent(this, getString(R.string.security), "html/security.htm"))

      R.id.buttonHelp -> FeedbackActivity.show(this)
    }
  }

  private fun initViews() {
    textViewExplanation = findViewById(R.id.textViewExplanation)
    textViewExplanation.text = getString(R.string.reconnect_your_account, getString(R.string
        .app_name), activeAccount?.email ?: "")
    findViewById<View>(R.id.buttonSignInWithGmail)?.setOnClickListener(this)
    findViewById<View>(R.id.buttonLogout)?.setOnClickListener(this)
    findViewById<View>(R.id.buttonPrivacy)?.setOnClickListener(this)
    findViewById<View>(R.id.buttonTerms)?.setOnClickListener(this)
    findViewById<View>(R.id.buttonSecurity)?.setOnClickListener(this)
    findViewById<View>(R.id.buttonHelp)?.setOnClickListener(this)
  }

  private fun logout() {
    val activeAccount = activeAccount ?: return
    lifecycleScope.launch {
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(applicationContext)

      //remove all info about the given account from the local db
      roomDatabase.accountDao().deleteSuspend(activeAccount)
      //todo-denbond7 Improve this via onDelete = ForeignKey.CASCADE
      roomDatabase.labelDao().deleteByEmailSuspend(activeAccount.email)
      roomDatabase.msgDao().deleteByEmailSuspend(activeAccount.email)
      roomDatabase.attachmentDao().deleteByEmailSuspend(activeAccount.email)
      roomDatabase.accountAliasesDao().deleteByEmailSuspend(activeAccount.email)

      val nonactiveAccounts = roomDatabase.accountDao().getAllNonactiveAccountsSuspend()
      if (nonactiveAccounts.isNotEmpty()) {
        val firstNonactiveAccount = nonactiveAccounts.first()
        roomDatabase.accountDao().updateAccountsSuspend(roomDatabase.accountDao().getAccountsSuspend().map { it.copy(isActive = false) })
        roomDatabase.accountDao().updateAccountSuspend(firstNonactiveAccount.copy(isActive = true))
        EmailSyncService.switchAccount(applicationContext)
        EmailManagerActivity.runEmailManagerActivity(this@UserRecoverableAuthExceptionActivity)
        finish()
      } else {
        stopService(Intent(applicationContext, EmailSyncService::class.java))
        val intent = Intent(applicationContext, SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
      }
    }
  }

  companion object {
    private var lastCallTime = 0L
    private var isRunEnabled = true

    private val EXTRA_KEY_RECOVERABLE_INTENT = GeneralUtil.generateUniqueExtraKey(
        "EXTRA_KEY_RECOVERABLE_INTENT", UserRecoverableAuthExceptionActivity::class.java)
    private const val REQUEST_CODE_RUN_RECOVERABLE_INTENT = 101

    fun isRunEnabled(): Boolean {
      return isRunEnabled || System.currentTimeMillis() - lastCallTime > 5000
    }

    fun newIntent(context: Context, incomingIntent: Intent): Intent {
      val intent = Intent(context, UserRecoverableAuthExceptionActivity::class.java)
      intent.putExtra(EXTRA_KEY_RECOVERABLE_INTENT, incomingIntent)
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
      return intent
    }
  }
}