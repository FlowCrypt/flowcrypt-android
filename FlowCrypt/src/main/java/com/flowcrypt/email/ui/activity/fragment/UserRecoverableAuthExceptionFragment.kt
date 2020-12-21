/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.app.Activity
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
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.workmanager.MessagesSenderWorker
import com.flowcrypt.email.ui.activity.EmailManagerActivity
import com.flowcrypt.email.ui.activity.HtmlViewFromAssetsRawActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.notifications.ErrorNotificationManager
import com.flowcrypt.email.util.GeneralUtil
import kotlinx.coroutines.launch

/**
 * @author Denis Bondarenko
 *         Date: 12/21/20
 *         Time: 12:18 PM
 *         E-mail: DenBond7@gmail.com
 */
class UserRecoverableAuthExceptionFragment : BaseFragment(), ProgressBehaviour {
  override val progressView: View?
    get() = view?.findViewById(R.id.progress)
  override val contentView: View?
    get() = view?.findViewById(R.id.layoutContent)
  override val statusView: View?
    get() = view?.findViewById(R.id.status)

  private lateinit var textViewExplanation: TextView

  override val contentResourceId: Int = R.layout.fragment_user_recoverable_auth_exception

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ErrorNotificationManager.isShowingAuthErrorEnabled = false
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    ErrorNotificationManager(requireContext()).cancel(R.id.notification_id_auth_failure)

    supportActionBar?.title = null
    initViews(view)
  }

  override fun onDestroy() {
    super.onDestroy()
    ErrorNotificationManager.isShowingAuthErrorEnabled = true
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_RUN_GMAIL_RECOVERABLE_INTENT -> {
        when (resultCode) {
          Activity.RESULT_OK -> {
            lifecycleScope.launch {
              accountViewModel.activeAccountLiveData.value?.let { accountEntity ->
                context?.let { context ->
                  val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
                  roomDatabase.msgDao().changeMsgsStateSuspend(
                      accountEntity.email, JavaEmailConstants.FOLDER_OUTBOX, MessageState.AUTH_FAILURE.value,
                      MessageState.QUEUED.value)
                  MessagesSenderWorker.enqueue(context)
                  EmailManagerActivity.runEmailManagerActivity(context)
                }

                activity?.finish()
              }
            }
          }

          Activity.RESULT_CANCELED -> {
            Toast.makeText(requireContext(), getString(R.string.access_was_not_granted), Toast.LENGTH_SHORT).show()
          }
        }
      }
      else -> super.onActivityResult(requestCode, resultCode, data)
    }

  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    textViewExplanation.text = getString(R.string.reconnect_your_account,
        getString(R.string.app_name), accountEntity?.email ?: "")
  }

  private fun initViews(view: View) {
    textViewExplanation = view.findViewById(R.id.textViewExplanation)
    view.findViewById<View>(R.id.buttonReconnect)?.setOnClickListener {
      account?.let { accountEntity ->
        when (accountEntity.accountType) {
          AccountEntity.ACCOUNT_TYPE_GOOGLE -> {
            val firstArgName = arguments?.keySet()?.firstOrNull() ?: return@setOnClickListener
            val recoverableIntent = arguments?.getParcelable<Intent>(firstArgName)?.getParcelableExtra<Intent>("recoverableIntent")
                ?: return@setOnClickListener
            startActivityForResult(recoverableIntent, REQUEST_CODE_RUN_GMAIL_RECOVERABLE_INTENT)
          }

          AccountEntity.ACCOUNT_TYPE_OUTLOOK -> {

          }

          else -> {
            Toast.makeText(requireContext(), getString(R.string.access_was_not_granted), Toast.LENGTH_SHORT).show()
          }
        }
      }
    }
    view.findViewById<View>(R.id.buttonLogout)?.setOnClickListener { baseActivity.logout() }
    view.findViewById<View>(R.id.buttonPrivacy)?.setOnClickListener {
      GeneralUtil.openCustomTab(requireContext(), Constants.FLOWCRYPT_PRIVACY_URL)
    }
    view.findViewById<View>(R.id.buttonTerms)?.setOnClickListener {
      GeneralUtil.openCustomTab(requireContext(), Constants.FLOWCRYPT_TERMS_URL)
    }
    view.findViewById<View>(R.id.buttonSecurity)?.setOnClickListener {
      startActivity(HtmlViewFromAssetsRawActivity.newIntent(requireContext(), getString(R.string.security), "html/security.htm"))
    }
  }

  companion object {
    private const val REQUEST_CODE_RUN_GMAIL_RECOVERABLE_INTENT = 101
  }
}