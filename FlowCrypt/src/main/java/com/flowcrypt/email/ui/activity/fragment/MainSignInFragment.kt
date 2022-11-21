/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.NavGraphDirections
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.retrofit.response.api.ClientConfigurationResponse
import com.flowcrypt.email.api.retrofit.response.api.DomainOrgRulesResponse
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.OrgRules
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.databinding.FragmentMainSignInBinding
import com.flowcrypt.email.extensions.android.os.getParcelableArrayListViaExt
import com.flowcrypt.email.extensions.android.os.getParcelableViaExt
import com.flowcrypt.email.extensions.android.os.getSerializableViaExt
import com.flowcrypt.email.extensions.androidx.navigation.navigateSafe
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.extensions.getNavigationResult
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.showFeedbackFragment
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showTwoWayDialog
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.CheckFesServerViewModel
import com.flowcrypt.email.jetpack.viewmodel.DomainOrgRulesViewModel
import com.flowcrypt.email.jetpack.viewmodel.EkmViewModel
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.service.CheckClipboardToFindKeyService
import com.flowcrypt.email.ui.activity.fragment.CheckKeysFragment.CheckingState.Companion.CHECKED_KEYS
import com.flowcrypt.email.ui.activity.fragment.CheckKeysFragment.CheckingState.Companion.SKIP_REMAINING_KEYS
import com.flowcrypt.email.ui.activity.fragment.base.BaseSingInFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.AccountAlreadyAddedException
import com.flowcrypt.email.util.exception.CommonConnectionException
import com.flowcrypt.email.util.exception.EkmNotSupportedException
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.flowcrypt.email.util.exception.UnsupportedOrgRulesException
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
import org.pgpainless.util.Passphrase
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException

/**
 * @author Denis Bondarenko
 *         Date: 7/17/20
 *         Time: 12:06 PM
 *         E-mail: DenBond7@gmail.com
 */
class MainSignInFragment : BaseSingInFragment<FragmentMainSignInBinding>() {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentMainSignInBinding.inflate(inflater, container, false)

  private lateinit var client: GoogleSignInClient
  private var googleSignInAccount: GoogleSignInAccount? = null
  private var orgRules: OrgRules? = null
  private var fesUrl: String? = null

  private val checkFesServerViewModel: CheckFesServerViewModel by viewModels()
  private val domainOrgRulesViewModel: DomainOrgRulesViewModel by viewModels()
  private val ekmViewModel: EkmViewModel by viewModels()
  private var useStartTlsForSmtp = false

