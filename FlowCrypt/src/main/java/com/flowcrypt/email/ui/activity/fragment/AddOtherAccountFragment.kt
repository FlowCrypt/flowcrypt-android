/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.accounts.Account
import android.accounts.AccountManager
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.preference.PreferenceManager
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.accounts.FlowcryptAccountAuthenticator
import com.flowcrypt.email.api.email.EmailProviderSettingsHelper
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailConstants
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.api.oauth.OAuth2Helper
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.databinding.FragmentAddOtherAccountBinding
import com.flowcrypt.email.extensions.addInputFilter
import com.flowcrypt.email.extensions.android.os.getParcelableArrayListViaExt
import com.flowcrypt.email.extensions.android.os.getSerializableViaExt
import com.flowcrypt.email.extensions.hideKeyboard
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.showFeedbackFragment
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showInfoDialogWithExceptionDetails
import com.flowcrypt.email.extensions.showTwoWayDialog
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseSingInFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.ui.widget.inputfilters.InputFilters
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.SharedPreferencesHelper
import com.flowcrypt.email.util.exception.AccountAlreadyAddedException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.sun.mail.util.MailConnectException
import jakarta.mail.AuthenticationFailedException
import java.net.SocketTimeoutException

/**
 * @author Denis Bondarenko
 *         Date: 7/20/20
 *         Time: 3:39 PM
 *         E-mail: DenBond7@gmail.com
 */
