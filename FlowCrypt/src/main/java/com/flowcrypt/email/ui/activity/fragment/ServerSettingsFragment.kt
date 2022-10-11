/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailProviderSettingsHelper
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.MessageState
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.databinding.FragmentServerSettingsBinding
import com.flowcrypt.email.extensions.hideKeyboard
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.onItemSelected
import com.flowcrypt.email.extensions.showTwoWayDialog
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.AccountSettingsViewModel
import com.flowcrypt.email.jetpack.workmanager.MessagesSenderWorker
import com.flowcrypt.email.jetpack.workmanager.sync.InboxIdleSyncWorker
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.ui.notifications.ErrorNotificationManager
import com.sun.mail.util.MailConnectException
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException

/**
 * @author Denis Bondarenko
 *         Date: 11/11/20
 *         Time: 2:16 PM
 *         E-mail: DenBond7@gmail.com
 */
class ServerSettingsFragment : BaseFragment<FragmentServerSettingsBinding>(), ProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentServerSettingsBinding.inflate(inflater, container, false)

  private var isImapSpinnerRestored: Boolean = false
  private var isSmtpSpinnerRestored: Boolean = false

  private val accountSettingsViewModel: AccountSettingsViewModel by viewModels()

  override val progressView: View?
    get() = binding?.progress?.root
  override val contentView: View?
    get() = binding?.layoutContent
  override val statusView: View?
    get() = binding?.status?.root

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ErrorNotificationManager.isShowingAuthErrorEnabled = false
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    ErrorNotificationManager(requireContext()).cancel(R.id.notification_id_auth_failure)
    initViews()
    setupChangeAuthCredentialsViewModel()
    initAccountViewModel()
    observeOnResultLiveData()
    subscribeToTwoWayDialog()
  }

  private fun setupChangeAuthCredentialsViewModel() {
    lifecycleScope.launchWhenStarted {
      accountSettingsViewModel.accountSettingsStateFlow.collect {
        updateViews(it)
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    ErrorNotificationManager.isShowingAuthErrorEnabled = true
  }

  private fun initViews() {
    binding?.editTextPassword?.doAfterTextChanged {
      binding?.editTextSmtpPassword?.text = it
    }

    binding?.checkBoxRequireSignInForSmtp?.setOnCheckedChangeListener { _, isChecked ->
      binding?.groupRequireSignInForSmtp?.isVisible = isChecked
    }

    binding?.spinnerImapSecurityType?.adapter = ArrayAdapter(
      requireContext(),
      android.R.layout.simple_spinner_dropdown_item,
      SecurityType.generateSecurityTypes(requireContext())
    )

    binding?.spinnerSmtpSecurityType?.adapter = ArrayAdapter(
      requireContext(),
      android.R.layout.simple_spinner_dropdown_item,
      SecurityType.generateSecurityTypes(requireContext())
    )

    binding?.spinnerImapSecurityType?.onItemSelected { parent, _, position, _ ->
      val securityType = parent?.adapter?.getItem(position) as SecurityType
      if (isImapSpinnerRestored) {
        binding?.editTextImapPort?.setText(securityType.defImapPort.toString())
      } else {
        isImapSpinnerRestored = true
      }
    }

    binding?.spinnerSmtpSecurityType?.onItemSelected { parent, _, position, _ ->
      val securityType = parent?.adapter?.getItem(position) as SecurityType
      if (isSmtpSpinnerRestored) {
        binding?.editTextSmtpPort?.setText(securityType.defSmtpPort.toString())
      } else {
        isSmtpSpinnerRestored = true
      }
    }

    binding?.buttonCheckAndSave?.setOnClickListener {
      checkCredentials(it)
    }
  }

  private fun checkCredentials(view: View? = null) {
    if (isDataCorrect()) {
      view?.hideKeyboard()
      isImapSpinnerRestored = false
      isSmtpSpinnerRestored = false
      changeAuthCredentialsCache()
      accountSettingsViewModel.accountSettingsStateFlow.value?.let { accountEntity ->
        navController?.navigate(
          ServerSettingsFragmentDirections.actionServerSettingsFragmentToCheckCredentialsFragment(
            accountEntity
          )
        )
      }
    }
  }

  private fun updateViews(accountEntity: AccountEntity?) {
    accountEntity?.let { nonNullAccount ->
      showContent()
      binding?.editTextEmail?.setText(nonNullAccount.email)
      binding?.editTextUserName?.setText(nonNullAccount.username)
      binding?.editTextPassword?.setText(nonNullAccount.password)
      binding?.editTextImapServer?.setText(nonNullAccount.imapServer)
      binding?.editTextImapPort?.setText(nonNullAccount.imapPort.toString())
      binding?.editTextSmtpServer?.setText(nonNullAccount.smtpServer)
      binding?.editTextSmtpPort?.setText(nonNullAccount.smtpPort.toString())
      binding?.checkBoxRequireSignInForSmtp?.isChecked = nonNullAccount.smtpUseCustomSign ?: false
      binding?.editTextSmtpUsername?.setText(nonNullAccount.smtpUsername)

      if (accountEntity.useOAuth2) {
        binding?.editTextEmail?.isEnabled = false
        binding?.editTextUserName?.isEnabled = false
        binding?.editTextImapServer?.isEnabled = false
        binding?.editTextImapPort?.isEnabled = false
        binding?.editTextSmtpServer?.isEnabled = false
        binding?.checkBoxRequireSignInForSmtp?.isEnabled = false
        binding?.editTextSmtpUsername?.isEnabled = false
        binding?.spinnerImapSecurityType?.isEnabled = false
        binding?.layoutPassword?.isVisible = false
        binding?.buttonCheckAndSave?.isVisible = true
        binding?.checkBoxRequireSignInForSmtp?.isVisible = nonNullAccount.smtpUseCustomSign ?: false
      } else {
        binding?.buttonCheckAndSave?.isVisible = true
        binding?.editTextSmtpPassword?.setText(nonNullAccount.smtpPassword)
      }

      val imapOptionsCount = binding?.spinnerImapSecurityType?.adapter?.count ?: 0
      for (i in 0 until imapOptionsCount) {
        if (nonNullAccount.imapOpt() === (binding?.spinnerImapSecurityType?.adapter?.getItem(i) as SecurityType).opt) {
          binding?.spinnerImapSecurityType?.setSelection(i)
        }
      }

      val smtpOptionsCount = binding?.spinnerSmtpSecurityType?.adapter?.count ?: 0
      for (i in 0 until smtpOptionsCount) {
        if (nonNullAccount.smtpOpt() === (binding?.spinnerSmtpSecurityType?.adapter?.getItem(i) as SecurityType).opt) {
          binding?.spinnerSmtpSecurityType?.setSelection(i)
        }
      }
    }
  }

  private fun isDataCorrect(): Boolean {
    when {
      accountSettingsViewModel.cachedAccountEntity?.useOAuth2 != true && binding?.editTextPassword?.text.isNullOrEmpty() -> {
        showInfoSnackbar(
          binding?.editTextPassword, getString(
            R.string.text_must_not_be_empty, getString(R.string.password)
          )
        )
        binding?.editTextPassword?.requestFocus()
      }

      binding?.editTextImapServer?.text.isNullOrEmpty() -> {
        showInfoSnackbar(
          binding?.editTextImapServer, getString(
            R.string.text_must_not_be_empty,
            getString(R.string.imap_server)
          )
        )
        binding?.editTextImapServer?.requestFocus()
      }

      binding?.editTextImapPort?.text.isNullOrEmpty() -> {
        showInfoSnackbar(
          binding?.editTextImapPort, getString(
            R.string.text_must_not_be_empty,
            getString(R.string.imap_port)
          )
        )
        binding?.editTextImapPort?.requestFocus()
      }

      binding?.editTextSmtpServer?.text.isNullOrEmpty() -> {
        showInfoSnackbar(
          binding?.editTextSmtpServer, getString(
            R.string.text_must_not_be_empty,
            getString(R.string.smtp_server)
          )
        )
        binding?.editTextSmtpServer?.requestFocus()
      }

      binding?.editTextSmtpPort?.text.isNullOrEmpty() -> {
        showInfoSnackbar(
          binding?.editTextSmtpPort, getString(
            R.string.text_must_not_be_empty,
            getString(R.string.smtp_port)
          )
        )
        binding?.editTextSmtpPort?.requestFocus()
      }

      binding?.checkBoxRequireSignInForSmtp?.isChecked == true -> when {
        binding?.editTextSmtpUsername?.text.isNullOrEmpty() -> {
          showInfoSnackbar(
            binding?.editTextSmtpUsername, getString(
              R.string.text_must_not_be_empty,
              getString(R.string.smtp_username)
            )
          )
          binding?.editTextSmtpUsername?.requestFocus()
        }

        binding?.editTextSmtpPassword?.text.isNullOrEmpty() -> {
          showInfoSnackbar(
            binding?.editTextSmtpPassword, getString(
              R.string.text_must_not_be_empty,
              getString(R.string.smtp_password)
            )
          )
          binding?.editTextSmtpPassword?.requestFocus()
        }
        else -> return true
      }

      else -> return true
    }

    return false
  }

  private fun changeAuthCredentialsCache() {
    val imapPort =
      if (TextUtils.isEmpty(binding?.editTextImapPort?.text)) JavaEmailConstants.SSL_IMAP_PORT
      else Integer.parseInt(binding?.editTextImapPort?.text.toString())

    val smtpPort =
      if (TextUtils.isEmpty(binding?.editTextSmtpPort?.text)) JavaEmailConstants.SSL_SMTP_PORT
      else Integer.parseInt(binding?.editTextSmtpPort?.text.toString())

    val imapSecurityType = (binding?.spinnerImapSecurityType?.selectedItem as SecurityType).opt
    val smtpSecurityType = (binding?.spinnerSmtpSecurityType?.selectedItem as SecurityType).opt

    accountSettingsViewModel.cachedAccountEntity?.let {
      accountSettingsViewModel.updateCachedAccountSettings(
        it.copy(
          email = binding?.editTextEmail?.text.toString(),
          username = binding?.editTextUserName?.text.toString(),
          password = binding?.editTextPassword?.text.toString(),
          imapServer = binding?.editTextImapServer?.text.toString(),
          imapPort = imapPort,
          imapUseSslTls = imapSecurityType === SecurityType.Option.SSL_TLS,
          imapUseStarttls = imapSecurityType === SecurityType.Option.STARTLS,
          smtpServer = binding?.editTextSmtpServer?.text.toString(),
          smtpPort = smtpPort,
          smtpUseSslTls = smtpSecurityType === SecurityType.Option.SSL_TLS,
          smtpUseStarttls = smtpSecurityType === SecurityType.Option.STARTLS,
          smtpUseCustomSign = binding?.checkBoxRequireSignInForSmtp?.isChecked ?: false,
          smtpUsername = binding?.editTextSmtpUsername?.text.toString(),
          smtpPassword = binding?.editTextSmtpPassword?.text.toString()
        )
      )
    }
  }

  private fun observeOnResultLiveData() {
    setFragmentResultListener(CheckCredentialsFragment.REQUEST_KEY_CHECK_ACCOUNT_SETTINGS) { _, bundle ->
      val result: Result<*> =
        bundle.getSerializable(CheckCredentialsFragment.KEY_CHECK_ACCOUNT_SETTINGS_RESULT) as Result<*>
      when (result.status) {
        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showContent()
          val exception = result.exception ?: return@setFragmentResultListener
          val original = result.exception.cause
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
            binding?.editTextEmail?.text.toString(), binding?.editTextPassword?.text.toString()
          )?.faqUrl
          val dialogMsg =
            msg + (if (faqUrl.isNullOrEmpty()) "" else getString(R.string.provider_faq, faqUrl))

          showTwoWayDialog(
            requestCode = REQUEST_CODE_RETRY_SETTINGS_CHECKING,
            dialogTitle = title,
            dialogMsg = dialogMsg,
            positiveButtonTitle = getString(R.string.retry),
            negativeButtonTitle = getString(R.string.cancel),
            isCancelable = true,
            useLinkify = true
          )
        }

        Result.Status.SUCCESS -> {
          val isSuccess = result.data as? Boolean?

          if (isSuccess == true) {
            accountSettingsViewModel.cachedAccountEntity?.let { accountEntity ->
              accountViewModel.updateAccount(accountEntity)
            }
          } else {
            showContent()
          }
        }

        else -> {}
      }
    }
  }

  private fun initAccountViewModel() {
    accountViewModel.updateAccountLiveData.observe(viewLifecycleOwner) {
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
                navController?.navigateUp()
              }
            }
          }

          else -> {
            showContent()
            toast(getString(R.string.server_settings_were_not_updated))
          }
        }
      }
    }
  }

  private fun subscribeToTwoWayDialog() {
    setFragmentResultListener(TwoWayDialogFragment.REQUEST_KEY_BUTTON_CLICK) { _, bundle ->
      val requestCode = bundle.getInt(TwoWayDialogFragment.KEY_REQUEST_CODE)
      val result = bundle.getInt(TwoWayDialogFragment.KEY_RESULT)

      when (requestCode) {
        REQUEST_CODE_RETRY_SETTINGS_CHECKING -> if (result == TwoWayDialogFragment.RESULT_OK) {
          checkCredentials()
        }
      }
    }
  }

  companion object {
    private const val REQUEST_CODE_RETRY_SETTINGS_CHECKING = 10
  }
}