  private val forActivityResultSignIn = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result: ActivityResult ->
    handleSignInResult(result.resultCode, GoogleSignIn.getSignedInAccountFromIntent(result.data))
  }

  private val forActivityResultSignInError = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result: ActivityResult ->
    if (result.resultCode == Activity.RESULT_OK) {
      signInWithGmail()
    }
  }

  override val progressView: View?
    get() = binding?.progress?.root
  override val contentView: View?
    get() = binding?.layoutContent
  override val statusView: View?
    get() = binding?.status?.root
  override val isDisplayHomeAsUpEnabled: Boolean
    get() = false

  override fun onAttach(context: Context) {
    super.onAttach(context)
    client = GoogleSignIn.getClient(context, GoogleApiClientHelper.generateGoogleSignInOptions())
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)

    subscribeToCheckAccountSettings()
    subscribeToAuthorizeAndSearchBackups()
    subscribeToCheckPrivateKeys()
    subscribeToTwoWayDialog()
    subscribeCreateOrImportPrivateKeyDuringSetup()
    observeOnResultLiveData()

    initAddNewAccountLiveData()
    initEnterpriseViewModels()
    initPrivateKeysViewModel()
    initProtectPrivateKeysLiveData()
  }

  override fun getTempAccount(): AccountEntity? {
    return googleSignInAccount?.let {
      AccountEntity(
        googleSignInAccount = it,
        orgRules = orgRules,
        useFES = fesUrl?.isNotEmpty() == true,
        useStartTlsForSmtp = useStartTlsForSmtp,
      )
    }
  }

  override fun onAccountAdded(accountEntity: AccountEntity) {
    privateKeysViewModel.encryptAndSaveKeysToDatabase(
      accountEntity,
      importCandidates
    )
  }

  override fun onAdditionalActionsAfterPrivateKeyCreationCompleted(
    accountEntity: AccountEntity,
    pgpKeyDetails: PgpKeyDetails
  ) {
    handleUnlockedKeys(accountEntity, listOf(pgpKeyDetails))
  }

  private fun initViews(view: View) {
    view.findViewById<View>(R.id.buttonSignInWithGmail)?.setOnClickListener {
      importCandidates.clear()
      signInWithGmail()
    }

    view.findViewById<View>(R.id.buttonOtherEmailProvider)?.setOnClickListener {
      navController?.navigateSafe(
        R.id.mainSignInFragment,
        MainSignInFragmentDirections.actionMainSignInFragmentToAddOtherAccountFragment()
      )
    }

    view.findViewById<View>(R.id.buttonPrivacy)?.setOnClickListener {
      GeneralUtil.openCustomTab(requireContext(), Constants.FLOWCRYPT_PRIVACY_URL)
    }

    view.findViewById<View>(R.id.buttonTerms)?.setOnClickListener {
      GeneralUtil.openCustomTab(requireContext(), Constants.FLOWCRYPT_TERMS_URL)
    }

    view.findViewById<View>(R.id.buttonSecurity)?.setOnClickListener {
      navController?.navigateSafe(
        R.id.mainSignInFragment,
        NavGraphDirections.actionGlobalHtmlViewFromAssetsRawFragment(
          title = getString(R.string.security),
          resourceIdAsString = "html/security.htm"
        )
      )
    }

    view.findViewById<View>(R.id.buttonHelp)?.setOnClickListener {
      showFeedbackFragment()
    }
  }

  private fun signInWithGmail() {
    googleSignInAccount = null
    client.signOut()
    forActivityResultSignIn.launch(client.signInIntent)
  }

  private fun handleSignInResult(resultCode: Int, task: Task<GoogleSignInAccount>) {
    try {
      if (task.isSuccessful) {
        googleSignInAccount = task.getResult(ApiException::class.java)

        val account = googleSignInAccount?.account?.name ?: return

        val publicEmailDomains = EmailUtil.getPublicEmailDomains()
        if (EmailUtil.getDomain(account) in publicEmailDomains) {
          onSignSuccess(googleSignInAccount)
        } else {
          orgRules = null
          fesUrl = null
          checkFesServerViewModel.checkFesServerAvailability(account)
        }
      } else {
        val error = task.exception

        if (error is ApiException) {
          throw error
        }

        showInfoSnackbar(
          msgText = error?.message ?: error?.javaClass?.simpleName
          ?: getString(R.string.unknown_error)
        )
      }
    } catch (e: ApiException) {
      val msg = GoogleSignInStatusCodes.getStatusCodeString(e.statusCode)
      if (resultCode == Activity.RESULT_OK) {
        showInfoSnackbar(msgText = msg)
      } else {
        toast(msg)
      }
    }
  }

  private fun onSignSuccess(googleSignInAccount: GoogleSignInAccount?) {
    val existedAccount = existingAccounts.firstOrNull {
      it.email.equals(googleSignInAccount?.email, ignoreCase = true)
    }

    if (existedAccount == null) {
      getTempAccount()?.let {
        if (orgRules?.flags?.firstOrNull { rule -> rule == OrgRules.DomainRule.NO_PRV_BACKUP } != null) {
          requireContext().startService(
            Intent(requireContext(), CheckClipboardToFindKeyService::class.java)
          )
          navController?.navigate(
            MainSignInFragmentDirections
              .actionMainSignInFragmentToCreateOrImportPrivateKeyDuringSetupFragment(
                accountEntity = it, isShowAnotherAccountBtnEnabled = true
              )
          )
        } else {
          navController?.navigate(
            MainSignInFragmentDirections.actionMainSignInFragmentToAuthorizeAndSearchBackupsFragment(
              it
            )
          )
        }
      }
    } else {
      showContent()
      showInfoSnackbar(
        msgText = getString(
          R.string.template_email_already_added,
          existedAccount.email
        ), duration = Snackbar.LENGTH_LONG
      )
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

            if (original is MailConnectException && !useStartTlsForSmtp) {
              useStartTlsForSmtp = true
              onSignSuccess(googleSignInAccount)
              return@setFragmentResultListener
            }

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
            onFetchKeysCompleted(result.data as ArrayList<PgpKeyDetails>?)
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            showContent()

            if (result.exception is UserRecoverableAuthIOException) {
              forActivityResultSignInError.launch(result.exception.intent)
            } else {
              showInfoSnackbar(
                msgText =
                result.exception?.message ?: result.exception?.javaClass?.simpleName
                ?: getString(R.string.unknown_error)
              )
            }
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
        CHECKED_KEYS,
        SKIP_REMAINING_KEYS -> {
          handleUnlockedKeys(getTempAccount(), keys)
        }

        CheckKeysFragment.CheckingState.NO_NEW_KEYS -> {
          toast(R.string.key_already_imported_finishing_setup, Toast.LENGTH_SHORT)
          getTempAccount()?.let { onSetupCompleted(it) }
        }

        CheckKeysFragment.CheckingState.CANCELED, CheckKeysFragment.CheckingState.NEGATIVE -> {
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
        REQUEST_CODE_RETRY_CHECK_FES_AVAILABILITY -> if (result == TwoWayDialogFragment.RESULT_OK) {
          orgRules = null
          fesUrl = null
          val account = googleSignInAccount?.account?.name ?: return@setFragmentResultListener
          checkFesServerViewModel.checkFesServerAvailability(account)
        }

        REQUEST_CODE_RETRY_GET_DOMAIN_ORG_RULES -> if (result == TwoWayDialogFragment.RESULT_OK) {
          val account = googleSignInAccount?.account?.name ?: return@setFragmentResultListener
          val idToken = googleSignInAccount?.idToken ?: return@setFragmentResultListener
          domainOrgRulesViewModel.fetchOrgRules(
            account = account,
            idToken = idToken,
            fesUrl = fesUrl
          )
        }

        REQUEST_CODE_RETRY_FETCH_PRV_KEYS_VIA_EKM -> if (result == TwoWayDialogFragment.RESULT_OK) {
          val idToken = googleSignInAccount?.idToken ?: return@setFragmentResultListener
          orgRules?.let { ekmViewModel.fetchPrvKeys(it, idToken) }
        }
      }
    }
  }

  private fun subscribeCreateOrImportPrivateKeyDuringSetup() {
    setFragmentResultListener(
      CreateOrImportPrivateKeyDuringSetupFragment.REQUEST_KEY_PRIVATE_KEYS
    ) { _, bundle ->
      @CreateOrImportPrivateKeyDuringSetupFragment.Result val result =
        bundle.getInt(CreateOrImportPrivateKeyDuringSetupFragment.KEY_STATE)

      val keys =
        bundle.getParcelableArrayListViaExt(CreateOrImportPrivateKeyDuringSetupFragment.KEY_PRIVATE_KEYS)
          ?: emptyList<PgpKeyDetails>()

      val account = bundle.getParcelableViaExt<AccountEntity>(
        CreateOrImportPrivateKeyDuringSetupFragment.KEY_ACCOUNT
      )

      when (result) {
        CreateOrImportPrivateKeyDuringSetupFragment.Result.HANDLE_RESOLVED_KEYS -> {
          handleUnlockedKeys(account, keys)
        }

        CreateOrImportPrivateKeyDuringSetupFragment.Result.HANDLE_CREATED_KEY -> {
          val pgpKeyDetails = keys.firstOrNull() ?: return@setFragmentResultListener
          account ?: return@setFragmentResultListener
          privateKeysViewModel.doAdditionalActionsAfterPrivateKeyCreation(
            account,
            pgpKeyDetails,
            googleSignInAccount?.idToken
          )
        }

        CreateOrImportPrivateKeyDuringSetupFragment.Result.USE_ANOTHER_ACCOUNT -> {
          this.googleSignInAccount = null
          showContent()
        }
      }
    }
  }

  private fun observeOnResultLiveData() {
    getNavigationResult<kotlin.Result<*>>(RecheckProvidedPassphraseFragment.KEY_ACCEPTED_PASSPHRASE_RESULT) {
      if (it.isSuccess) {
        val passphrase = it.getOrNull() as? CharArray ?: return@getNavigationResult
        privateKeysViewModel.protectPrivateKeys(importCandidates, Passphrase(passphrase))
      }
    }
  }

  private fun onFetchKeysCompleted(keyDetailsList: List<PgpKeyDetails>?) {
    if (keyDetailsList.isNullOrEmpty()) {
      getTempAccount()?.let {
        requireContext().startService(
          Intent(
            requireContext(),
            CheckClipboardToFindKeyService::class.java
          )
        )
        navController?.navigate(
          MainSignInFragmentDirections
            .actionMainSignInFragmentToCreateOrImportPrivateKeyDuringSetupFragment(
              accountEntity = it, isShowAnotherAccountBtnEnabled = true
            )
        )
      }
    } else {
      navController?.navigate(
        MainSignInFragmentDirections
          .actionMainSignInFragmentToCheckKeysFragment(
            privateKeys = keyDetailsList.toTypedArray(),
            sourceType = KeyImportDetails.SourceType.EMAIL,
            positiveBtnTitle = getString(R.string.continue_),
            negativeBtnTitle = getString(R.string.use_another_account),
            initSubTitlePlurals = R.plurals.found_backup_of_your_account_key
          )
      )
    }
  }

  private fun initEnterpriseViewModels() {
    initCheckFesServerViewModel()
    initDomainOrgRulesViewModel()
    initEkmViewModel()
  }

  private fun initCheckFesServerViewModel() {
    checkFesServerViewModel.checkFesServerLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely()
          showProgress(progressMsg = it.progressMsg)
        }

        Result.Status.SUCCESS -> {
          if ("enterprise-server" == it.data?.service) {
            googleSignInAccount?.account?.name?.let { account ->
              val domain = EmailUtil.getDomain(account)
              val idToken = googleSignInAccount?.idToken ?: return@let
              fesUrl = GeneralUtil.generateFesUrl(domain)
              domainOrgRulesViewModel.fetchOrgRules(
                account = account,
                idToken = idToken,
                fesUrl = fesUrl
              )
            }
          } else {
            continueBasedOnFlavorSettings()
          }

          checkFesServerViewModel.checkFesServerLiveData.value = Result.none()
          countingIdlingResource?.decrementSafely()
        }

        Result.Status.ERROR -> {
          checkFesServerViewModel.checkFesServerLiveData.value = Result.none()
          showDialogWithRetryButton(it, REQUEST_CODE_RETRY_CHECK_FES_AVAILABILITY)
          countingIdlingResource?.decrementSafely()
        }

        Result.Status.EXCEPTION -> {
          when (it.exception) {
            is CommonConnectionException -> {
              if (it.exception.hasInternetAccess == true) {
                continueBasedOnFlavorSettings()
              } else {
                showDialogWithRetryButton(
                  getString(R.string.no_connection_or_server_is_not_reachable),
                  REQUEST_CODE_RETRY_CHECK_FES_AVAILABILITY
                )
              }
            }

            is com.flowcrypt.email.util.exception.ApiException -> {
              when (it.exception.apiError?.code) {
                HttpURLConnection.HTTP_NOT_FOUND -> {
                  continueWithRegularFlow()
                }

                else -> {
                  continueBasedOnFlavorSettings()
                }
              }
            }

            is SSLException -> {
              if (BuildConfig.FLAVOR == Constants.FLAVOR_NAME_ENTERPRISE) {
                showDialogWithRetryButton(it, REQUEST_CODE_RETRY_CHECK_FES_AVAILABILITY)
              } else {
                continueWithRegularFlow()
              }
            }

            else -> {
              showDialogWithRetryButton(it, REQUEST_CODE_RETRY_CHECK_FES_AVAILABILITY)
            }
          }

          checkFesServerViewModel.checkFesServerLiveData.value = Result.none()
          countingIdlingResource?.decrementSafely()
        }
        else -> {}
      }
    }
  }

  private fun continueBasedOnFlavorSettings() {
    if (BuildConfig.FLAVOR == Constants.FLAVOR_NAME_ENTERPRISE) {
      /*
       here we actually need to decide if we should show error or proceed with
       regular setup flow based on exact customers that will skip to regular setup flow,
       and the rest will be shown error.
      */
      continueWithRegularFlow()
    } else {
      continueWithRegularFlow()
    }
  }

  private fun continueWithRegularFlow() {
    val idToken = googleSignInAccount?.idToken
    val account = googleSignInAccount?.account?.name

    if (account != null && idToken != null) {
      domainOrgRulesViewModel.fetchOrgRules(account, idToken)
    } else {
      showContent()
      askUserToReLogin()
    }
  }

  private fun initDomainOrgRulesViewModel() {
    domainOrgRulesViewModel.domainOrgRulesLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely()
          showProgress(progressMsg = it.progressMsg)
        }

        Result.Status.SUCCESS -> {
          val idToken = googleSignInAccount?.idToken
          orgRules = (it.data as? DomainOrgRulesResponse)?.orgRules
            ?: (it.data as? ClientConfigurationResponse)?.orgRules

          if (idToken != null) {
            orgRules?.let { fetchedOrgRules -> ekmViewModel.fetchPrvKeys(fetchedOrgRules, idToken) }
          } else {
            showContent()
            askUserToReLogin()
          }
          domainOrgRulesViewModel.domainOrgRulesLiveData.value = Result.none()
          countingIdlingResource?.decrementSafely()
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showDialogWithRetryButton(it, REQUEST_CODE_RETRY_GET_DOMAIN_ORG_RULES)
          domainOrgRulesViewModel.domainOrgRulesLiveData.value = Result.none()
          countingIdlingResource?.decrementSafely()
        }

        else -> {}
      }
    }
  }

  private fun initEkmViewModel() {
    ekmViewModel.ekmLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely()
          showProgress(progressMsg = it.progressMsg)
        }

        Result.Status.SUCCESS -> {
          showContent()
          importCandidates.clear()
          importCandidates.addAll(it?.data?.pgpKeyDetailsList ?: emptyList())
          navController?.navigate(
            MainSignInFragmentDirections
              .actionMainSignInFragmentToCheckPassphraseStrengthFragment(
                popBackStackIdIfSuccess = R.id.mainSignInFragment,
                title = getString(R.string.set_up_flow_crypt, getString(R.string.app_name))
              )
          )
          ekmViewModel.ekmLiveData.value = Result.none()
          countingIdlingResource?.decrementSafely()
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showContent()
          when (it.exception) {
            is EkmNotSupportedException -> {
              onSignSuccess(googleSignInAccount)
            }

            is UnsupportedOrgRulesException -> {
              showInfoDialog(
                dialogTitle = "",
                dialogMsg = getString(
                  R.string.combination_of_org_rules_is_not_supported,
                  it.exception.message
                ),
                isCancelable = true
              )
            }

            else -> {
              showDialogWithRetryButton(it, REQUEST_CODE_RETRY_FETCH_PRV_KEYS_VIA_EKM)
            }
          }
          ekmViewModel.ekmLiveData.value = Result.none()
          countingIdlingResource?.decrementSafely()
        }

        else -> {}
      }
    }
  }

  private fun initProtectPrivateKeysLiveData() {
    privateKeysViewModel.protectPrivateKeysLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely()
          showProgress(getString(R.string.processing))
        }

        Result.Status.SUCCESS -> {
          importCandidates.clear()
          importCandidates.addAll(it.data ?: emptyList())
          importCandidates.forEach { pgpKeyDetails ->
            pgpKeyDetails.passphraseType = KeyEntity.PassphraseType.RAM
          }
          getTempAccount()?.let { account ->
            accountViewModel.addNewAccount(account)
          }
          countingIdlingResource?.decrementSafely()
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showContent()
          showInfoDialog(
            dialogTitle = "",
            dialogMsg = it.exceptionMsg,
            isCancelable = true
          )
          countingIdlingResource?.decrementSafely()
        }

        else -> {}
      }
    }
  }

  private fun showDialogWithRetryButton(it: Result<ApiResponse>, requestCode: Int) {
    showContent()
    showDialogWithRetryButton(it.exceptionMsg, requestCode)
  }

  private fun showDialogWithRetryButton(errorMsg: String, requestCode: Int) {
    showTwoWayDialog(
      requestCode = requestCode,
      dialogTitle = "",
      dialogMsg = errorMsg,
      positiveButtonTitle = getString(R.string.retry),
      negativeButtonTitle = getString(R.string.cancel),
      isCancelable = true
    )
  }

  private fun askUserToReLogin() {
    showInfoDialog(
      dialogTitle = "",
      dialogMsg = getString(R.string.please_login_again_to_continue),
      isCancelable = true
    )
  }

  private fun handleUnlockedKeys(accountEntity: AccountEntity?, keys: List<PgpKeyDetails>) {
    if (keys.isEmpty()) {
      showContent()
      showInfoSnackbar(msgText = getString(R.string.error_no_keys))
    } else {
      importCandidates.clear()
      importCandidates.addAll(keys)

      if (accountEntity == null) {
        showContent()
        ExceptionUtil.handleError(NullPointerException("GoogleSignInAccount is null!"))
        toast(R.string.error_occurred_try_again_later)
      } else {
        accountViewModel.addNewAccount(accountEntity)
      }
    }
  }

  companion object {
    private const val REQUEST_CODE_RETRY_GET_DOMAIN_ORG_RULES = 105
    private const val REQUEST_CODE_RETRY_FETCH_PRV_KEYS_VIA_EKM = 106
    private const val REQUEST_CODE_RETRY_CHECK_FES_AVAILABILITY = 107
  }
}
