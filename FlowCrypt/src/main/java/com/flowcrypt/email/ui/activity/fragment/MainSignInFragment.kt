/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.retrofit.response.api.DomainRulesResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.jetpack.viewmodel.EnterpriseDomainRulesViewModel
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.security.model.NodeKeyDetails
import com.flowcrypt.email.service.CheckClipboardToFindKeyService
import com.flowcrypt.email.service.actionqueue.actions.LoadGmailAliasesAction
import com.flowcrypt.email.ui.activity.CheckKeysActivity
import com.flowcrypt.email.ui.activity.CreateOrImportKeyActivity
import com.flowcrypt.email.ui.activity.HtmlViewFromAssetsRawActivity
import com.flowcrypt.email.ui.activity.SignInActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseSingInFragment
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.AccountAlreadyAddedException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.google.GoogleApiClientHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.sun.mail.util.MailConnectException
import java.net.SocketTimeoutException
import java.util.*

/**
 * @author Denis Bondarenko
 *         Date: 7/17/20
 *         Time: 12:06 PM
 *         E-mail: DenBond7@gmail.com
 */
class MainSignInFragment : BaseSingInFragment() {
  private lateinit var client: GoogleSignInClient
  private var googleSignInAccount: GoogleSignInAccount? = null
  private var uuid: String? = null
  private var domainRules: List<String>? = null

  private val enterpriseDomainRulesViewModel: EnterpriseDomainRulesViewModel by viewModels()

  override val progressView: View?
    get() = view?.findViewById(R.id.progress)
  override val contentView: View?
    get() = view?.findViewById(R.id.layoutContent)
  override val statusView: View?
    get() = view?.findViewById(R.id.status)

  override val contentResourceId: Int = R.layout.fragment_main_sign_in

  override fun onAttach(context: Context) {
    super.onAttach(context)
    client = GoogleSignIn.getClient(context, GoogleApiClientHelper.generateGoogleSignInOptions())
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)

    subscribeToCheckAccountSettings()
    subscribeToAuthorizeAndSearchBackups()

    initAddNewAccountLiveData()
    setupEnterpriseViewModel()
    initSavePrivateKeysLiveData()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_RESOLVE_SIGN_IN_ERROR -> {
        if (resultCode == Activity.RESULT_OK) {
          signInWithGmail()
        }
      }

      REQUEST_CODE_SIGN_IN -> {
        handleSignInResult(resultCode, GoogleSignIn.getSignedInAccountFromIntent(data))
      }

      REQUEST_CODE_CREATE_OR_IMPORT_KEY -> when (resultCode) {
        Activity.RESULT_OK -> if (existedAccounts.isEmpty()) runEmailManagerActivity() else returnResultOk()

        Activity.RESULT_CANCELED, CreateOrImportKeyActivity.RESULT_CODE_USE_ANOTHER_ACCOUNT -> {
          this.googleSignInAccount = null
          showContent()
        }

        CreateOrImportKeyActivity.RESULT_CODE_HANDLE_RESOLVED_KEYS -> {
          handleResultFromCheckKeysActivity(resultCode, data)
        }
      }

      REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL -> {
        handleResultFromCheckKeysActivity(resultCode, data)
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun getTempAccount(): AccountEntity? {
    return googleSignInAccount?.let { AccountEntity(it, uuid, domainRules) }
  }

  override fun returnResultOk() {
    getTempAccount()?.let {
      roomBasicViewModel.addActionToQueue(LoadGmailAliasesAction(email = it.email))

      val intent = Intent()
      intent.putExtra(SignInActivity.KEY_EXTRA_NEW_ACCOUNT, it)
      activity?.setResult(Activity.RESULT_OK, intent)
      activity?.finish()
    }
  }

  private fun initViews(view: View) {
    view.findViewById<View>(R.id.buttonSignInWithGmail)?.setOnClickListener {
      importCandidates.clear()
      signInWithGmail()
    }

    view.findViewById<View>(R.id.buttonOtherEmailProvider)?.setOnClickListener {
      val addOtherAccountFragment = AddOtherAccountFragment.newInstance()
      activity?.supportFragmentManager?.beginTransaction()
          ?.replace(R.id.fragmentContainerView, addOtherAccountFragment, AddOtherAccountFragment::class.java.simpleName)
          ?.addToBackStack(null)
          ?.commit()
    }

    view.findViewById<View>(R.id.buttonPrivacy)?.setOnClickListener {
      GeneralUtil.openCustomTab(requireContext(), Constants.FLOWCRYPT_PRIVACY_URL)
    }

    view.findViewById<View>(R.id.buttonTerms)?.setOnClickListener {
      GeneralUtil.openCustomTab(requireContext(), Constants.FLOWCRYPT_TERMS_URL)
    }

    view.findViewById<View>(R.id.buttonSecurity)?.setOnClickListener {
      startActivity(HtmlViewFromAssetsRawActivity.newIntent(requireContext(), getString(R.string.security),
          "html/security.htm"))
    }

    view.findViewById<View>(R.id.buttonHelp)?.setOnClickListener {
      FeedbackActivity.show(requireActivity())
    }
  }

  private fun signInWithGmail() {
    googleSignInAccount = null

    if (GeneralUtil.isConnected(activity)) {
      client.signOut()
      startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
    } else {
      showInfoSnackbar(msgText = activity?.getString(R.string.internet_connection_is_not_available))
    }
  }

  private fun handleSignInResult(resultCode: Int, task: Task<GoogleSignInAccount>) {
    try {
      if (task.isSuccessful) {
        googleSignInAccount = task.getResult(ApiException::class.java)

        val account = googleSignInAccount?.account?.name ?: return
        val idToken = googleSignInAccount?.idToken ?: return
        uuid = SecurityUtils.generateRandomUUID()
        if (JavaEmailConstants.EMAIL_PROVIDER_GMAIL.equals(EmailUtil.getDomain(account), true)) {
          domainRules = emptyList()
          onSignSuccess(googleSignInAccount)
        } else {
          uuid?.let { enterpriseDomainRulesViewModel.getDomainRules(account, it, idToken) }
        }
      } else {
        val error = task.exception

        if (error is ApiException) {
          throw error
        }

        showInfoSnackbar(msgText = error?.message ?: error?.javaClass?.simpleName
        ?: getString(R.string.unknown_error))
      }
    } catch (e: ApiException) {
      val msg = GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)
      if (resultCode == Activity.RESULT_OK) {
        showInfoSnackbar(msgText = msg)
      } else {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun onSignSuccess(googleSignInAccount: GoogleSignInAccount?) {
    val existedAccount = existedAccounts.firstOrNull {
      it.email.equals(googleSignInAccount?.email, ignoreCase = true)
    }

    if (existedAccount == null) {
      getTempAccount()?.let {
        if (domainRules?.contains(AccountEntity.DomainRule.NO_PRV_BACKUP.name) == true) {
          requireContext().startService(Intent(requireContext(), CheckClipboardToFindKeyService::class.java))
          val intent = CreateOrImportKeyActivity.newIntent(requireContext(), it, true)
          startActivityForResult(intent, REQUEST_CODE_CREATE_OR_IMPORT_KEY)
        } else {
          val nextFrag = AuthorizeAndSearchBackupsFragment.newInstance(it)
          activity?.supportFragmentManager?.beginTransaction()
              ?.replace(R.id.fragmentContainerView, nextFrag, AuthorizeAndSearchBackupsFragment::class.java.simpleName)
              ?.addToBackStack(null)
              ?.commit()
        }
      }
    } else {
      showContent()
      showInfoSnackbar(msgText = getString(R.string.template_email_already_added, existedAccount.email), duration = Snackbar.LENGTH_LONG)
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
            val msg: String? = if (exception.message.isNullOrEmpty()) {
              exception.javaClass.simpleName
            } else exception.message
            var title: String? = null

            if (original != null) {
              if (original is MailConnectException || original is SocketTimeoutException) {
                title = getString(R.string.network_error)
              }
            } else if (exception is AccountAlreadyAddedException) {
              showInfoSnackbar(view, exception.message, Snackbar.LENGTH_LONG)
              return@setFragmentResultListener
            }

            val faqUrl = "https://support.google.com/mail/answer/75725?hl=" + GeneralUtil
                .getLocaleLanguageCode(requireContext())
            val dialogMsg = msg + getString(R.string.provider_faq, faqUrl)

            showInfoDialog(
                dialogTitle = title,
                dialogMsg = dialogMsg,
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
            onFetchKeysCompleted(result.data as ArrayList<NodeKeyDetails>?)
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            showContent()

            if (result.exception is UserRecoverableAuthIOException) {
              startActivityForResult(result.exception.intent, REQUEST_CODE_RESOLVE_SIGN_IN_ERROR)
            } else {
              showInfoSnackbar(msgText =
              result.exception?.message ?: result.exception?.javaClass?.simpleName
              ?: getString(R.string.unknown_error))
            }
          }

          else -> {

          }
        }
      }
    }
  }

  private fun onFetchKeysCompleted(keyDetailsList: ArrayList<NodeKeyDetails>?) {
    if (keyDetailsList.isNullOrEmpty()) {
      getTempAccount()?.let {
        requireContext().startService(Intent(requireContext(), CheckClipboardToFindKeyService::class.java))
        val intent = CreateOrImportKeyActivity.newIntent(requireContext(), it, true)
        startActivityForResult(intent, REQUEST_CODE_CREATE_OR_IMPORT_KEY)
      }
    } else {
      val subTitle = resources.getQuantityString(R.plurals.found_backup_of_your_account_key, keyDetailsList.size, keyDetailsList.size)
      val positiveBtnTitle = getString(R.string.continue_)
      val negativeBtnTitle = getString(R.string.use_another_account)
      val intent = CheckKeysActivity.newIntent(context = requireContext(), privateKeys = keyDetailsList,
          type = KeyDetails.Type.EMAIL, subTitle = subTitle, positiveBtnTitle = positiveBtnTitle,
          negativeBtnTitle = negativeBtnTitle)
      startActivityForResult(intent, REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL)
    }
  }

  private fun setupEnterpriseViewModel() {
    enterpriseDomainRulesViewModel.domainRulesLiveData.observe(viewLifecycleOwner, {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            showProgress(progressMsg = getString(R.string.loading_domain_rules))
          }

          Result.Status.SUCCESS -> {
            val result = it.data as? DomainRulesResponse
            domainRules = result?.domainRules?.flags ?: emptyList()
            onSignSuccess(googleSignInAccount)
            enterpriseDomainRulesViewModel.domainRulesLiveData.value = null
          }

          Result.Status.ERROR -> {
            showContent()
            Toast.makeText(requireContext(), it.data?.apiError?.msg
                ?: getString(R.string.could_not_load_domain_rules), Toast.LENGTH_SHORT).show()
          }

          Result.Status.EXCEPTION -> {
            showContent()
            Toast.makeText(requireContext(), it.exception?.message
                ?: getString(R.string.could_not_load_domain_rules), Toast.LENGTH_SHORT).show()
          }
        }
      }
    })
  }

  private fun handleResultFromCheckKeysActivity(resultCode: Int, data: Intent?) {
    when (resultCode) {
      Activity.RESULT_OK, CheckKeysActivity.RESULT_SKIP_REMAINING_KEYS -> {
        val keys: List<NodeKeyDetails>? = data?.getParcelableArrayListExtra(
            CheckKeysActivity.KEY_EXTRA_UNLOCKED_PRIVATE_KEYS)

        if (keys.isNullOrEmpty()) {
          showInfoSnackbar(msgText = getString(R.string.error_no_keys))
        } else {
          importCandidates.clear()
          importCandidates.addAll(keys)

          if (getTempAccount() == null) {
            ExceptionUtil.handleError(NullPointerException("GoogleSignInAccount is null!"))
            Toast.makeText(requireContext(), R.string.error_occurred_try_again_later, Toast.LENGTH_SHORT).show()
            return
          } else {
            getTempAccount()?.let {
              accountViewModel.addNewAccount(it)
            }
          }
        }
      }

      CheckKeysActivity.RESULT_NO_NEW_KEYS -> {
        Toast.makeText(requireContext(), getString(R.string.key_already_imported_finishing_setup), Toast.LENGTH_SHORT).show()
        if (existedAccounts.isEmpty()) runEmailManagerActivity() else returnResultOk()
      }

      Activity.RESULT_CANCELED, CheckKeysActivity.RESULT_NEGATIVE -> {
        this.googleSignInAccount = null
        showContent()
      }
    }
  }

  companion object {
    private const val REQUEST_CODE_SIGN_IN = 100
    private const val REQUEST_CODE_RESOLVE_SIGN_IN_ERROR = 101
    private const val REQUEST_CODE_CREATE_OR_IMPORT_KEY = 102
    private const val REQUEST_CODE_CHECK_PRIVATE_KEYS_FROM_GMAIL = 103
  }
}