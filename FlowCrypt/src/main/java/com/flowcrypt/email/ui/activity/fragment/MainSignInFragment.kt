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
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.email.JavaEmailConstants
import com.flowcrypt.email.api.retrofit.response.api.DomainRulesResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.jetpack.viewmodel.EnterpriseDomainRulesViewModel
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.security.SecurityUtils
import com.flowcrypt.email.service.CheckClipboardToFindKeyService
import com.flowcrypt.email.service.EmailSyncService
import com.flowcrypt.email.service.actionqueue.actions.LoadGmailAliasesAction
import com.flowcrypt.email.ui.activity.CheckKeysActivity
import com.flowcrypt.email.ui.activity.CreateOrImportKeyActivity
import com.flowcrypt.email.ui.activity.EmailManagerActivity
import com.flowcrypt.email.ui.activity.HtmlViewFromAssetsRawActivity
import com.flowcrypt.email.ui.activity.SignInActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseSingInFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.SavePrivateKeyToDatabaseException
import com.flowcrypt.email.util.google.GoogleApiClientHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import kotlinx.coroutines.launch

/**
 * @author Denis Bondarenko
 *         Date: 7/17/20
 *         Time: 12:06 PM
 *         E-mail: DenBond7@gmail.com
 */
class MainSignInFragment : BaseSingInFragment(), ProgressBehaviour {
  private lateinit var client: GoogleSignInClient
  private var googleSignInAccount: GoogleSignInAccount? = null
  private var uuid: String? = null
  private var domainRules: List<String>? = null

  private val enterpriseDomainRulesViewModel: EnterpriseDomainRulesViewModel by viewModels()
  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()

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

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    subscribeToAuthorizeAndSearchBackups()
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)
    setupEnterpriseViewModel()
    setupPrivateKeysViewModel()
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

  override fun runEmailManagerActivity() {
    if (googleSignInAccount == null) {
      ExceptionUtil.handleError(NullPointerException("GoogleSignInAccount is null!"))
      Toast.makeText(requireContext(), R.string.error_occurred_try_again_later, Toast.LENGTH_SHORT).show()
      return
    }

    googleSignInAccount?.email?.let { email ->
      lifecycleScope.launch {
        val roomDatabase = FlowCryptRoomDatabase.getDatabase(requireContext())
        val existedAccount = roomDatabase.accountDao().getAccountSuspend(email)

        val insertOrUpdateCandidate = googleSignInAccount?.let {
          AccountEntity(it, uuid, domainRules)
        }

        insertOrUpdateCandidate?.let {
          if (existedAccount == null) {
            roomDatabase.accountDao().addAccountSuspend(insertOrUpdateCandidate)
          } else {
            roomDatabase.accountDao().updateAccountSuspend(insertOrUpdateCandidate.copy(
                id = existedAccount.id, uuid = existedAccount.uuid, domainRules = existedAccount.domainRules))
          }
        }

        EmailSyncService.startEmailSyncService(requireContext())
        roomBasicViewModel.addActionToQueue(LoadGmailAliasesAction(email = googleSignInAccount?.email))
        EmailManagerActivity.runEmailManagerActivity(requireContext())
        activity?.finish()
      }
    }
  }

  override fun returnResultOk() {
    googleSignInAccount?.let {
      lifecycleScope.launch {
        val accountEntity = AccountEntity(it, uuid, domainRules)
        val roomDatabase = FlowCryptRoomDatabase.getDatabase(requireContext())
        roomDatabase.accountDao().addAccountSuspend(accountEntity)
        roomBasicViewModel.addActionToQueue(LoadGmailAliasesAction(email = accountEntity.email))

        val intent = Intent()
        intent.putExtra(SignInActivity.KEY_EXTRA_NEW_ACCOUNT, accountEntity)

        activity?.setResult(Activity.RESULT_OK, intent)
        activity?.finish()
      }
    }
  }

  private fun initViews(view: View) {
    view.findViewById<View>(R.id.buttonSignInWithGmail)?.setOnClickListener {
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
      if (domainRules?.contains(AccountEntity.DomainRule.NO_PRV_BACKUP.name) == true) {
        if (googleSignInAccount != null) {
          requireContext().startService(Intent(requireContext(), CheckClipboardToFindKeyService::class.java))
          val intent = CreateOrImportKeyActivity.newIntent(requireContext(),
              AccountEntity(googleSignInAccount, uuid, domainRules), true)
          startActivityForResult(intent, REQUEST_CODE_CREATE_OR_IMPORT_KEY)
        }
      } else {
        googleSignInAccount?.let {
          val account = AccountEntity(it, uuid, domainRules)
          val nextFrag = AuthorizeAndSearchBackupsFragment.newInstance(account)
          activity?.supportFragmentManager?.beginTransaction()
              ?.replace(R.id.fragmentContainerView, nextFrag, AuthorizeAndSearchBackupsFragment::class.java.simpleName)
              ?.addToBackStack(null)
              ?.commit()
        }
      }
    } else {
      showContent()
      showInfoSnackbar(msgText = getString(R.string.template_email_alredy_added, existedAccount.email), duration = Snackbar.LENGTH_LONG)
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
      } else {
        //show error
      }
    }
  }

  private fun onFetchKeysCompleted(keyDetailsList: ArrayList<NodeKeyDetails>?) {
    if (keyDetailsList.isNullOrEmpty()) {
      googleSignInAccount?.let {
        requireContext().startService(Intent(requireContext(), CheckClipboardToFindKeyService::class.java))
        val intent = CreateOrImportKeyActivity.newIntent(requireContext(),
            AccountEntity(it, uuid, domainRules), true)
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

  private fun setupPrivateKeysViewModel() {
    privateKeysViewModel.savePrivateKeysLiveData.observe(viewLifecycleOwner, Observer {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            showProgress(getString(R.string.processing))
          }

          Result.Status.SUCCESS -> {
            if (existedAccounts.isEmpty()) runEmailManagerActivity() else returnResultOk()
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            showContent()
            val e = it.exception
            if (e is SavePrivateKeyToDatabaseException) {
              showSnackbar(
                  msgText = e.message ?: e.javaClass.simpleName,
                  btnName = getString(R.string.retry),
                  duration = Snackbar.LENGTH_INDEFINITE,
                  onClickListener = View.OnClickListener {
                    privateKeysViewModel.encryptAndSaveKeysToDatabase(e.keys, KeyDetails.Type.EMAIL)
                  }
              )
            } else {
              showInfoSnackbar(msgText = e?.message ?: e?.javaClass?.simpleName
              ?: getString(R.string.unknown_error))
            }
          }
        }
      }
    })
  }

  private fun setupEnterpriseViewModel() {
    enterpriseDomainRulesViewModel.domainRulesLiveData.observe(viewLifecycleOwner, Observer {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            showProgress(progressMsg = "Loading domain rules...")
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
          privateKeysViewModel.encryptAndSaveKeysToDatabase(keys, KeyDetails.Type.EMAIL)
        }
      }

      CheckKeysActivity.RESULT_USE_EXISTING_KEYS -> {
        if (existedAccounts.isEmpty()) runEmailManagerActivity() else returnResultOk()
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