/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

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
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.Spinner
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.preference.PreferenceManager
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.email.gmail.GmailConstants
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.dao.source.AccountDao
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.ui.loader.CheckEmailSettingsAsyncTaskLoader
import com.flowcrypt.email.ui.loader.LoadPrivateKeysFromMailAsyncTaskLoader
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.SharedPreferencesHelper
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.common.util.CollectionUtils
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.sun.mail.util.MailConnectException
import java.net.SocketTimeoutException
import java.util.*
import javax.mail.AuthenticationFailedException

/**
 * This activity describes a logic of adding a new account of other email providers.
 *
 * @author Denis Bondarenko
 * Date: 12.09.2017
 * Time: 17:21
 * E-mail: DenBond7@gmail.com
 */

class AddNewAccountManuallyActivity : BaseNodeActivity(), CompoundButton.OnCheckedChangeListener,
    AdapterView.OnItemSelectedListener, View.OnClickListener, TextWatcher, LoaderManager.LoaderCallbacks<LoaderResult> {

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
  private var layoutSmtpSignIn: View? = null
  private var progressView: View? = null
  override lateinit var rootView: View
  private var checkBoxRequireSignInForSmtp: CheckBox? = null
  private var authCreds: AuthCredentials? = null

  private var isImapSpinnerRestored: Boolean = false
  private var isSmtpSpinnerRestored: Boolean = false

  override val isDisplayHomeAsUpEnabled: Boolean
    get() = true

  override val contentViewResourceId: Int
    get() = R.layout.activity_add_new_account_manually

  /**
   * Retrieve a temp [AuthCredentials] from the shared preferences.
   */
  private val tempAuthCreds: AuthCredentials?
    get() {
      val authCredsJson =
          SharedPreferencesHelper.getString(PreferenceManager.getDefaultSharedPreferences(this),
              Constants.PREFERENCES_KEY_TEMP_LAST_AUTH_CREDENTIALS, "")

      if (!TextUtils.isEmpty(authCredsJson)) {
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
   * Do a lot of checks to validate an outgoing message info.
   *
   * @return <tt>Boolean</tt> true if all information is correct, false otherwise.
   */
  private val isDataCorrect: Boolean
    get() {
      if (TextUtils.isEmpty(editTextEmail!!.text)) {
        showInfoSnackbar(editTextEmail!!, getString(R.string.text_must_not_be_empty, getString(R.string.e_mail)))
        editTextEmail!!.requestFocus()
      } else if (GeneralUtil.isEmailValid(editTextEmail!!.text)) {
        when {
          TextUtils.isEmpty(editTextUserName!!.text) -> {
            showInfoSnackbar(editTextUserName!!, getString(R.string.text_must_not_be_empty,
                getString(R.string.username)))
            editTextUserName!!.requestFocus()
          }

          TextUtils.isEmpty(editTextPassword!!.text) -> {
            showInfoSnackbar(editTextPassword!!, getString(R.string.text_must_not_be_empty,
                getString(R.string.password)))
            editTextPassword!!.requestFocus()
          }

          TextUtils.isEmpty(editTextImapServer!!.text) -> {
            showInfoSnackbar(editTextImapServer!!, getString(R.string.text_must_not_be_empty,
                getString(R.string.imap_server)))
            editTextImapServer!!.requestFocus()
          }

          TextUtils.isEmpty(editTextImapPort!!.text) -> {
            showInfoSnackbar(editTextImapPort!!, getString(R.string.text_must_not_be_empty,
                getString(R.string.imap_port)))
            editTextImapPort!!.requestFocus()
          }

          TextUtils.isEmpty(editTextSmtpServer!!.text) -> {
            showInfoSnackbar(editTextSmtpServer!!, getString(R.string.text_must_not_be_empty,
                getString(R.string.smtp_server)))
            editTextSmtpServer!!.requestFocus()
          }

          TextUtils.isEmpty(editTextSmtpPort!!.text) -> {
            showInfoSnackbar(editTextSmtpPort!!, getString(R.string.text_must_not_be_empty,
                getString(R.string.smtp_port)))
            editTextSmtpPort!!.requestFocus()
          }

          checkBoxRequireSignInForSmtp!!.isChecked -> when {
            TextUtils.isEmpty(editTextSmtpUsername!!.text) -> {
              showInfoSnackbar(editTextSmtpUsername!!, getString(R.string.text_must_not_be_empty,
                  getString(R.string.smtp_username)))
              editTextSmtpUsername!!.requestFocus()
            }

            TextUtils.isEmpty(editTextSmtpPassword!!.text) -> {
              showInfoSnackbar(editTextSmtpPassword!!, getString(R.string.text_must_not_be_empty,
                  getString(R.string.smtp_password)))
              editTextSmtpPassword!!.requestFocus()
            }
            else -> return true
          }

          else -> return true
        }
      } else {
        showInfoSnackbar(editTextEmail!!, getString(R.string.error_email_is_not_valid))
        editTextEmail!!.requestFocus()
      }

      return false
    }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    this.authCreds = tempAuthCreds

    if (authCreds == null) {
      isImapSpinnerRestored = true
      isSmtpSpinnerRestored = true
    }

    initViews(savedInstanceState)
  }

  public override fun onPause() {
    super.onPause()
    saveTempCreds()
  }

  public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_ADD_NEW_ACCOUNT -> when (resultCode) {
        Activity.RESULT_OK -> returnOkResult()

        CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT -> {
          setResult(CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT, data)
          finish()
        }
      }

      REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_EMAIL -> when (resultCode) {
        Activity.RESULT_OK, CheckKeysActivity.RESULT_NEUTRAL -> returnOkResult()

        Activity.RESULT_CANCELED -> UIUtil.exchangeViewVisibility(this, false, progressView!!, rootView)

        CheckKeysActivity.RESULT_NEGATIVE -> {
          setResult(CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT, data)
          finish()
        }
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
    when (buttonView.id) {
      R.id.checkBoxRequireSignInForSmtp -> layoutSmtpSignIn!!.visibility = if (isChecked) View.VISIBLE else View.GONE
    }
  }

  override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
    when (parent.id) {
      R.id.spinnerImapSecurityType -> {
        val (_, _, defImapPort) = parent.adapter.getItem(position) as SecurityType
        if (isImapSpinnerRestored) {
          editTextImapPort!!.setText(defImapPort.toString())
        } else {
          isImapSpinnerRestored = true
        }
      }

      R.id.spinnerSmtpSecyrityType -> {
        val (_, _, _, defSmtpPort) = parent.adapter.getItem(position) as SecurityType
        if (isSmtpSpinnerRestored) {
          editTextSmtpPort!!.setText(defSmtpPort.toString())
        } else {
          isSmtpSpinnerRestored = true
        }
      }
    }
  }

  override fun onNothingSelected(parent: AdapterView<*>) {

  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonTryToConnect -> if (isDataCorrect) {
        authCreds = generateAuthCreds()
        UIUtil.hideSoftInput(this, rootView)
        if (checkDuplicate()) {
          LoaderManager.getInstance(this).restartLoader(R.id.loader_id_check_email_settings, null, this)
        } else {
          showInfoSnackbar(rootView, getString(R.string.template_email_alredy_added,
              authCreds!!.email), Snackbar.LENGTH_LONG)
        }
      }
    }
  }

  override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {

  }

  override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {

  }

  override fun afterTextChanged(editable: Editable) {
    if (GeneralUtil.isEmailValid(editable)) {
      val email = editable.toString()
      val mainDomain = email.substring(email.indexOf('@') + 1)
      editTextImapServer!!.setText(getString(R.string.template_imap_server, mainDomain))
      editTextSmtpServer!!.setText(getString(R.string.template_smtp_server, mainDomain))
      editTextUserName!!.setText(email.substring(0, email.indexOf('@')))
      editTextSmtpUsername!!.setText(email.substring(0, email.indexOf('@')))
    }
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<LoaderResult> {
    when (id) {
      R.id.loader_id_check_email_settings -> {
        UIUtil.exchangeViewVisibility(this, true, progressView!!, rootView)

        authCreds = generateAuthCreds()
        return CheckEmailSettingsAsyncTaskLoader(this, authCreds!!)
      }

      R.id.loader_id_load_private_key_backups_from_email -> {
        UIUtil.exchangeViewVisibility(this, true, progressView!!, rootView)
        val account = AccountDao(authCreds!!.email, null, authCreds!!.username, null, null, null,
            false, authCreds)
        return LoadPrivateKeysFromMailAsyncTaskLoader(this, account)
      }

      else -> return Loader(this)
    }
  }

  override fun onLoadFinished(loader: Loader<LoaderResult>, loaderResult: LoaderResult) {
    handleLoaderResult(loader, loaderResult)
  }

  override fun onLoaderReset(loader: Loader<LoaderResult>) {

  }

  @Suppress("UNCHECKED_CAST")
  override fun onSuccess(loaderId: Int, result: Any?) {
    when (loaderId) {
      R.id.loader_id_check_email_settings -> {
        val isCorrect = result as Boolean
        if (isCorrect) {
          LoaderManager.getInstance(this).restartLoader(R.id.loader_id_load_private_key_backups_from_email, null, this)
        } else {
          UIUtil.exchangeViewVisibility(this, false, progressView!!, rootView)
          showInfoSnackbar(rootView, getString(R.string.settings_not_valid), Snackbar.LENGTH_LONG)
        }
      }

      R.id.loader_id_load_private_key_backups_from_email -> {
        val keyDetailsList = result as ArrayList<NodeKeyDetails>?
        if (CollectionUtils.isEmpty(keyDetailsList)) {
          val account = AccountDao(authCreds!!.email, null, authCreds!!.username, null, null, null,
              false, authCreds)
          startActivityForResult(CreateOrImportKeyActivity.newIntent(this, account, true),
              REQUEST_CODE_ADD_NEW_ACCOUNT)
          UIUtil.exchangeViewVisibility(this, false, progressView!!, rootView)
        } else {
          val bottomTitle = resources.getQuantityString(R.plurals.found_backup_of_your_account_key,
              keyDetailsList!!.size, keyDetailsList.size)
          val neutralBtnTitle = if (SecurityUtils.hasBackup(this)) getString(R.string.use_existing_keys) else null
          val intent = CheckKeysActivity.newIntent(this, keyDetailsList, KeyDetails.Type.EMAIL, bottomTitle,
              getString(R.string.continue_), neutralBtnTitle, getString(R.string.use_another_account))
          startActivityForResult(intent, REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_EMAIL)
        }

        LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_load_private_key_backups_from_email)
      }

      else -> super.onSuccess(loaderId, result)
    }
  }

  override fun onError(loaderId: Int, e: Exception?) {
    when (loaderId) {
      R.id.loader_id_check_email_settings -> {
        UIUtil.exchangeViewVisibility(this, false, progressView!!, rootView)
        val original = e?.cause
        if (original != null) {
          if (original is AuthenticationFailedException) {
            val isGmailImapServer = editTextImapServer!!.text.toString().equals(GmailConstants
                .GMAIL_IMAP_SERVER, ignoreCase = true)
            val isMsgEmpty = TextUtils.isEmpty(original.message)
            val hasAlert = original.message?.startsWith(GmailConstants
                .GMAIL_ALERT_MESSAGE_WHEN_LESS_SECURE_NOT_ALLOWED)
            if (isGmailImapServer && !isMsgEmpty && hasAlert == true) {
              showLessSecurityWarning()
            } else {
              showInfoSnackbar(rootView, if (!TextUtils.isEmpty(e.message))
                e.message
              else
                getString(R.string.unknown_error), Snackbar.LENGTH_LONG)
            }
          } else if (original is MailConnectException || original is SocketTimeoutException) {
            showNetworkErrorHint()
          }
        } else {
          showInfoSnackbar(rootView, if (e != null && !TextUtils.isEmpty(e.message))
            e.message
          else
            getString(R.string.unknown_error), Snackbar.LENGTH_LONG)
        }
      }

      R.id.loader_id_load_private_key_backups_from_email -> {
        UIUtil.exchangeViewVisibility(this, false, progressView!!, rootView)
        showInfoSnackbar(rootView, if (e != null && !TextUtils.isEmpty(e.message))
          e.message
        else
          getString(R.string.unknown_error), Snackbar.LENGTH_LONG)
      }

      else -> super.onError(loaderId, e)
    }
  }

  private fun showNetworkErrorHint() {
    showSnackbar(rootView, getString(R.string.network_error_please_retry), getString(R.string.retry),
        Snackbar.LENGTH_LONG, View.OnClickListener {
      LoaderManager.getInstance(this@AddNewAccountManuallyActivity)
          .restartLoader(R.id.loader_id_check_email_settings, null, this@AddNewAccountManuallyActivity)
    })
  }

  private fun showLessSecurityWarning() {
    showSnackbar(rootView, getString(R.string.less_secure_login_is_not_allowed),
        getString(android.R.string.ok), Snackbar.LENGTH_LONG, View.OnClickListener {
      setResult(RESULT_CODE_CONTINUE_WITH_GMAIL)
      finish()
    })
  }

  /**
   * Return the [Activity.RESULT_OK] to the initiator-activity.
   */
  private fun returnOkResult() {
    authCreds = generateAuthCreds()
    val intent = Intent()
    intent.putExtra(KEY_EXTRA_AUTH_CREDENTIALS, authCreds)
    setResult(Activity.RESULT_OK, intent)
    finish()
  }

  /**
   * Check that current email is not duplicate and not added yet.
   *
   * @return true if email not added yet, otherwise false.
   */
  private fun checkDuplicate(): Boolean {
    return AccountDaoSource().getAccountInformation(this, authCreds!!.email) == null
  }

  private fun initViews(savedInstanceState: Bundle?) {
    editTextEmail = findViewById(R.id.editTextEmail)
    editTextUserName = findViewById(R.id.editTextUserName)
    editTextPassword = findViewById(R.id.editTextPassword)
    editTextImapServer = findViewById(R.id.editTextImapServer)
    editTextImapPort = findViewById(R.id.editTextImapPort)
    editTextSmtpServer = findViewById(R.id.editTextSmtpServer)
    editTextSmtpPort = findViewById(R.id.editTextSmtpPort)
    editTextSmtpUsername = findViewById(R.id.editTextSmtpUsername)
    editTextSmtpPassword = findViewById(R.id.editTextSmtpPassword)

    editTextEmail!!.addTextChangedListener(this)

    layoutSmtpSignIn = findViewById(R.id.layoutSmtpSignIn)
    progressView = findViewById(R.id.progressView)
    rootView = findViewById(R.id.layoutContent)
    checkBoxRequireSignInForSmtp = findViewById(R.id.checkBoxRequireSignInForSmtp)
    checkBoxRequireSignInForSmtp!!.setOnCheckedChangeListener(this)

    spinnerImapSecurityType = findViewById(R.id.spinnerImapSecurityType)
    spinnerSmtpSecurityType = findViewById(R.id.spinnerSmtpSecyrityType)

    val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
        SecurityType.generateSecurityTypes(this))

    spinnerImapSecurityType!!.adapter = adapter
    spinnerSmtpSecurityType!!.adapter = adapter

    spinnerImapSecurityType!!.onItemSelectedListener = this
    spinnerSmtpSecurityType!!.onItemSelectedListener = this

    if (findViewById<View>(R.id.buttonTryToConnect) != null) {
      findViewById<View>(R.id.buttonTryToConnect).setOnClickListener(this)
    }

    if (savedInstanceState == null) {
      updateView()
    }
  }

  /**
   * Update the current views if [AuthCredentials] not null.l
   */
  private fun updateView() {
    if (authCreds != null) {

      editTextEmail!!.setText(authCreds!!.email)
      editTextUserName!!.setText(authCreds!!.username)
      editTextImapServer!!.setText(authCreds!!.imapServer)
      editTextImapPort!!.setText(authCreds!!.imapPort.toString())
      editTextSmtpServer!!.setText(authCreds!!.smtpServer)
      editTextSmtpPort!!.setText(authCreds!!.smtpPort.toString())
      checkBoxRequireSignInForSmtp!!.isChecked = authCreds!!.hasCustomSignInForSmtp
      editTextSmtpUsername!!.setText(authCreds!!.smtpSigInUsername)

      val imapOptionsCount = spinnerImapSecurityType!!.adapter.count
      for (i in 0 until imapOptionsCount) {
        if (authCreds!!.imapOpt === (spinnerImapSecurityType!!.adapter.getItem(i) as SecurityType).opt) {
          spinnerImapSecurityType!!.setSelection(i)
        }
      }

      val smtpOptionsCount = spinnerSmtpSecurityType!!.adapter.count
      for (i in 0 until smtpOptionsCount) {
        if (authCreds!!.smtpOpt === (spinnerSmtpSecurityType!!.adapter.getItem(i) as SecurityType).opt) {
          spinnerSmtpSecurityType!!.setSelection(i)
        }
      }
    }
  }

  /**
   * Save the current [AuthCredentials] to the shared preferences.
   */
  private fun saveTempCreds() {
    authCreds = generateAuthCreds()
    val gson = Gson()
    authCreds!!.password = ""
    authCreds!!.smtpSignInPassword = null
    SharedPreferencesHelper.setString(PreferenceManager.getDefaultSharedPreferences(this),
        Constants.PREFERENCES_KEY_TEMP_LAST_AUTH_CREDENTIALS, gson.toJson(authCreds))
  }

  /**
   * Generate the [AuthCredentials] using user input.
   *
   * @return [AuthCredentials].
   */
  private fun generateAuthCreds(): AuthCredentials {
    val imapPort = if (TextUtils.isEmpty(editTextImapPort!!.text))
      JavaEmailConstants.DEFAULT_IMAP_PORT
    else
      Integer.parseInt(editTextImapPort!!.text.toString())

    val smtpPort = if (TextUtils.isEmpty(editTextSmtpPort!!.text))
      JavaEmailConstants.DEFAULT_SMTP_PORT
    else
      Integer.parseInt(editTextSmtpPort!!.text.toString())

    return AuthCredentials(editTextEmail!!.text.toString(), editTextUserName!!.text.toString(),
        editTextPassword!!.text.toString(), editTextImapServer!!.text.toString(), imapPort,
        (spinnerImapSecurityType!!.selectedItem as SecurityType).opt,
        editTextSmtpServer!!.text.toString(), smtpPort,
        (spinnerSmtpSecurityType!!.selectedItem as SecurityType).opt,
        checkBoxRequireSignInForSmtp!!.isChecked,
        editTextSmtpUsername!!.text.toString(),
        editTextSmtpPassword!!.text.toString())
  }

  companion object {
    const val RESULT_CODE_CONTINUE_WITH_GMAIL = 101

    val KEY_EXTRA_AUTH_CREDENTIALS =
        GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_AUTH_CREDENTIALS", AddNewAccountManuallyActivity::class.java)

    private const val REQUEST_CODE_ADD_NEW_ACCOUNT = 10
    private const val REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_EMAIL = 11
  }
}
