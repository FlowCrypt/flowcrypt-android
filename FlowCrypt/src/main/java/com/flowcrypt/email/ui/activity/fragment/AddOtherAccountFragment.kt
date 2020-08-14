/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
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
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.hideKeyboard
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showTwoWayDialog
import com.flowcrypt.email.jetpack.viewmodel.OAuth2AuthCredentialsViewModel
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.service.EmailSyncService
import com.flowcrypt.email.ui.activity.CheckKeysActivity
import com.flowcrypt.email.ui.activity.CreateOrImportKeyActivity
import com.flowcrypt.email.ui.activity.EmailManagerActivity
import com.flowcrypt.email.ui.activity.SignInActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseSingInFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity
import com.flowcrypt.email.ui.widget.inputfilters.InputFilters
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.SharedPreferencesHelper
import com.flowcrypt.email.util.exception.AccountAlreadyAddedException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.SavePrivateKeyToDatabaseException
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.sun.mail.util.MailConnectException
import kotlinx.android.synthetic.main.fragment_screenshot_editor.*
import kotlinx.coroutines.launch
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import java.net.SocketTimeoutException
import java.util.*
import java.util.regex.Pattern
import javax.mail.AuthenticationFailedException
import kotlin.collections.ArrayList

/**
 * @author Denis Bondarenko
 *         Date: 7/20/20
 *         Time: 3:39 PM
 *         E-mail: DenBond7@gmail.com
 */
