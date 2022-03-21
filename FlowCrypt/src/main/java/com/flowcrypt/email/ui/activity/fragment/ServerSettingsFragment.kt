/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.flowcrypt.email.NavGraphDirections
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailProviderSettingsHelper
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.addInputFilter
import com.flowcrypt.email.extensions.getNavigationResult
import com.flowcrypt.email.extensions.getNavigationResultForDialog
import com.flowcrypt.email.extensions.hideKeyboard
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.onItemSelected
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.workmanager.MessagesSenderWorker
import com.flowcrypt.email.jetpack.workmanager.sync.InboxIdleSyncWorker
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.ui.notifications.ErrorNotificationManager
import com.flowcrypt.email.ui.widget.inputfilters.InputFilters
import com.sun.mail.util.MailConnectException
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException

/**
 * @author Denis Bondarenko
 *         Date: 11/11/20
 *         Time: 2:16 PM
 *         E-mail: DenBond7@gmail.com
 */
class ServerSettingsFragment : BaseFragment(), ProgressBehaviour {
  private var editTextEmail: EditText? = null
  private var editTextUserName: EditText? = null
  private var editTextPassword: EditText? = null
  private var editTextImapServer: EditText? = null
  private var editTextImapPort: EditText? = null
  private var editTextSmtpServer: EditText? = null
  private var editTextSmtpPort: EditText? = null
  private var editTextSmtpUsername: EditText? = null
  private var editTextSmtpPassword: EditText? = null
  private var spinnerImapSecurityType: Spinner? = null
  private var spinnerSmtpSecurityType: Spinner? = null
  private var checkBoxRequireSignInForSmtp: CheckBox? = null
  private var authCreds: AuthCredentials? = null

  private var isImapSpinnerRestored: Boolean = false
  private var isSmtpSpinnerRestored: Boolean = false

  override val progressView: View?
    get() = view?.findViewById(R.id.progress)
  override val contentView: View?
    get() = view?.findViewById(R.id.layoutContent)
  override val statusView: View?
    get() = view?.findViewById(R.id.status)

  override val contentResourceId: Int = R.layout.fragment_server_settings

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ErrorNotificationManager.isShowingAuthErrorEnabled = false
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    ErrorNotificationManager(requireContext()).cancel(R.id.notification_id_auth_failure)

