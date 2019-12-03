/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.database.dao.source.imap.MessageDaoSource
import com.flowcrypt.email.database.provider.FlowcryptContract
import com.flowcrypt.email.jobscheduler.MessagesSenderJobService
import com.flowcrypt.email.service.EmailSyncService
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity
import com.flowcrypt.email.util.GeneralUtil

/**
 * This [Activity] helps a user recover access to Gmail account for the app
 *
 * @author Denis Bondarenko
 *         Date: 11/26/19
 *         Time: 3:39 PM
 *         E-mail: DenBond7@gmail.com
 */
class UserRecoverableAuthExceptionActivity : AppCompatActivity(), View.OnClickListener {
  private val account: AccountDao? by lazy { AccountDaoSource().getActiveAccountInformation(this) }
  private val recoverableIntent: Intent? by lazy { intent.getParcelableExtra<Intent>(EXTRA_KEY_RECOVERABLE_INTENT) }
  private lateinit var textViewExplanation: TextView

  init {
    lastCallTime = System.currentTimeMillis()
    isRunEnabled = false
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_user_recoverable_auth_exception)
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
            AccountDaoSource().updateAccountInformation(this, account, ContentValues()
                .apply { put(AccountDaoSource.COL_IS_RESTORE_ACCESS_REQUIRED, false) })
            MessageDaoSource().changeMsgsState(this, account?.email, JavaEmailConstants
                .FOLDER_OUTBOX, MessageState.AUTH_FAILURE, MessageState.QUEUED)
            MessagesSenderJobService.schedule(applicationContext)
            EmailManagerActivity.runEmailManagerActivity(this)
            finish()
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
        .app_name), account?.email ?: "")
    findViewById<View>(R.id.buttonSignInWithGmail)?.setOnClickListener(this)
    findViewById<View>(R.id.buttonLogout)?.setOnClickListener(this)
    findViewById<View>(R.id.buttonPrivacy)?.setOnClickListener(this)
    findViewById<View>(R.id.buttonTerms)?.setOnClickListener(this)
    findViewById<View>(R.id.buttonSecurity)?.setOnClickListener(this)
    findViewById<View>(R.id.buttonHelp)?.setOnClickListener(this)
  }

  private fun logout() {
    val nonNullAccount = account ?: return

    val accountDaoSource = AccountDaoSource()
    val accountDaoList = accountDaoSource.getAccountsWithoutActive(this, nonNullAccount.email)

    val uri = Uri.parse(FlowcryptContract.AUTHORITY_URI.toString() + "/" + FlowcryptContract.CLEAN_DATABASE)
    contentResolver.delete(uri, null, arrayOf(nonNullAccount.email))

    if (accountDaoList.isNotEmpty()) {
      val (email) = accountDaoList[0]
      AccountDaoSource().setActiveAccount(this, email)
      finish()
      EmailSyncService.switchAccount(this)
      EmailManagerActivity.runEmailManagerActivity(this)
    } else {
      stopService(Intent(this, EmailSyncService::class.java))
      val intent = Intent(this, SignInActivity::class.java)
      intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
      startActivity(intent)
      finish()
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