class AddOtherAccountFragment : BaseSingInFragment<FragmentAddOtherAccountBinding>(),
  AdapterView.OnItemSelectedListener {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentAddOtherAccountBinding.inflate(inflater, container, false)

  private var authCreds: AuthCredentials? = null

  private var isImapSpinnerRestored: Boolean = false
  private var isSmtpSpinnerRestored: Boolean = false

  override val progressView: View?
    get() = binding?.progress?.root
  override val contentView: View?
    get() = binding?.layoutContent
  override val statusView: View?
    get() = binding?.status?.root

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.authCreds = getTempAuthCreds()

    if (authCreds == null) {
      isImapSpinnerRestored = true
      isSmtpSpinnerRestored = true
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)
    updateView(authCreds)

    subscribeToCheckAccountSettings()
    subscribeToAuthorizeAndSearchBackups()
    subscribeToCheckPrivateKeys()
    subscribeCreateOrImportPrivateKeyDuringSetup()
    subscribeToTwoWayDialog()

    setupOAuth2AuthCredentialsViewModel()
    initAddNewAccountLiveData()
    initPrivateKeysViewModel()
  }

  override fun onPause() {
    super.onPause()
    saveTempCreds()
  }

  override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
    when (parent?.id) {
      R.id.spinnerImapSecurityType -> {
        val (_, _, defImapPort) = parent.adapter.getItem(position) as SecurityType
        if (isImapSpinnerRestored) {
          binding?.editTextImapPort?.setText(defImapPort.toString())
        } else {
          isImapSpinnerRestored = true
        }
      }

      R.id.spinnerSmtpSecyrityType -> {
        val (_, _, _, defSmtpPort) = parent.adapter.getItem(position) as SecurityType
        if (isSmtpSpinnerRestored) {
          binding?.editTextSmtpPort?.setText(defSmtpPort.toString())
        } else {
          isSmtpSpinnerRestored = true
        }
      }
    }
  }

  override fun onNothingSelected(parent: AdapterView<*>) {

  }

  override fun navigateToPrimaryAccountMessagesList(accountEntity: AccountEntity) {
    if (authCreds?.useOAuth2 == true) {
      storeAccountInfoToAccountManager()
    }
    super.navigateToPrimaryAccountMessagesList(accountEntity)
  }

  override fun getTempAccount(): AccountEntity {
    val authCreds = generateAuthCreds()
    return AccountEntity(
      if (authCreds.useOAuth2) {
        authCreds.copy(password = "", smtpSignInPassword = null)
      } else {
        authCreds
      }
    )
  }

  override fun onAccountAdded(accountEntity: AccountEntity) {
    //we should be sure we save keys with the same source type
    if (importCandidates.mapNotNull { it.importSourceType }.toSet().size == 1) {
      privateKeysViewModel.encryptAndSaveKeysToDatabase(
        accountEntity,
        importCandidates
      )
    } else {
      //remove account and show error
      accountViewModel.deleteAccount(accountEntity)
      showInfoDialog(
        dialogTitle = getString(R.string.error),
        dialogMsg = getString(R.string.import_keys_from_different_sources)
      )
    }
  }

  override fun onAdditionalActionsAfterPrivateKeyCreationCompleted(
    accountEntity: AccountEntity,
    pgpKeyDetails: PgpKeyDetails
  ) {
    handleUnlockedKeys(listOf(pgpKeyDetails))
  }

  override fun onAdditionalActionsAfterPrivateKeyImportingCompleted(
    accountEntity: AccountEntity,
    keys: List<PgpKeyDetails>
  ) {
    handleUnlockedKeys(keys)
  }

  override fun switchAccount(accountEntity: AccountEntity) {
    if (authCreds?.useOAuth2 == true) {
      storeAccountInfoToAccountManager()
    }

    super.switchAccount(accountEntity)
  }

  private fun initViews(view: View) {
    binding?.editTextEmail?.addInputFilter(InputFilters.NoCaps())
    binding?.editTextImapServer?.addInputFilter(InputFilters.NoCaps())
    binding?.editTextSmtpServer?.addInputFilter(InputFilters.NoCaps())
    binding?.editTextImapPort?.addInputFilter(InputFilters.OnlyDigits())
    binding?.editTextSmtpPort?.addInputFilter(InputFilters.OnlyDigits())

    binding?.editTextPassword?.setOnEditorActionListener { _, actionId, _ ->
      return@setOnEditorActionListener when (actionId) {
        EditorInfo.IME_ACTION_DONE -> {
          tryToConnect()
          true
        }
        else -> false
      }
    }

    binding?.editTextEmail?.doAfterTextChanged {
      if (GeneralUtil.isEmailValid(it)) {
        if (binding?.checkBoxAdvancedMode?.isChecked == false) {
          if (applyRecommendSettings()) return@doAfterTextChanged
        }

        val email = it.toString()
        val mainDomain = email.substring(email.indexOf('@') + 1)
        binding?.editTextImapServer?.setText(getString(R.string.template_imap_server, mainDomain))
        binding?.editTextSmtpServer?.setText(getString(R.string.template_smtp_server, mainDomain))
        binding?.editTextUserName?.setText(email)
        binding?.editTextSmtpUsername?.setText(email)
      }
    }

    binding?.editTextPassword?.doAfterTextChanged {
      if (binding?.checkBoxAdvancedMode?.isChecked == false) {
        val recommendAuthCredentials = EmailProviderSettingsHelper.getBaseSettings(
          binding?.editTextEmail?.text.toString(), binding?.editTextPassword?.text.toString()
        )

        binding?.editTextSmtpPassword?.setText(recommendAuthCredentials?.smtpSignInPassword)
      }
    }

    binding?.checkBoxRequireSignInForSmtp?.setOnCheckedChangeListener { _, isChecked ->
      if (binding?.checkBoxAdvancedMode?.isChecked == true) {
        binding?.groupRequireSignInForSmtp?.visibility = if (isChecked) View.VISIBLE else View.GONE
      }
    }

    binding?.checkBoxAdvancedMode?.setOnCheckedChangeListener { buttonView, isChecked ->
      buttonView.hideKeyboard()
      view.findViewById<View>(R.id.groupAdvancedSettings)?.visibility =
        if (isChecked) View.VISIBLE else View.GONE
      if ((binding?.checkBoxRequireSignInForSmtp?.isChecked == true) && isChecked) {
        binding?.groupRequireSignInForSmtp?.visibility = View.VISIBLE
      } else {
        binding?.groupRequireSignInForSmtp?.visibility = View.GONE
      }

      if (!isChecked) {
        applyRecommendSettings()
      }
    }

    val adapter = ArrayAdapter(
      requireContext(), android.R.layout.simple_spinner_dropdown_item,
      SecurityType.generateSecurityTypes(requireContext())
    )

    binding?.spinnerImapSecurityType?.adapter = adapter
    binding?.spinnerSmtpSecyrityType?.adapter = adapter

    binding?.spinnerImapSecurityType?.onItemSelectedListener = this
    binding?.spinnerSmtpSecyrityType?.onItemSelectedListener = this

    view.findViewById<View>(R.id.buttonTryToConnect)?.setOnClickListener {
      importCandidates.clear()
      tryToConnect()
    }

    view.findViewById<View>(R.id.buttonHelp)?.setOnClickListener {
      showFeedbackFragment()
    }

    binding?.buttonSignInWithOutlook?.setOnClickListener {
      importCandidates.clear()
      it.isEnabled = false
      oAuth2AuthCredentialsViewModel.getAuthorizationRequestForProvider(
        provider = OAuth2Helper.Provider.MICROSOFT
      )
    }
  }

  /**
   * Update the current views if [AuthCredentials] is not null
   */
  private fun updateView(authCreds: AuthCredentials?, updateEmail: Boolean = true) {
    authCreds?.let { nonNullAuthCreds ->
      if (updateEmail) {
        binding?.editTextEmail?.setText(nonNullAuthCreds.email)
      }
      binding?.editTextUserName?.setText(nonNullAuthCreds.username)
      binding?.editTextImapServer?.setText(nonNullAuthCreds.imapServer)
      binding?.editTextImapPort?.setText(nonNullAuthCreds.imapPort.toString())
      binding?.editTextSmtpServer?.setText(nonNullAuthCreds.smtpServer)
      binding?.editTextSmtpPort?.setText(nonNullAuthCreds.smtpPort.toString())
      binding?.checkBoxRequireSignInForSmtp?.isChecked = nonNullAuthCreds.hasCustomSignInForSmtp
      binding?.editTextSmtpUsername?.setText(nonNullAuthCreds.smtpSigInUsername)
      binding?.editTextSmtpPassword?.setText(nonNullAuthCreds.smtpSignInPassword)

      val imapOptionsCount = binding?.spinnerImapSecurityType?.adapter?.count ?: 0
      for (i in 0 until imapOptionsCount) {
        if (nonNullAuthCreds.imapOpt === (binding?.spinnerImapSecurityType?.adapter?.getItem(i) as SecurityType).opt) {
          binding?.spinnerImapSecurityType?.setSelection(i)
        }
      }

      val smtpOptionsCount = binding?.spinnerSmtpSecyrityType?.adapter?.count ?: 0
      for (i in 0 until smtpOptionsCount) {
        if (nonNullAuthCreds.smtpOpt === (binding?.spinnerSmtpSecyrityType?.adapter?.getItem(i) as SecurityType).opt) {
          binding?.spinnerSmtpSecyrityType?.setSelection(i)
        }
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun subscribeToCheckAccountSettings() {
    setFragmentResultListener(AuthorizeAndSearchBackupsFragment.REQUEST_KEY_CHECK_ACCOUNT_SETTINGS) { _, bundle ->
      val result: Result<*>? =
        bundle.getSerializableViaExt(AuthorizeAndSearchBackupsFragment.KEY_CHECK_ACCOUNT_SETTINGS_RESULT) as? Result<*>

      if (result != null) {
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
              if (original is AuthenticationFailedException) {
                val isGmailImapServer = binding?.editTextImapServer?.text.toString()
                  .equals(GmailConstants.GMAIL_IMAP_SERVER, ignoreCase = true)
                val isMsgEmpty = TextUtils.isEmpty(original.message)
                val hasAlert = original.message?.startsWith(
                  GmailConstants
                    .GMAIL_ALERT_MESSAGE_WHEN_LESS_SECURE_NOT_ALLOWED
                )
                if (isGmailImapServer && !isMsgEmpty && hasAlert == true) {
                  showLessSecurityWarning()
                  return@setFragmentResultListener
                }
              } else if (original is MailConnectException || original is SocketTimeoutException) {
                title = getString(R.string.network_error)
              }
            } else if (exception is AccountAlreadyAddedException) {
              showInfoSnackbar(view, exception.message, Snackbar.LENGTH_LONG)
              return@setFragmentResultListener
            }

            val faqUrl = EmailProviderSettingsHelper.getBaseSettings(
              binding?.editTextEmail?.text.toString(), binding?.editTextPassword?.text.toString()
            )?.faqUrl

            val dialogMsg = if (binding?.checkBoxAdvancedMode?.isChecked == false) {
              getString(R.string.show_error_msg_with_recommendations, msg)
            } else {
              msg
            } + if (faqUrl.isNullOrEmpty()) "" else getString(R.string.provider_faq, faqUrl)

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

          else -> {

          }
        }
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  private fun subscribeToAuthorizeAndSearchBackups() {
    setFragmentResultListener(AuthorizeAndSearchBackupsFragment.REQUEST_KEY_SEARCH_BACKUPS) { _, bundle ->
      val result: Result<*>? =
        bundle.getSerializableViaExt(AuthorizeAndSearchBackupsFragment.KEY_PRIVATE_KEY_BACKUPS_RESULT) as? Result<*>

      if (result != null) {
        when (result.status) {
          Result.Status.SUCCESS -> {
            dismissCurrentSnackBar()

            val keyDetailsList = result.data as ArrayList<PgpKeyDetails>?
            if (keyDetailsList?.isEmpty() == true) {
              authCreds?.let { authCredentials ->
                val account = AccountEntity(authCredentials)
                navController?.navigate(
                  AddOtherAccountFragmentDirections
                    .actionAddOtherAccountFragmentToCreateOrImportPrivateKeyDuringSetupFragment(
                      accountEntity = account, isShowAnotherAccountBtnEnabled = true
                    )
                )
                showContent()
              }
            } else {
              navController?.navigate(
                AddOtherAccountFragmentDirections
                  .actionAddOtherAccountFragmentToCheckKeysFragment(
                    privateKeys = (keyDetailsList ?: ArrayList()).toTypedArray(),
                    sourceType = KeyImportDetails.SourceType.EMAIL,
                    positiveBtnTitle = getString(R.string.continue_),
                    negativeBtnTitle = getString(R.string.use_another_account),
                    initSubTitlePlurals = R.plurals.found_backup_of_your_account_key
                  )
              )
            }
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            showContent()
            showInfoDialogWithExceptionDetails(
              result.exception,
              getString(R.string.could_not_load_private_keys)
            )
          }

          else -> {

          }
        }
      }
    }
  }

  private fun subscribeToCheckPrivateKeys() {
    setFragmentResultListener(CheckKeysFragment.REQUEST_KEY_CHECK_PRIVATE_KEYS) { _, bundle ->
      val keys = bundle.getParcelableArrayListViaExt(
        CheckKeysFragment.KEY_UNLOCKED_PRIVATE_KEYS
      ) ?: emptyList<PgpKeyDetails>()
      when (bundle.getInt(CheckKeysFragment.KEY_STATE)) {
        CheckKeysFragment.CheckingState.CHECKED_KEYS, CheckKeysFragment.CheckingState.SKIP_REMAINING_KEYS -> {
          handleUnlockedKeys(keys)
        }

        CheckKeysFragment.CheckingState.NO_NEW_KEYS -> {
          toast(R.string.key_already_imported_finishing_setup, Toast.LENGTH_SHORT)
          onSetupCompleted(getTempAccount())
        }

        CheckKeysFragment.CheckingState.CANCELED -> showContent()
      }
    }
  }

  private fun subscribeCreateOrImportPrivateKeyDuringSetup() {
    setFragmentResultListener(CreateOrImportPrivateKeyDuringSetupFragment.REQUEST_KEY_PRIVATE_KEYS) { _, bundle ->
      @CreateOrImportPrivateKeyDuringSetupFragment.Result val result =
        bundle.getInt(CreateOrImportPrivateKeyDuringSetupFragment.KEY_STATE)

      val keys = bundle.getParcelableArrayListViaExt(
        CreateOrImportPrivateKeyDuringSetupFragment.KEY_PRIVATE_KEYS
      ) ?: emptyList<PgpKeyDetails>()

      when (result) {
        CreateOrImportPrivateKeyDuringSetupFragment.Result.HANDLE_RESOLVED_KEYS -> {
          if (keys.isNotEmpty()) {
            privateKeysViewModel.doAdditionalActionsAfterPrivateKeysImporting(
              getTempAccount(),
              keys
            )
          }
        }

        CreateOrImportPrivateKeyDuringSetupFragment.Result.HANDLE_CREATED_KEY -> {
          if (keys.isNotEmpty()) {
            privateKeysViewModel.doAdditionalActionsAfterPrivateKeyCreation(
              getTempAccount(),
              keys
            )
          }
        }

        CreateOrImportPrivateKeyDuringSetupFragment.Result.USE_ANOTHER_ACCOUNT -> {
          showContent()
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
          tryToConnect()
        }
      }
    }
  }

  private fun setupOAuth2AuthCredentialsViewModel() {
    oAuth2AuthCredentialsViewModel.microsoftOAuth2TokenLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          showProgress(progressMsg = getString(R.string.loading_account_details))
        }

        Result.Status.SUCCESS -> {
          it.data?.let { authCredentials ->
            authCreds = authCredentials
            oAuth2AuthCredentialsViewModel.microsoftOAuth2TokenLiveData.value = Result.none()

            val existingAccount = existingAccounts.firstOrNull { account ->
              account.email.equals(authCredentials.email, ignoreCase = true)
            }

            if (existingAccount == null) {
              val account = AccountEntity(authCredentials).copy(
                password = authCredentials.peekPassword(),
                smtpPassword = authCredentials.peekSmtpPassword()
              )

              navController?.navigate(
                AddOtherAccountFragmentDirections
                  .actionAddOtherAccountFragmentToAuthorizeAndSearchBackupsFragment(account)
              )

              return@let
            } else {
              showContent()
              showInfoSnackbar(
                msgText = getString(R.string.template_email_already_added, existingAccount.email),
                duration = Snackbar.LENGTH_LONG
              )
            }
          }
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          oAuth2AuthCredentialsViewModel.microsoftOAuth2TokenLiveData.value = Result.none()
          showContent()
          showInfoDialogWithExceptionDetails(
            it.exception,
            "Couldn't fetch token"
          )
        }
        else -> {}
      }
    }

    oAuth2AuthCredentialsViewModel.authorizationRequestLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          showProgress(progressMsg = getString(R.string.loading_oauth_server_configuration))
        }

        Result.Status.SUCCESS -> {
          it.data?.let { authorizationRequest ->
            oAuth2AuthCredentialsViewModel.authorizationRequestLiveData.value = Result.none()
            binding?.buttonSignInWithOutlook?.isEnabled = true
            showContent()

            authRequest = authorizationRequest
            authRequest?.let { request -> processAuthorizationRequest(request) }
          }
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          oAuth2AuthCredentialsViewModel.authorizationRequestLiveData.value = Result.none()
          binding?.buttonSignInWithOutlook?.isEnabled = true
          showContent()
          showInfoDialogWithExceptionDetails(
            it.exception,
            getString(R.string.could_not_load_oauth_server_configuration)
          )
        }
        else -> {}
      }
    }
  }

  /**
   * Retrieve a temp [AuthCredentials] from the shared preferences.
   */
  private fun getTempAuthCreds(): AuthCredentials? {
    val authCredsJson =
      SharedPreferencesHelper.getString(
        PreferenceManager.getDefaultSharedPreferences(requireContext()),
        Constants.PREF_KEY_TEMP_LAST_AUTH_CREDENTIALS, ""
      )

    if (authCredsJson?.isNotEmpty() == true) {
      try {
        return Gson().fromJson(authCredsJson, AuthCredentials::class.java)
      } catch (e: JsonSyntaxException) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
      }
    }

    return null
  }

  /**
   * Save the current [AuthCredentials] to the shared preferences.
   */
  private fun saveTempCreds() {
    authCreds?.let { if (it.useOAuth2) return }

    val authCreds = generateAuthCreds().copy(password = "", smtpSignInPassword = "")
    SharedPreferencesHelper.setString(
      PreferenceManager.getDefaultSharedPreferences(requireContext()),
      Constants.PREF_KEY_TEMP_LAST_AUTH_CREDENTIALS, Gson().toJson(authCreds)
    )
  }

  /**
   * Generate the [AuthCredentials] using user input.
   *
   * @return [AuthCredentials].
   */
  private fun generateAuthCreds(): AuthCredentials {
    authCreds?.let {
      if (it.useOAuth2) return it
    }

    val imapPort = if (TextUtils.isEmpty(binding?.editTextImapPort?.text))
      JavaEmailConstants.SSL_IMAP_PORT
    else
      Integer.parseInt(binding?.editTextImapPort?.text.toString())

    val smtpPort = if (TextUtils.isEmpty(binding?.editTextSmtpPort?.text))
      JavaEmailConstants.SSL_SMTP_PORT
    else
      Integer.parseInt(binding?.editTextSmtpPort?.text.toString())

    return AuthCredentials(
      email = binding?.editTextEmail?.text.toString(),
      username = binding?.editTextUserName?.text.toString(),
      password = binding?.editTextPassword?.text.toString(),
      imapServer = binding?.editTextImapServer?.text.toString(),
      imapPort = imapPort,
      imapOpt = (binding?.spinnerImapSecurityType?.selectedItem as SecurityType).opt,
      smtpServer = binding?.editTextSmtpServer?.text.toString(),
      smtpPort = smtpPort,
      smtpOpt = (binding?.spinnerSmtpSecyrityType?.selectedItem as SecurityType).opt,
      hasCustomSignInForSmtp = binding?.checkBoxRequireSignInForSmtp?.isChecked ?: false,
      smtpSigInUsername = binding?.editTextSmtpUsername?.text.toString(),
      smtpSignInPassword = binding?.editTextSmtpPassword?.text.toString()
    )
  }

  private fun showLessSecurityWarning() {
    showSnackbar(
      view, getString(R.string.less_secure_login_is_not_allowed),
      getString(android.R.string.ok), Snackbar.LENGTH_LONG
    ) {
      navController?.navigateUp()
    }
  }

  /**
   * Do a lot of checks to validate an outgoing message info.
   *
   * @return <tt>Boolean</tt> true if all information is correct, false otherwise.
   */
  private fun isDataCorrect(): Boolean {
    when {
      binding?.editTextEmail?.text.isNullOrEmpty() -> {
        showInfoSnackbar(
          binding?.editTextEmail,
          getString(R.string.text_must_not_be_empty, getString(R.string.e_mail))
        )
        binding?.editTextEmail?.requestFocus()
      }

      GeneralUtil.isEmailValid(binding?.editTextEmail?.text) -> {
        if (binding?.checkBoxAdvancedMode?.isChecked == false) {
          when {
            binding?.editTextPassword?.text.isNullOrEmpty() -> {
              showInfoSnackbar(
                binding?.editTextPassword, getString(
                  R.string.text_must_not_be_empty,
                  getString(R.string.password)
                )
              )
              binding?.editTextPassword?.requestFocus()
            }

            else -> return true
          }
        }

        when {
          binding?.editTextUserName?.text.isNullOrEmpty() -> {
            showInfoSnackbar(
              binding?.editTextUserName, getString(
                R.string.text_must_not_be_empty,
                getString(R.string.username)
              )
            )
            binding?.editTextUserName?.requestFocus()
          }

          binding?.editTextPassword?.text.isNullOrEmpty() -> {
            showInfoSnackbar(
              binding?.editTextPassword, getString(
                R.string.text_must_not_be_empty,
                getString(R.string.password)
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
      }

      else -> {
        showInfoSnackbar(binding?.editTextEmail, getString(R.string.error_email_is_not_valid))
        binding?.editTextEmail?.requestFocus()
      }
    }

    return false
  }

  private fun tryToConnect() {
    if (isDataCorrect()) {
      view?.hideKeyboard()
      authCreds = generateAuthCreds()
      authCreds?.let { authCredentials ->
        val account = AccountEntity(authCredentials)
        navController?.navigate(
          AddOtherAccountFragmentDirections
            .actionAddOtherAccountFragmentToAuthorizeAndSearchBackupsFragment(account)
        )
      }
    }
  }

  private fun applyRecommendSettings(): Boolean {
    val recommendAuthCredentials = EmailProviderSettingsHelper.getBaseSettings(
      binding?.editTextEmail?.text.toString(), binding?.editTextPassword?.text.toString()
    )

    if (recommendAuthCredentials != null) {
      updateView(recommendAuthCredentials, false)
      return true
    }
    return false
  }

  private fun storeAccountInfoToAccountManager() {
    getTempAccount().let { accountEntity ->
      val accountManager = AccountManager.get(requireContext())
      val account = Account(
        accountEntity.email.lowercase(),
        FlowcryptAccountAuthenticator.ACCOUNT_TYPE
      )
      accountManager.addAccountExplicitly(account, null, Bundle().apply {
        with(authCreds?.authTokenInfo) {
          putString(FlowcryptAccountAuthenticator.KEY_ACCOUNT_EMAIL, this?.email)
          putString(FlowcryptAccountAuthenticator.KEY_REFRESH_TOKEN, this?.refreshToken)
          putString(FlowcryptAccountAuthenticator.KEY_EXPIRES_AT, this?.expiresAt?.toString())
        }
      })
    }
  }

  private fun handleUnlockedKeys(keys: List<PgpKeyDetails>) {
    if (keys.isEmpty()) {
      showContent()
      showInfoSnackbar(msgText = getString(R.string.error_no_keys))
    } else {
      importCandidates.clear()
      importCandidates.addAll(keys)
      accountViewModel.addNewAccount(getTempAccount())
    }
  }

  companion object {
    private const val REQUEST_CODE_RETRY_SETTINGS_CHECKING = 12
    private const val REQUEST_CODE_FETCH_MICROSOFT_OPENID_CONFIGURATION = 13L
  }
}
