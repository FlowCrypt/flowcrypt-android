/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailConstants
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.hideKeyboard
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showTwoWayDialog
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
import java.net.SocketTimeoutException
import java.util.regex.Pattern
import javax.mail.AuthenticationFailedException

/**
 * @author Denis Bondarenko
 *         Date: 7/20/20
 *         Time: 3:39 PM
 *         E-mail: DenBond7@gmail.com
 */
class AddOtherAccountFragment : BaseSingInFragment(), ProgressBehaviour,
    AdapterView.OnItemSelectedListener {

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

  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()

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
    subscribeToCheckAccountSettings()
    subscribeToAuthorizeAndSearchBackups()

    this.authCreds = getTempAuthCreds()

    if (authCreds == null) {
      isImapSpinnerRestored = true
      isSmtpSpinnerRestored = true
    }
  }

  override fun onPause() {
    super.onPause()
    saveTempCreds()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)
    updateView()

    setupPrivateKeysViewModel()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_ADD_NEW_ACCOUNT -> when (resultCode) {
        Activity.RESULT_OK -> if (existedAccounts.isEmpty()) runEmailManagerActivity() else returnResultOk()

        CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT -> {
          //return result --> need to use another account
          activity?.finish()
        }
      }

      REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_EMAIL -> when (resultCode) {
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
          //setResult(CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT, data)
          //activity?.finish()
        }
      }

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
      val accountEntity = AccountEntity(generateAuthCreds())
      val roomDatabase = FlowCryptRoomDatabase.getDatabase(requireContext())
      roomDatabase.accountDao().addAccountSuspend(accountEntity)
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
        val newAccount = AccountEntity(generateAuthCreds())
        val roomDatabase = FlowCryptRoomDatabase.getDatabase(requireContext())
        roomDatabase.accountDao().addAccountSuspend(newAccount)
        val addedAccount = roomDatabase.accountDao().getAccountSuspend(newAccount.email)

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

    editTextEmail?.addTextChangedListener(object : TextWatcher {
      override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

      override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

      override fun afterTextChanged(editable: Editable) {
        if (GeneralUtil.isEmailValid(editable)) {
          val email = editable.toString()
          val mainDomain = email.substring(email.indexOf('@') + 1)
          editTextImapServer?.setText(getString(R.string.template_imap_server, mainDomain))
          editTextSmtpServer?.setText(getString(R.string.template_smtp_server, mainDomain))
          editTextUserName?.setText(email)
          editTextSmtpUsername?.setText(email)
        }
      }
    })

    val checkBoxAdvancedMode = view.findViewById<CheckBox>(R.id.checkBoxAdvancedMode)
    checkBoxRequireSignInForSmtp = view.findViewById(R.id.checkBoxRequireSignInForSmtp)
    checkBoxRequireSignInForSmtp?.setOnCheckedChangeListener { _, isChecked ->
      if (checkBoxAdvancedMode?.isChecked == true) {
        view.findViewById<View>(R.id.groupRequireSignInForSmtp).visibility = if (isChecked) View.VISIBLE else View.GONE
      }
    }

    checkBoxAdvancedMode.setOnCheckedChangeListener { _, isChecked ->
      view.findViewById<View>(R.id.groupAdvancedSettings)?.visibility = if (isChecked) View.VISIBLE else View.GONE
      if ((checkBoxRequireSignInForSmtp?.isChecked == true) && isChecked) {
        view.findViewById<View>(R.id.groupRequireSignInForSmtp)?.visibility = View.VISIBLE
      } else {
        view.findViewById<View>(R.id.groupRequireSignInForSmtp)?.visibility = View.GONE
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
      if (isDataCorrect()) {
        it.hideKeyboard()
        authCreds = generateAuthCreds()
        tryToConnect()
      }
    }
  }

  /**
   * Update the current views if [AuthCredentials] not null.l
   */
  private fun updateView() {
    authCreds?.let { nonNullAuthCreds ->
      editTextEmail?.setText(nonNullAuthCreds.email)
      editTextUserName?.setText(nonNullAuthCreds.username)
      editTextImapServer?.setText(nonNullAuthCreds.imapServer)
      editTextImapPort?.setText(nonNullAuthCreds.imapPort.toString())
      editTextSmtpServer?.setText(nonNullAuthCreds.smtpServer)
      editTextSmtpPort?.setText(nonNullAuthCreds.smtpPort.toString())
      checkBoxRequireSignInForSmtp?.isChecked = nonNullAuthCreds.hasCustomSignInForSmtp
      editTextSmtpUsername?.setText(nonNullAuthCreds.smtpSigInUsername)

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

            showTwoWayDialog(
                requestCode = REQUEST_CODE_RETRY_SETTINGS_CHECKING,
                dialogTitle = title,
                dialogMsg = msg,
                positiveButtonTitle = getString(R.string.retry),
                negativeButtonTitle = getString(R.string.cancel),
                isCancelable = true)
          }

          else -> {

          }
        }
      } else {
        //show error
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
                val account = AccountEntity(authCredentials, null, null)
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
      } else {
        //show error
      }
    }
  }

  private fun setupPrivateKeysViewModel() {
    privateKeysViewModel.savePrivateKeysLiveData.observe(viewLifecycleOwner, Observer {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            showProgress()
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
    authCreds = generateAuthCreds()
    val gson = Gson()
    authCreds?.password = ""
    authCreds?.smtpSignInPassword = null
    SharedPreferencesHelper.setString(PreferenceManager.getDefaultSharedPreferences(requireContext()),
        Constants.PREF_KEY_TEMP_LAST_AUTH_CREDENTIALS, gson.toJson(authCreds))
  }

  /**
   * Generate the [AuthCredentials] using user input.
   *
   * @return [AuthCredentials].
   */
  private fun generateAuthCreds(): AuthCredentials {
    val imapPort = if (TextUtils.isEmpty(editTextImapPort?.text))
      JavaEmailConstants.DEFAULT_IMAP_PORT
    else
      Integer.parseInt(editTextImapPort?.text.toString())

    val smtpPort = if (TextUtils.isEmpty(editTextSmtpPort?.text))
      JavaEmailConstants.DEFAULT_SMTP_PORT
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
      //setResult(RESULT_CODE_CONTINUE_WITH_GMAIL)
      //finish()
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
    authCreds?.let { authCredentials ->
      val account = AccountEntity(authCredentials)
      val nextFrag = AuthorizeAndSearchBackupsFragment.newInstance(account)
      activity?.supportFragmentManager?.beginTransaction()
          ?.replace(R.id.fragmentContainerView, nextFrag, AuthorizeAndSearchBackupsFragment::class.java.simpleName)
          ?.addToBackStack(null)
          ?.commit()
    }
  }

  companion object {
    private const val REQUEST_CODE_ADD_NEW_ACCOUNT = 10
    private const val REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_EMAIL = 11
    private const val REQUEST_CODE_RETRY_SETTINGS_CHECKING = 12

    fun newInstance(): AddOtherAccountFragment {
      return AddOtherAccountFragment()
    }
  }
}