    supportActionBar?.title = getString(R.string.server_settings)
    initViews(view)
    updateViews(authCreds)
    initAccountViewModel()
    observeOnResultLiveData()
  }

  override fun onDestroy() {
    super.onDestroy()
    ErrorNotificationManager.isShowingAuthErrorEnabled = true
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    accountEntity?.let {
      if (authCreds == null) {
        authCreds = AuthCredentials.from(it).copy(password = "", smtpSignInPassword = "")
        updateViews(authCreds)
      }
    }
  }

  private fun initViews(view: View) {
    editTextEmail = view.findViewById(R.id.editTextEmail)
    editTextUserName = view.findViewById(R.id.editTextUserName)
    editTextPassword = view.findViewById(R.id.editTextPassword)
    editTextImapServer = view.findViewById(R.id.editTextImapServer)
    editTextImapPort = view.findViewById(R.id.editTextImapPort)
    editTextSmtpServer = view.findViewById(R.id.editTextSmtpServer)
    editTextSmtpPort = view.findViewById(R.id.editTextSmtpPort)
    editTextSmtpUsername = view.findViewById(R.id.editTextSmtpUsername)
    editTextSmtpPassword = view.findViewById(R.id.editTextSmtpPassword)

    editTextEmail?.addInputFilter(InputFilters.NoCaps())
    editTextImapServer?.addInputFilter(InputFilters.NoCaps())
    editTextSmtpServer?.addInputFilter(InputFilters.NoCaps())
    editTextImapPort?.addInputFilter(InputFilters.OnlyDigits())
    editTextSmtpPort?.addInputFilter(InputFilters.OnlyDigits())
    editTextPassword?.doAfterTextChanged {
      editTextSmtpPassword?.text = it
    }

    checkBoxRequireSignInForSmtp = view.findViewById(R.id.checkBoxRequireSignInForSmtp)
    checkBoxRequireSignInForSmtp?.setOnCheckedChangeListener { _, isChecked ->
      view.findViewById<View>(R.id.groupRequireSignInForSmtp)?.isVisible = isChecked
    }

    spinnerImapSecurityType = view.findViewById(R.id.spinnerImapSecurityType)
    spinnerSmtpSecurityType = view.findViewById(R.id.spinnerSmtpSecurityType)

    spinnerImapSecurityType?.adapter = ArrayAdapter(
      requireContext(),
      android.R.layout.simple_spinner_dropdown_item,
      SecurityType.generateSecurityTypes(requireContext())
    )

    spinnerSmtpSecurityType?.adapter = ArrayAdapter(
      requireContext(),
      android.R.layout.simple_spinner_dropdown_item,
      SecurityType.generateSecurityTypes(requireContext())
    )

    spinnerImapSecurityType?.onItemSelected { parent, _, position, _ ->
      val securityType = parent?.adapter?.getItem(position) as SecurityType
      if (isImapSpinnerRestored) {
        editTextImapPort?.setText(securityType.defImapPort.toString())
      } else {
        isImapSpinnerRestored = true
      }
    }

    spinnerSmtpSecurityType?.onItemSelected { parent, _, position, _ ->
      val securityType = parent?.adapter?.getItem(position) as SecurityType
      if (isSmtpSpinnerRestored) {
        editTextSmtpPort?.setText(securityType.defSmtpPort.toString())
      } else {
        isSmtpSpinnerRestored = true
      }
    }

    view.findViewById<View>(R.id.buttonCheckAndSave)?.setOnClickListener {
      checkCredentials(it)
    }
  }

  private fun checkCredentials(view: View? = null) {
    if (isDataCorrect()) {
      view?.hideKeyboard()
      authCreds = generateAuthCreds()
      authCreds?.let { authCredentials ->
        isImapSpinnerRestored = false
        isSmtpSpinnerRestored = false
        navController?.navigate(
          ServerSettingsFragmentDirections.actionServerSettingsFragmentToCheckCredentialsFragment(
            AccountEntity(authCredentials)
          )
        )
      }
    }
  }

  private fun updateViews(authCreds: AuthCredentials?) {
    authCreds?.let { nonNullAuthCreds ->
      showContent()
      editTextEmail?.setText(nonNullAuthCreds.email)
      editTextUserName?.setText(nonNullAuthCreds.username)
      editTextPassword?.setText(nonNullAuthCreds.password)
      editTextImapServer?.setText(nonNullAuthCreds.imapServer)
      editTextImapPort?.setText(nonNullAuthCreds.imapPort.toString())
      editTextSmtpServer?.setText(nonNullAuthCreds.smtpServer)
      editTextSmtpPort?.setText(nonNullAuthCreds.smtpPort.toString())
      checkBoxRequireSignInForSmtp?.isChecked = nonNullAuthCreds.hasCustomSignInForSmtp
      editTextSmtpUsername?.setText(nonNullAuthCreds.smtpSigInUsername)

      if (authCreds.useOAuth2) {
        editTextEmail?.isEnabled = false
        editTextUserName?.isEnabled = false
        editTextImapServer?.isEnabled = false
        editTextImapPort?.isEnabled = false
        editTextSmtpServer?.isEnabled = false
        editTextSmtpPort?.isEnabled = false
        checkBoxRequireSignInForSmtp?.isEnabled = false
        editTextSmtpUsername?.isEnabled = false
        view?.findViewById<View>(R.id.spinnerImapSecurityType)?.isEnabled = false
        view?.findViewById<View>(R.id.spinnerSmtpSecurityType)?.isEnabled = false
        view?.findViewById<View>(R.id.layoutPassword)?.isVisible = false
        view?.findViewById<View>(R.id.buttonCheckAndSave)?.isVisible = false

        if (!nonNullAuthCreds.hasCustomSignInForSmtp) {
          checkBoxRequireSignInForSmtp?.isVisible = false
        }

        toast(text = getString(R.string.settings_oauth_note), duration = Toast.LENGTH_LONG)
      } else {
        view?.findViewById<View>(R.id.buttonCheckAndSave)?.isVisible = true
        editTextSmtpPassword?.setText(nonNullAuthCreds.smtpSignInPassword)
      }

      val imapOptionsCount = spinnerImapSecurityType?.adapter?.count ?: 0
      for (i in 0 until imapOptionsCount) {
        if (nonNullAuthCreds.imapOpt === (spinnerImapSecurityType?.adapter?.getItem(i) as SecurityType).opt) {
          spinnerImapSecurityType?.setSelection(i)
        }
      }

      val smtpOptionsCount = spinnerSmtpSecurityType?.adapter?.count ?: 0
      for (i in 0 until smtpOptionsCount) {
        if (nonNullAuthCreds.smtpOpt === (spinnerSmtpSecurityType?.adapter?.getItem(i) as SecurityType).opt) {
          spinnerSmtpSecurityType?.setSelection(i)
        }
      }
    }
  }

  private fun isDataCorrect(): Boolean {
    when {
      editTextPassword?.text.isNullOrEmpty() -> {
        showInfoSnackbar(
          editTextPassword, getString(
            R.string.text_must_not_be_empty,
            getString(R.string.password)
          )
        )
        editTextPassword?.requestFocus()
      }

      editTextImapServer?.text.isNullOrEmpty() -> {
        showInfoSnackbar(
          editTextImapServer, getString(
            R.string.text_must_not_be_empty,
            getString(R.string.imap_server)
          )
        )
        editTextImapServer?.requestFocus()
      }

      editTextImapPort?.text.isNullOrEmpty() -> {
        showInfoSnackbar(
          editTextImapPort, getString(
            R.string.text_must_not_be_empty,
            getString(R.string.imap_port)
          )
        )
        editTextImapPort?.requestFocus()
      }

      editTextSmtpServer?.text.isNullOrEmpty() -> {
        showInfoSnackbar(
          editTextSmtpServer, getString(
            R.string.text_must_not_be_empty,
            getString(R.string.smtp_server)
          )
        )
        editTextSmtpServer?.requestFocus()
      }

      editTextSmtpPort?.text.isNullOrEmpty() -> {
        showInfoSnackbar(
          editTextSmtpPort, getString(
            R.string.text_must_not_be_empty,
            getString(R.string.smtp_port)
          )
        )
        editTextSmtpPort?.requestFocus()
      }

      checkBoxRequireSignInForSmtp?.isChecked == true -> when {
        editTextSmtpUsername?.text.isNullOrEmpty() -> {
          showInfoSnackbar(
            editTextSmtpUsername, getString(
              R.string.text_must_not_be_empty,
              getString(R.string.smtp_username)
            )
          )
          editTextSmtpUsername?.requestFocus()
        }

        editTextSmtpPassword?.text.isNullOrEmpty() -> {
          showInfoSnackbar(
            editTextSmtpPassword, getString(
              R.string.text_must_not_be_empty,
              getString(R.string.smtp_password)
            )
          )
          editTextSmtpPassword?.requestFocus()
        }
        else -> return true
      }

      else -> return true
    }

    return false
  }

  private fun generateAuthCreds(): AuthCredentials {
    val imapPort = if (TextUtils.isEmpty(editTextImapPort?.text))
      JavaEmailConstants.SSL_IMAP_PORT
    else
      Integer.parseInt(editTextImapPort?.text.toString())

    val smtpPort = if (TextUtils.isEmpty(editTextSmtpPort?.text))
      JavaEmailConstants.SSL_SMTP_PORT
    else
      Integer.parseInt(editTextSmtpPort?.text.toString())

    return AuthCredentials(
      email = editTextEmail?.text.toString(),
      username = editTextUserName?.text.toString(),
      password = editTextPassword?.text.toString(),
      imapServer = editTextImapServer?.text.toString(),
      imapPort = imapPort,
      imapOpt = (spinnerImapSecurityType?.selectedItem as SecurityType).opt,
      smtpServer = editTextSmtpServer?.text.toString(),
      smtpPort = smtpPort,
      smtpOpt = (spinnerSmtpSecurityType?.selectedItem as SecurityType).opt,
      hasCustomSignInForSmtp = checkBoxRequireSignInForSmtp?.isChecked ?: false,
      smtpSigInUsername = editTextSmtpUsername?.text.toString(),
      smtpSignInPassword = editTextSmtpPassword?.text.toString()
    )
  }

  private fun observeOnResultLiveData() {
    getNavigationResult<Result<*>>(CheckCredentialsFragment.KEY_CHECK_ACCOUNT_SETTINGS_RESULT) {
      when (it.status) {
        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showContent()
          val exception = it.exception ?: return@getNavigationResult
          val original = it.exception.cause
          var title: String? = null
          val msg: String? = if (exception.message.isNullOrEmpty()) {
            exception.javaClass.simpleName
          } else exception.message

          if (original != null) {
            if (original is MailConnectException || original is SocketTimeoutException) {
              title = getString(R.string.network_error)
            }
          }

          val faqUrl = EmailProviderSettingsHelper.getBaseSettings(
            editTextEmail?.text.toString(), editTextPassword?.text.toString()
          )?.faqUrl
          val dialogMsg =
            msg + (if (faqUrl.isNullOrEmpty()) "" else getString(R.string.provider_faq, faqUrl))

          navController?.navigate(
            NavGraphDirections.actionGlobalTwoWayDialogFragment(
              requestCode = REQUEST_CODE_RETRY_SETTINGS_CHECKING,
              dialogTitle = title,
              dialogMsg = dialogMsg,
              positiveButtonTitle = getString(R.string.retry),
              negativeButtonTitle = getString(R.string.cancel),
              isCancelable = true,
              useLinkify = true
            )
          )
        }

        Result.Status.SUCCESS -> {
          val isSuccess = it.data as? Boolean?

          if (isSuccess == true) {
            authCreds?.let { authCredentials ->
              accountViewModel.updateAccountByAuthCredentials(authCredentials)
            }
          } else {
            showContent()
          }
        }

        else -> {

        }
      }
    }

    getNavigationResultForDialog<TwoWayDialogFragment.Result>(
      destinationId = R.id.serverSettingsFragment,
      key = TwoWayDialogFragment.KEY_RESULT
    ) {
      when (it.requestCode) {
        REQUEST_CODE_RETRY_SETTINGS_CHECKING -> {
          when (it.resultCode) {
            TwoWayDialogFragment.RESULT_OK -> {
              checkCredentials()
            }
          }
        }
      }
    }
  }

  private fun initAccountViewModel() {
    accountViewModel.updateAuthCredentialsLiveData.observe(viewLifecycleOwner, {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            showProgress(it.progressMsg)
          }

          Result.Status.SUCCESS -> {
            lifecycleScope.launch {
              context?.let { context ->
                val roomDatabase = FlowCryptRoomDatabase.getDatabase(context)
                roomDatabase.msgDao().changeMsgsStateSuspend(
                  account?.email, JavaEmailConstants.FOLDER_OUTBOX, MessageState.AUTH_FAILURE.value,
                  MessageState.QUEUED.value
                )
                MessagesSenderWorker.enqueue(context, true)
                InboxIdleSyncWorker.enqueue(context)
                toast(text = getString(R.string.server_settings_updated))
                //EmailManagerActivity.runEmailManagerActivity(context)
              }
              activity?.finish()
            }
          }

          else -> {
            showContent()
            toast(getString(R.string.server_settings_were_not_updated))
          }
        }
      }
    })
  }

  companion object {
    private const val REQUEST_CODE_RETRY_SETTINGS_CHECKING = 10
  }
}