class AddOtherAccountFragment : BaseSingInFragment(), ProgressBehaviour,
    AdapterView.OnItemSelectedListener {

  private var buttonSignInWithOutlook: Button? = null
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
  private var checkBoxAdvancedMode: CheckBox? = null
  private var checkBoxRequireSignInForSmtp: CheckBox? = null
  private var authCreds: AuthCredentials? = null

  private var isImapSpinnerRestored: Boolean = false
  private var isSmtpSpinnerRestored: Boolean = false
  private var authRequest: AuthorizationRequest? = null

  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()
  private val oAuth2AuthCredentialsViewModel: OAuth2AuthCredentialsViewModel by viewModels()

  private val digitsTextWatcher: TextWatcher = object : TextWatcher {
    override fun afterTextChanged(s: Editable?) {
      s?.let {
        if (!Pattern.compile("\\d+").matcher(it).matches()) {
          it.clear()
        }
      }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

    }
  }

  override val progressView: View?
    get() = view?.findViewById(R.id.progress)
  override val contentView: View?
    get() = view?.findViewById(R.id.layoutContent)
  override val statusView: View?
    get() = view?.findViewById(R.id.status)

  override val contentResourceId: Int = R.layout.fragment_add_other_account

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    savedInstanceState?.let { restoreAuthRequest(it) }

    subscribeToCheckAccountSettings()
    subscribeToAuthorizeAndSearchBackups()

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

    setupPrivateKeysViewModel()
  }

  override fun onPause() {
    super.onPause()
    saveTempCreds()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putString(KEY_AUTH_REQUEST, authRequest?.jsonSerializeString())
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_ADD_NEW_ACCOUNT -> when (resultCode) {
        Activity.RESULT_OK -> if (existedAccounts.isEmpty()) runEmailManagerActivity() else returnResultOk()

        CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT -> {
          parentFragmentManager.popBackStack()
        }

        CreateOrImportKeyActivity.RESULT_CODE_HANDLE_RESOLVED_KEYS -> handleResultFromCheckKeysActivity(resultCode, data)
      }

      REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_EMAIL -> handleResultFromCheckKeysActivity(resultCode, data)

      REQUEST_CODE_RETRY_SETTINGS_CHECKING -> {
        when (resultCode) {
          TwoWayDialogFragment.RESULT_OK -> {
            tryToConnect()
          }
        }
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
    when (parent?.id) {
      R.id.spinnerImapSecurityType -> {
        val (_, _, defImapPort) = parent.adapter.getItem(position) as SecurityType
        if (isImapSpinnerRestored) {
          editTextImapPort?.setText(defImapPort.toString())
        } else {
          isImapSpinnerRestored = true
        }
      }

      R.id.spinnerSmtpSecyrityType -> {
        val (_, _, _, defSmtpPort) = parent.adapter.getItem(position) as SecurityType
        if (isSmtpSpinnerRestored) {
          editTextSmtpPort?.setText(defSmtpPort.toString())
        } else {
          isSmtpSpinnerRestored = true
        }
      }
    }
  }

  override fun onNothingSelected(parent: AdapterView<*>) {

  }

  override fun runEmailManagerActivity() {
    lifecycleScope.launch {
      val authCreds = generateAuthCreds()
      val accountEntity = AccountEntity(
          if (authCreds.useOAuth2) {
            authCreds.copy(password = "", smtpSignInPassword = null)
          } else {
            authCreds
          })
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(requireContext())
      roomDatabase.accountDao().addAccountSuspend(accountEntity)

      if (authCreds.useOAuth2) {
        storeAccountInfoToAccountManager(accountEntity, authCreds)
      }

      EmailSyncService.startEmailSyncService(requireContext())

      val addedAccount = roomDatabase.accountDao().getAccountSuspend(accountEntity.email)
      if (addedAccount != null) {
        EmailManagerActivity.runEmailManagerActivity(requireContext())
        activity?.finish()
      } else {
        Toast.makeText(requireContext(), R.string.error_occurred_try_again_later, Toast.LENGTH_SHORT).show()
      }
    }
  }

  override fun returnResultOk() {
    lifecycleScope.launch {
      try {
        val authCreds = generateAuthCreds()
        val accountEntity = AccountEntity(
            if (authCreds.useOAuth2) {
              authCreds.copy(password = "", smtpSignInPassword = null)
            } else {
              authCreds
            })
        val roomDatabase = FlowCryptRoomDatabase.getDatabase(requireContext())
        roomDatabase.accountDao().addAccountSuspend(accountEntity)
        val addedAccount = roomDatabase.accountDao().getAccountSuspend(accountEntity.email)

        if (authCreds.useOAuth2) {
          storeAccountInfoToAccountManager(accountEntity, authCreds)
        }

        val intent = Intent()
        intent.putExtra(SignInActivity.KEY_EXTRA_NEW_ACCOUNT, addedAccount)
        activity?.setResult(Activity.RESULT_OK, intent)
        activity?.finish()
      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        Toast.makeText(requireContext(), e.message
            ?: getString(R.string.error_occurred_during_adding_new_account), Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun storeAccountInfoToAccountManager(accountEntity: AccountEntity, authCreds: AuthCredentials) {
    val accountManager = AccountManager.get(requireContext())
    val account = Account(accountEntity.email.toLowerCase(Locale.US), FlowcryptAccountAuthenticator.ACCOUNT_TYPE)
    accountManager.addAccountExplicitly(account, null, Bundle().apply {
      with(authCreds.authTokenInfo) {
        putString(FlowcryptAccountAuthenticator.KEY_ACCOUNT_EMAIL, this?.email)
        putString(FlowcryptAccountAuthenticator.KEY_REFRESH_TOKEN, this?.refreshToken)
        putString(FlowcryptAccountAuthenticator.KEY_EXPIRES_AT, this?.expiresAt?.toString())
      }
    })
  }

  fun handleOAuth2Intent(intent: Intent?) {
    intent?.let {
      val schema = intent.data?.scheme
      if (schema !in OAuth2Helper.SUPPORTED_SCHEMAS) {
        return
      }

      val authResponse = AuthorizationResponse.fromIntent(intent)
      val authException = AuthorizationException.fromIntent(intent)
      if (authResponse != null) {
        val code = authResponse.authorizationCode
        if (code != null) {
          getOAuthToken(schema, code)
        } else {
          showInfoDialog(
              dialogTitle = "",
              dialogMsg = getString(R.string.could_not_verify_response),
              useLinkify = true
          )
        }
      } else if (authException != null) {
        showInfoDialog(
            dialogTitle = getString(R.string.error_with_value, authException.error),
            dialogMsg = authException.errorDescription,
            useLinkify = true
        )
      } else {
        showInfoDialog(
            dialogTitle = getString(R.string.oauth_error),
            dialogMsg = getString(R.string.could_not_verify_response),
            useLinkify = true
        )
      }
    }
  }

  private fun initViews(view: View) {
    editTextEmail = view.findViewById(R.id.editTextEmail)
    editTextUserName = view.findViewById(R.id.editTextUserName)
    editTextPassword = view.findViewById(R.id.editTextPassword)
    editTextImapServer = view.findViewById(R.id.editTextImapServer)
    editTextImapPort = view.findViewById(R.id.editTextImapPort)
    editTextImapPort?.addTextChangedListener(digitsTextWatcher)
    editTextSmtpServer = view.findViewById(R.id.editTextSmtpServer)
    editTextSmtpPort = view.findViewById(R.id.editTextSmtpPort)
    editTextSmtpPort?.addTextChangedListener(digitsTextWatcher)
    editTextSmtpUsername = view.findViewById(R.id.editTextSmtpUsername)
    editTextSmtpPassword = view.findViewById(R.id.editTextSmtpPassword)

    editTextEmail?.filters = arrayOf<InputFilter>(InputFilters.NoCaps())
    editTextImapServer?.filters = arrayOf<InputFilter>(InputFilters.NoCaps())
    editTextSmtpServer?.filters = arrayOf<InputFilter>(InputFilters.NoCaps())

    editTextPassword?.setOnEditorActionListener { _, actionId, _ ->
      return@setOnEditorActionListener when (actionId) {
        EditorInfo.IME_ACTION_DONE -> {
          tryToConnect()
          true
        }
        else -> false
      }
    }

    editTextEmail?.doAfterTextChanged {
      if (GeneralUtil.isEmailValid(it)) {
        if (checkBoxAdvancedMode?.isChecked == false) {
          if (applyRecommendSettings()) return@doAfterTextChanged
        }

        val email = it.toString()
        val mainDomain = email.substring(email.indexOf('@') + 1)
        editTextImapServer?.setText(getString(R.string.template_imap_server, mainDomain))
        editTextSmtpServer?.setText(getString(R.string.template_smtp_server, mainDomain))
        editTextUserName?.setText(email)
        editTextSmtpUsername?.setText(email)
      }
    }

    editTextPassword?.doAfterTextChanged {
      if (checkBoxAdvancedMode?.isChecked == false) {
        val recommendAuthCredentials = EmailProviderSettingsHelper.getBaseSettings(
            editTextEmail?.text.toString(), editTextPassword?.text.toString())

        editTextSmtpPassword?.setText(recommendAuthCredentials?.smtpSignInPassword)
      }
    }

    checkBoxAdvancedMode = view.findViewById(R.id.checkBoxAdvancedMode)
    checkBoxRequireSignInForSmtp = view.findViewById(R.id.checkBoxRequireSignInForSmtp)
    checkBoxRequireSignInForSmtp?.setOnCheckedChangeListener { _, isChecked ->
      if (checkBoxAdvancedMode?.isChecked == true) {
        view.findViewById<View>(R.id.groupRequireSignInForSmtp).visibility = if (isChecked) View.VISIBLE else View.GONE
      }
    }

    checkBoxAdvancedMode?.setOnCheckedChangeListener { buttonView, isChecked ->
      buttonView.hideKeyboard()
      view.findViewById<View>(R.id.groupAdvancedSettings)?.visibility = if (isChecked) View.VISIBLE else View.GONE
      if ((checkBoxRequireSignInForSmtp?.isChecked == true) && isChecked) {
        view.findViewById<View>(R.id.groupRequireSignInForSmtp)?.visibility = View.VISIBLE
      } else {
        view.findViewById<View>(R.id.groupRequireSignInForSmtp)?.visibility = View.GONE
      }

      if (!isChecked) {
        applyRecommendSettings()
      }
    }

    spinnerImapSecurityType = view.findViewById(R.id.spinnerImapSecurityType)
    spinnerSmtpSecurityType = view.findViewById(R.id.spinnerSmtpSecyrityType)

    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item,
        SecurityType.generateSecurityTypes(requireContext()))

    spinnerImapSecurityType?.adapter = adapter
    spinnerSmtpSecurityType?.adapter = adapter

    spinnerImapSecurityType?.onItemSelectedListener = this
    spinnerSmtpSecurityType?.onItemSelectedListener = this

    view.findViewById<View>(R.id.buttonTryToConnect)?.setOnClickListener {
      tryToConnect()
    }

    view.findViewById<View>(R.id.buttonHelp)?.setOnClickListener {
      FeedbackActivity.show(requireActivity())
    }

    buttonSignInWithOutlook = view.findViewById(R.id.buttonSignInWithOutlook)
    buttonSignInWithOutlook?.setOnClickListener {
      it.isEnabled = false
      getAuthorizationRequestForProvider(
          requestCode = REQUEST_CODE_FETCH_MICROSOFT_OPENID_CONFIGURATION,
          provider = OAuth2Helper.Provider.MICROSOFT)
    }
  }

  /**
   * Update the current views if [AuthCredentials] not null.l
   */
  private fun updateView(authCreds: AuthCredentials?, updateEmail: Boolean = true) {
    authCreds?.let { nonNullAuthCreds ->
      if (updateEmail) {
        editTextEmail?.setText(nonNullAuthCreds.email)
      }
      editTextUserName?.setText(nonNullAuthCreds.username)
      editTextImapServer?.setText(nonNullAuthCreds.imapServer)
      editTextImapPort?.setText(nonNullAuthCreds.imapPort.toString())
      editTextSmtpServer?.setText(nonNullAuthCreds.smtpServer)
      editTextSmtpPort?.setText(nonNullAuthCreds.smtpPort.toString())
      checkBoxRequireSignInForSmtp?.isChecked = nonNullAuthCreds.hasCustomSignInForSmtp
      editTextSmtpUsername?.setText(nonNullAuthCreds.smtpSigInUsername)
      editTextSmtpPassword?.setText(nonNullAuthCreds.smtpSignInPassword)

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

  @Suppress("UNCHECKED_CAST")
  private fun subscribeToCheckAccountSettings() {
    setFragmentResultListener(AuthorizeAndSearchBackupsFragment.REQUEST_KEY_CHECK_ACCOUNT_SETTINGS) { _, bundle ->
      val result: Result<*>? = bundle.getSerializable(AuthorizeAndSearchBackupsFragment.KEY_CHECK_ACCOUNT_SETTINGS_RESULT) as? Result<*>

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
                val isGmailImapServer = editTextImapServer?.text.toString()
                    .equals(GmailConstants.GMAIL_IMAP_SERVER, ignoreCase = true)
                val isMsgEmpty = TextUtils.isEmpty(original.message)
                val hasAlert = original.message?.startsWith(GmailConstants
                    .GMAIL_ALERT_MESSAGE_WHEN_LESS_SECURE_NOT_ALLOWED)
                if (isGmailImapServer && !isMsgEmpty && hasAlert == true) {
                  showLessSecurityWarning()
                  return@setFragmentResultListener
                }
              } else if (original is MailConnectException || original is SocketTimeoutException) {
                title = getString(R.string.network_error)
              }
            } else if (exception is AccountAlreadyAddedException) {
              showInfoSnackbar(rootView, exception.message, Snackbar.LENGTH_LONG)
              return@setFragmentResultListener
            }

            val faqUrl = EmailProviderSettingsHelper.getBaseSettings(
                editTextEmail?.text.toString(), editTextPassword?.text.toString())?.faqUrl

            val dialogMsg = if (checkBoxAdvancedMode?.isChecked == false) {
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
                useLinkify = true)
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
      val result: Result<*>? = bundle.getSerializable(AuthorizeAndSearchBackupsFragment.KEY_PRIVATE_KEY_BACKUPS_RESULT) as? Result<*>

      if (result != null) {
        when (result.status) {
          Result.Status.SUCCESS -> {
            dismissCurrentSnackBar()

            val keyDetailsList = result.data as ArrayList<NodeKeyDetails>?
            if (keyDetailsList?.isEmpty() == true) {
              authCreds?.let { authCredentials ->
                val account = AccountEntity(authCredentials)
                startActivityForResult(CreateOrImportKeyActivity.newIntent(requireContext(), account, true),
                    REQUEST_CODE_ADD_NEW_ACCOUNT)
                showContent()
              }
            } else {
              val subTitle = resources.getQuantityString(
                  R.plurals.found_backup_of_your_account_key,
                  keyDetailsList?.size ?: 0,
                  keyDetailsList?.size ?: 0
              )

              val intent = CheckKeysActivity.newIntent(
                  context = requireContext(),
                  privateKeys = keyDetailsList ?: ArrayList(),
                  type = KeyDetails.Type.EMAIL,
                  subTitle = subTitle,
                  positiveBtnTitle = getString(R.string.continue_),
                  negativeBtnTitle = getString(R.string.use_another_account)
              )
              startActivityForResult(intent, REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_EMAIL)
            }
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            showContent()
            showInfoDialog(
                dialogMsg = result.exception?.message
                    ?: result.exception?.javaClass?.simpleName
                    ?: getString(R.string.could_not_load_private_keys))
          }

          else -> {

          }
        }
      }
    }
  }

  private fun setupPrivateKeysViewModel() {
    privateKeysViewModel.savePrivateKeysLiveData.observe(viewLifecycleOwner, Observer {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            showProgress(progressMsg = getString(R.string.saving_prv_keys))
          }

          Result.Status.SUCCESS -> {
            if (existedAccounts.isEmpty()) runEmailManagerActivity() else returnResultOk()
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            showContent()
            val e = it.exception
            if (e is SavePrivateKeyToDatabaseException) {
              showSnackbar(rootView, e.message ?: e.javaClass.simpleName,
                  getString(R.string.retry), Snackbar.LENGTH_INDEFINITE, View.OnClickListener {
                privateKeysViewModel.encryptAndSaveKeysToDatabase(e.keys, KeyDetails.Type.EMAIL)
              })
            } else {
              showInfoSnackbar(rootView, e?.message ?: e?.javaClass?.simpleName
              ?: getString(R.string.unknown_error))
            }
          }
        }
      }
    })
  }

  private fun getOAuthToken(schema: String?, code: String) {
    authRequest?.let { request ->
      when (schema) {
        OAuth2Helper.MICROSOFT_OAUTH2_SCHEMA -> {
          observeMicrosoftOAuth2TokenLiveData()
          oAuth2AuthCredentialsViewModel.getMicrosoftOAuth2Token(authorizeCode = code, authRequest = request)
        }
      }
    }
  }

  private fun observeMicrosoftOAuth2TokenLiveData() {
    oAuth2AuthCredentialsViewModel.microsoftOAuth2TokenLiveData.observe(viewLifecycleOwner, Observer {
      when (it.status) {
        Result.Status.LOADING -> {
          showProgress(progressMsg = getString(R.string.loading_account_details))
        }

        Result.Status.SUCCESS -> {
          oAuth2AuthCredentialsViewModel.microsoftOAuth2TokenLiveData.removeObservers(viewLifecycleOwner)
          it.data?.let { authCredentials ->
            authCreds = authCredentials
            val account = AccountEntity(authCredentials).copy(password = authCredentials
                .peekPassword(), smtpPassword = authCredentials.peekSmtpPassword())
            val nextFrag = AuthorizeAndSearchBackupsFragment.newInstance(account)
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragmentContainerView, nextFrag, AuthorizeAndSearchBackupsFragment::class.java.simpleName)
                ?.addToBackStack(null)
                ?.commit()
          }
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          oAuth2AuthCredentialsViewModel.microsoftOAuth2TokenLiveData.removeObservers(viewLifecycleOwner)
          showContent()
          showInfoDialog(
              dialogMsg = it.exception?.message ?: it.exception?.javaClass?.simpleName
              ?: "Couldn't fetch token")
        }
      }
    })
  }

  private fun getAuthorizationRequestForProvider(requestCode: Long, provider: OAuth2Helper.Provider) {
    oAuth2AuthCredentialsViewModel.authorizationRequestLiveData.observe(viewLifecycleOwner, Observer {
      when (it.status) {
        Result.Status.LOADING -> {
          showProgress(progressMsg = getString(R.string.loading_oauth_server_configuration))
        }

        Result.Status.SUCCESS -> {
          oAuth2AuthCredentialsViewModel.authorizationRequestLiveData.removeObservers(viewLifecycleOwner)
          buttonSignInWithOutlook?.isEnabled = true
          showContent()

          it.data?.let { authorizationRequest ->
            authRequest = authorizationRequest
            authRequest?.let { request ->
              AuthorizationService(requireContext())
                  .performAuthorizationRequest(
                      request,
                      PendingIntent.getActivity(requireContext(), 0, Intent(requireContext(), SignInActivity::class.java), 0))
            }
          }
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          oAuth2AuthCredentialsViewModel.authorizationRequestLiveData.removeObservers(viewLifecycleOwner)
          buttonSignInWithOutlook?.isEnabled = true
          showContent()
          showInfoDialog(
              dialogMsg = it.exception?.message ?: it.exception?.javaClass?.simpleName
              ?: "Couldn't load the server configuration")
        }
      }
    })

    oAuth2AuthCredentialsViewModel.getAuthorizationRequestForProvider(requestCode, provider)
  }

  /**
   * Retrieve a temp [AuthCredentials] from the shared preferences.
   */
  private fun getTempAuthCreds(): AuthCredentials? {
    val authCredsJson =
        SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(requireContext()),
            Constants.PREF_KEY_TEMP_LAST_AUTH_CREDENTIALS, "")

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
    SharedPreferencesHelper.setString(PreferenceManager.getDefaultSharedPreferences(requireContext()),
        Constants.PREF_KEY_TEMP_LAST_AUTH_CREDENTIALS, Gson().toJson(authCreds))
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

  private fun showLessSecurityWarning() {
    showSnackbar(rootView, getString(R.string.less_secure_login_is_not_allowed),
        getString(android.R.string.ok), Snackbar.LENGTH_LONG, View.OnClickListener {
      parentFragmentManager.popBackStack()
    })
  }

  /**
   * Do a lot of checks to validate an outgoing message info.
   *
   * @return <tt>Boolean</tt> true if all information is correct, false otherwise.
   */
  private fun isDataCorrect(): Boolean {
    when {
      editTextEmail?.text.isNullOrEmpty() -> {
        showInfoSnackbar(editTextEmail, getString(R.string.text_must_not_be_empty, getString(R.string.e_mail)))
        editTextEmail?.requestFocus()
      }

      GeneralUtil.isEmailValid(editTextEmail?.text) -> {
        if (checkBoxAdvancedMode?.isChecked == false) {
          when {
            editTextPassword?.text.isNullOrEmpty() -> {
              showInfoSnackbar(editTextPassword, getString(R.string.text_must_not_be_empty,
                  getString(R.string.password)))
              editTextPassword?.requestFocus()
            }

            else -> return true
          }
        }

        when {
          editTextUserName?.text.isNullOrEmpty() -> {
            showInfoSnackbar(editTextUserName, getString(R.string.text_must_not_be_empty,
                getString(R.string.username)))
            editTextUserName?.requestFocus()
          }

          editTextPassword?.text.isNullOrEmpty() -> {
            showInfoSnackbar(editTextPassword, getString(R.string.text_must_not_be_empty,
                getString(R.string.password)))
            editTextPassword?.requestFocus()
          }

          editTextImapServer?.text.isNullOrEmpty() -> {
            showInfoSnackbar(editTextImapServer, getString(R.string.text_must_not_be_empty,
                getString(R.string.imap_server)))
            editTextImapServer?.requestFocus()
          }

          editTextImapPort?.text.isNullOrEmpty() -> {
            showInfoSnackbar(editTextImapPort, getString(R.string.text_must_not_be_empty,
                getString(R.string.imap_port)))
            editTextImapPort?.requestFocus()
          }

          editTextSmtpServer?.text.isNullOrEmpty() -> {
            showInfoSnackbar(editTextSmtpServer, getString(R.string.text_must_not_be_empty,
                getString(R.string.smtp_server)))
            editTextSmtpServer?.requestFocus()
          }

          editTextSmtpPort?.text.isNullOrEmpty() -> {
            showInfoSnackbar(editTextSmtpPort, getString(R.string.text_must_not_be_empty,
                getString(R.string.smtp_port)))
            editTextSmtpPort?.requestFocus()
          }

          checkBoxRequireSignInForSmtp?.isChecked == true -> when {
            editTextSmtpUsername?.text.isNullOrEmpty() -> {
              showInfoSnackbar(editTextSmtpUsername, getString(R.string.text_must_not_be_empty,
                  getString(R.string.smtp_username)))
              editTextSmtpUsername?.requestFocus()
            }

            editTextSmtpPassword?.text.isNullOrEmpty() -> {
              showInfoSnackbar(editTextSmtpPassword, getString(R.string.text_must_not_be_empty,
                  getString(R.string.smtp_password)))
              editTextSmtpPassword?.requestFocus()
            }
            else -> return true
          }

          else -> return true
        }
      }

      else -> {
        showInfoSnackbar(editTextEmail, getString(R.string.error_email_is_not_valid))
        editTextEmail?.requestFocus()
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
        val nextFrag = AuthorizeAndSearchBackupsFragment.newInstance(account)
        activity?.supportFragmentManager?.beginTransaction()
            ?.replace(R.id.fragmentContainerView, nextFrag, AuthorizeAndSearchBackupsFragment::class.java.simpleName)
            ?.addToBackStack(null)
            ?.commit()
      }
    }
  }

  private fun handleResultFromCheckKeysActivity(resultCode: Int, data: Intent?) {
    when (resultCode) {
      Activity.RESULT_OK, CheckKeysActivity.RESULT_SKIP_REMAINING_KEYS -> {
        val keys: List<NodeKeyDetails>? = data?.getParcelableArrayListExtra(
            CheckKeysActivity.KEY_EXTRA_UNLOCKED_PRIVATE_KEYS)

        if (keys.isNullOrEmpty()) {
          showContent()
          showInfoSnackbar(msgText = getString(R.string.error_no_keys))
        } else {
          privateKeysViewModel.encryptAndSaveKeysToDatabase(keys, KeyDetails.Type.EMAIL)
        }
      }

      CheckKeysActivity.RESULT_USE_EXISTING_KEYS -> {
        if (existedAccounts.isEmpty()) runEmailManagerActivity() else returnResultOk()
      }

      CheckKeysActivity.RESULT_NO_NEW_KEYS -> {
        Toast.makeText(requireContext(), getString(R.string.key_already_imported_finishing_setup), Toast
            .LENGTH_SHORT).show()
        if (existedAccounts.isEmpty()) runEmailManagerActivity() else returnResultOk()
      }

      Activity.RESULT_CANCELED -> showContent()

      CheckKeysActivity.RESULT_NEGATIVE -> {
        parentFragmentManager.popBackStack()
      }
    }
  }

  private fun applyRecommendSettings(): Boolean {
    val recommendAuthCredentials = EmailProviderSettingsHelper.getBaseSettings(
        editTextEmail?.text.toString(), editTextPassword?.text.toString())

    if (recommendAuthCredentials != null) {
      updateView(recommendAuthCredentials, false)
      return true
    }
    return false
  }

  private fun restoreAuthRequest(state: Bundle) {
    val serializedAuthorizationRequest = state.getString(KEY_AUTH_REQUEST)
    serializedAuthorizationRequest?.let { jsonString ->
      try {
        authRequest = AuthorizationRequest.jsonDeserialize(jsonString)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  companion object {
    private val KEY_AUTH_REQUEST =
        GeneralUtil.generateUniqueExtraKey("KEY_UUID_FOR_OAUTH", AddOtherAccountFragment::class.java)

    private const val REQUEST_CODE_ADD_NEW_ACCOUNT = 10
    private const val REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_EMAIL = 11
    private const val REQUEST_CODE_RETRY_SETTINGS_CHECKING = 12
    private const val REQUEST_CODE_FETCH_MICROSOFT_OPENID_CONFIGURATION = 13L

    fun newInstance(): AddOtherAccountFragment {
      return AddOtherAccountFragment()
    }
  }
}