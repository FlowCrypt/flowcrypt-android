/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.model.AuthCredentials
import com.flowcrypt.email.api.email.model.SecurityType
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.addInputFilter
import com.flowcrypt.email.extensions.onItemSelected
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.widget.inputfilters.InputFilters

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

  override val progressView: View?
    get() = view?.findViewById(R.id.progress)
  override val contentView: View?
    get() = view?.findViewById(R.id.layoutContent)
  override val statusView: View?
    get() = view?.findViewById(R.id.status)

  override val contentResourceId: Int = R.layout.fragment_server_settings

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.title = getString(R.string.server_settings)
    initViews(view)
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    accountEntity?.let { updateViews(AuthCredentials.from(it).copy(password = "", smtpSignInPassword = "")) }
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
        SecurityType.generateSecurityTypes(requireContext()))

    spinnerSmtpSecurityType?.adapter = ArrayAdapter(
        requireContext(),
        android.R.layout.simple_spinner_dropdown_item,
        SecurityType.generateSecurityTypes(requireContext()))

    spinnerImapSecurityType?.onItemSelected { parent, _, position, _ ->
      val securityType = parent?.adapter?.getItem(position) as SecurityType
      editTextImapPort?.setText(securityType.defImapPort.toString())
    }

    spinnerSmtpSecurityType?.onItemSelected { parent, _, position, _ ->
      val securityType = parent?.adapter?.getItem(position) as SecurityType
      editTextSmtpPort?.setText(securityType.defSmtpPort.toString())
    }

    view.findViewById<View>(R.id.buttonCheckAndSave)?.setOnClickListener {

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
}