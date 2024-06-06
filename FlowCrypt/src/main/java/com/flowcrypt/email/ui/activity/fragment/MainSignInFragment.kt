/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.NavDirections
import com.flowcrypt.email.BuildConfig
import com.flowcrypt.email.Constants
import com.flowcrypt.email.NavGraphDirections
import com.flowcrypt.email.R
import com.flowcrypt.email.api.email.EmailUtil
import com.flowcrypt.email.api.retrofit.ApiClientRepository
import com.flowcrypt.email.api.retrofit.response.base.ApiResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.ClientConfiguration
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.databinding.FragmentMainSignInBinding
import com.flowcrypt.email.extensions.android.os.getParcelableArrayListViaExt
import com.flowcrypt.email.extensions.android.os.getParcelableViaExt
import com.flowcrypt.email.extensions.android.os.getSerializableViaExt
import com.flowcrypt.email.extensions.androidx.fragment.app.countingIdlingResource
import com.flowcrypt.email.extensions.androidx.fragment.app.getNavigationResult
import com.flowcrypt.email.extensions.androidx.fragment.app.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.setFragmentResultListenerForTwoWayDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.showFeedbackFragment
import com.flowcrypt.email.extensions.androidx.fragment.app.showInfoDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.showTwoWayDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.toast
import com.flowcrypt.email.extensions.androidx.navigation.navigateSafe
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.exceptionMsg
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.java.lang.printStackTraceIfDebugOnly
import com.flowcrypt.email.extensions.java.lang.showDialogWithErrorDetails
import com.flowcrypt.email.jetpack.viewmodel.CheckCustomerUrlFesServerViewModel
import com.flowcrypt.email.jetpack.viewmodel.ClientConfigurationViewModel
import com.flowcrypt.email.jetpack.viewmodel.EkmViewModel
import com.flowcrypt.email.jetpack.viewmodel.SignInWithGoogleViewModel
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.model.PgpKeyRingDetails
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
import com.flowcrypt.email.util.exception.UnsupportedClientConfigurationException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.snackbar.Snackbar
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.sun.mail.util.MailConnectException
import org.pgpainless.util.Passphrase
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import javax.net.ssl.SSLException

/**
 * @author Denys Bondarenko
 */
class MainSignInFragment : BaseSingInFragment<FragmentMainSignInBinding>() {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentMainSignInBinding.inflate(inflater, container, false)

  private var cachedClientConfiguration: ClientConfiguration? = null
  private var cachedBaseFesUrlPath: String? = null

  private val checkCustomerUrlFesServerViewModel: CheckCustomerUrlFesServerViewModel by viewModels()
  private val clientConfigurationViewModel: ClientConfigurationViewModel by viewModels()
  private val ekmViewModel: EkmViewModel by viewModels()
  private val signInWithGoogleViewModel: SignInWithGoogleViewModel by viewModels()
  private var useStartTlsForSmtp = false

  private val forActivityResultSignIn = registerForActivityResult(
    ActivityResultContracts.StartIntentSenderForResult()
  ) { result: ActivityResult ->
    if (result.resultCode == Activity.RESULT_OK) {
      onUserAuthorizedToGmailApi()
    } else {
      signInWithGoogleViewModel.resetCachedAuthenticateState()
    }
  }

  private val forActivityResultSignInError = registerForActivityResult(
    ActivityResultContracts.StartActivityForResult()
  ) { result: ActivityResult ->
    if (result.resultCode == Activity.RESULT_OK) {
      binding?.buttonSignInWithGmail?.callOnClick()
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

  private val cachedGoogleIdTokenCredential: GoogleIdTokenCredential?
    get() = signInWithGoogleViewModel.googleIdTokenCredentialStateFlow.value.data

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()

    subscribeToCheckAccountSettingsAndSearchBackups()
    subscribeToCheckPrivateKeys()
    subscribeToTwoWayDialog()
    subscribeCreateOrImportPrivateKeyDuringSetup()
    observeOnResultLiveData()

    initAddNewAccountLiveData()
    initEnterpriseViewModels()
    initPrivateKeysViewModel()
    initProtectPrivateKeysLiveData()
    initSignInWithGoogleViewModel()
  }

  override fun getTempAccount(): AccountEntity? {
    val sharedTenantFesBaseUrlPath = GeneralUtil.genBaseFesUrlPath(useCustomerFesUrl = false)
    return cachedGoogleIdTokenCredential?.let {
      AccountEntity(
        googleIdTokenCredential = it,
        clientConfiguration = cachedClientConfiguration,
        useCustomerFesUrl = cachedBaseFesUrlPath?.isNotEmpty() == true &&
            cachedBaseFesUrlPath != sharedTenantFesBaseUrlPath,
        useStartTlsForSmtp = useStartTlsForSmtp,
      )
    }
  }

  override fun onAccountAdded(accountEntity: AccountEntity) {
    privateKeysViewModel.encryptAndSaveKeysToDatabase(
      accountEntity = accountEntity,
      keys = importCandidates
    )
  }

  override fun onAdditionalActionsAfterPrivateKeyCreationCompleted(
    accountEntity: AccountEntity,
    pgpKeyRingDetails: PgpKeyRingDetails
  ) {
    handleUnlockedKeys(accountEntity, listOf(pgpKeyRingDetails))
  }

  override fun onAdditionalActionsAfterPrivateKeyImportingCompleted(
    accountEntity: AccountEntity,
    keys: List<PgpKeyRingDetails>
  ) {
    handleUnlockedKeys(accountEntity, keys)
  }

  private fun initViews() {
    binding?.buttonSignInWithGmail?.setOnClickListener {
      cachedBaseFesUrlPath = null
      cachedClientConfiguration = null
      importCandidates.clear()
      signInWithGoogleViewModel.authenticateUser()
    }

    binding?.buttonOtherEmailProvider?.setOnClickListener {
      cachedBaseFesUrlPath = null
      cachedClientConfiguration = null
      navController?.navigateSafe(
        R.id.mainSignInFragment,
        MainSignInFragmentDirections.actionMainSignInFragmentToAddOtherAccountFragment()
      )
    }

    binding?.buttonPrivacy?.setOnClickListener {
      GeneralUtil.openCustomTab(requireContext(), Constants.FLOWCRYPT_PRIVACY_URL)
    }

    binding?.buttonTerms?.setOnClickListener {
      GeneralUtil.openCustomTab(requireContext(), Constants.FLOWCRYPT_TERMS_URL)
    }

    binding?.buttonSecurity?.setOnClickListener {
      navController?.navigateSafe(
        R.id.mainSignInFragment,
        NavGraphDirections.actionGlobalHtmlViewFromAssetsRawFragment(
          title = getString(R.string.security),
          resourceIdAsString = "html/security.htm"
        )
      )
    }

    binding?.buttonHelp?.setOnClickListener {
      showFeedbackFragment()
    }
  }

  private fun handleAuthentication(googleIdTokenCredential: GoogleIdTokenCredential) {
    val account = googleIdTokenCredential.id
    cachedBaseFesUrlPath = GeneralUtil.genBaseFesUrlPath(useCustomerFesUrl = false)

    val publicEmailDomains = EmailUtil.getPublicEmailDomains()
    val domain = EmailUtil.getDomain(account)
    if (domain in publicEmailDomains) {
      @Suppress("KotlinConstantConditions")
      if (BuildConfig.FLAVOR == Constants.FLAVOR_NAME_ENTERPRISE) {
        signInWithGoogleViewModel.resetCachedAuthenticateState()
        showInfoDialog(
          dialogTitle = "",
          dialogMsg = getString(
            R.string.enterprise_does_not_support_pub_domains,
            getString(R.string.app_name),
            domain
          ),
          isCancelable = true
        )
      } else {
        authorizeUserToGmailApi()
      }
    } else {
      authorizeUserToGmailApi()
    }
  }

  private fun authorizeUserToGmailApi() {
    val authorizationRequest: AuthorizationRequest = AuthorizationRequest.builder()
      .setRequestedScopes(listOf(Scope(Constants.SCOPE_MAIL_GOOGLE_COM))).build()
    Identity.getAuthorizationClient(requireContext())
      .authorize(authorizationRequest)
      .addOnSuccessListener { authorizationResult ->
        if (authorizationResult.hasResolution()) {
          // Access needs to be granted by the user.
          // At this stage the grant access to Gmail API screen should be displayed
          val pendingIntent = authorizationResult.pendingIntent
          if (pendingIntent != null) {
            try {
              forActivityResultSignIn.launch(
                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
              )
            } catch (e: Exception) {
              e.showDialogWithErrorDetails(this)
            }
          } else {
            showInfoDialog(
              dialogTitle = "",
              dialogMsg = getString(
                R.string.error_occurred_with_details_please_try_again,
                "pendingIntent == null"
              )
            )
          }
        } else {
          // Access already granted, continue with user action
          onUserAuthorizedToGmailApi()
        }
      }.addOnFailureListener { exception ->
        exception.showDialogWithErrorDetails(this)
      }
  }

  private fun onUserAuthorizedToGmailApi() {
    val account = cachedGoogleIdTokenCredential?.id?.lowercase()
    val idToken = cachedGoogleIdTokenCredential?.idToken

    if (idToken == null) {
      IllegalStateException("idToken == null").showDialogWithErrorDetails(this)
      return
    }

    if (account != null) {
      val publicEmailDomains = EmailUtil.getPublicEmailDomains()
      val domain = EmailUtil.getDomain(account)
      if (domain in publicEmailDomains) {
        clientConfigurationViewModel.fetchClientConfiguration(
          idToken = idToken,
          baseFesUrlPath = GeneralUtil.genBaseFesUrlPath(useCustomerFesUrl = false),
          domain = domain
        )
      } else {
        checkCustomerUrlFesServerViewModel.checkServerAvailability(account)
      }
    } else {
      IllegalStateException("account == null").showDialogWithErrorDetails(this)
    }
  }

  private fun onSignSuccess(googleIdTokenCredential: GoogleIdTokenCredential?) {
    val existedAccount = existingAccounts.firstOrNull {
      it.email.equals(googleIdTokenCredential?.id, ignoreCase = true)
    }

    if (existedAccount == null) {
      getTempAccount()?.let {
        if (cachedClientConfiguration?.flags?.firstOrNull { rule -> rule == ClientConfiguration.ConfigurationProperty.NO_PRV_BACKUP } != null) {
          requireContext().startService(
            Intent(requireContext(), CheckClipboardToFindKeyService::class.java)
          )
          navController?.navigate(
            MainSignInFragmentDirections
              .actionMainSignInFragmentToCreateOrImportPrivateKeyDuringSetupFragment(
                requestKey = REQUEST_KEY_PRIVATE_KEYS,
                accountEntity = it,
                isShowAnotherAccountBtnEnabled = true
              )
          )
        } else {
          navController?.navigate(
            MainSignInFragmentDirections.actionMainSignInFragmentToAuthorizeAndSearchBackupsFragment(
              requestKey = REQUEST_KEY_CHECK_ACCOUNT_SETTINGS_AND_SEARCH_BACKUPS, account = it
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

  private fun subscribeToCheckAccountSettingsAndSearchBackups() {
    setFragmentResultListener(REQUEST_KEY_CHECK_ACCOUNT_SETTINGS_AND_SEARCH_BACKUPS) { _, bundle ->
      when (bundle.getInt(AuthorizeAndSearchBackupsFragment.KEY_RESULT_TYPE)) {
        AuthorizeAndSearchBackupsFragment.RESULT_TYPE_SETTINGS -> {
          val result: Result<*>? =
            bundle.getSerializableViaExt(AuthorizeAndSearchBackupsFragment.KEY_CHECK_ACCOUNT_SETTINGS_RESULT) as? Result<*>
          result?.let { handleCheckSettingsResult(it) }
        }

        AuthorizeAndSearchBackupsFragment.RESULT_TYPE_BACKUPS -> {
          val result: Result<*>? =
            bundle.getSerializableViaExt(AuthorizeAndSearchBackupsFragment.KEY_PRIVATE_KEY_BACKUPS_RESULT) as? Result<*>
          result?.let { handleSearchBackupsResult(it) }
        }
      }
    }
  }

  private fun handleSearchBackupsResult(
    result: Result<*>
  ) {
    when (result.status) {
      Result.Status.SUCCESS -> {
        @Suppress("UNCHECKED_CAST")
        onFetchKeysCompleted(result.data as ArrayList<PgpKeyRingDetails>?)
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

      else -> {}
    }
  }

  private fun handleCheckSettingsResult(result: Result<*>) {
    when (result.status) {
      Result.Status.ERROR, Result.Status.EXCEPTION -> {
        showContent()
        val exception = result.exception ?: return
        val original = result.exception.cause

        if (original is MailConnectException && !useStartTlsForSmtp) {
          useStartTlsForSmtp = true
          onSignSuccess(cachedGoogleIdTokenCredential)
          return
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
          return
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

      else -> {}
    }
  }

  private fun subscribeToCheckPrivateKeys() {
    setFragmentResultListener(REQUEST_KEY_CHECK_PRIVATE_KEYS) { _, bundle ->
      val keys = bundle.getParcelableArrayListViaExt(
        CheckKeysFragment.KEY_UNLOCKED_PRIVATE_KEYS
      ) ?: emptyList<PgpKeyRingDetails>()

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
    setFragmentResultListenerForTwoWayDialog { _, bundle ->
      val requestCode = bundle.getInt(TwoWayDialogFragment.KEY_REQUEST_CODE)
      val result = bundle.getInt(TwoWayDialogFragment.KEY_RESULT)

      when (requestCode) {
        REQUEST_CODE_RETRY_CHECK_FES_AVAILABILITY -> if (result == TwoWayDialogFragment.RESULT_OK) {
          val account = cachedGoogleIdTokenCredential?.id
            ?: return@setFragmentResultListenerForTwoWayDialog
          checkCustomerUrlFesServerViewModel.checkServerAvailability(account)
        }

        REQUEST_CODE_RETRY_GET_CLIENT_CONFIGURATION -> if (result == TwoWayDialogFragment.RESULT_OK) {
          val idToken =
            cachedGoogleIdTokenCredential?.idToken
              ?: return@setFragmentResultListenerForTwoWayDialog
          val account = cachedGoogleIdTokenCredential?.id
            ?: return@setFragmentResultListenerForTwoWayDialog
          val domain = EmailUtil.getDomain(account)
          val baseFesUrlPath =
            cachedBaseFesUrlPath ?: return@setFragmentResultListenerForTwoWayDialog
          clientConfigurationViewModel.fetchClientConfiguration(
            idToken = idToken,
            baseFesUrlPath = baseFesUrlPath,
            domain = domain
          )
        }

        REQUEST_CODE_RETRY_FETCH_PRV_KEYS_VIA_EKM -> if (result == TwoWayDialogFragment.RESULT_OK) {
          val idToken =
            cachedGoogleIdTokenCredential?.idToken
              ?: return@setFragmentResultListenerForTwoWayDialog
          cachedClientConfiguration?.let { ekmViewModel.fetchPrvKeys(it, idToken) }
        }
      }
    }
  }

  private fun subscribeCreateOrImportPrivateKeyDuringSetup() {
    setFragmentResultListener(REQUEST_KEY_PRIVATE_KEYS) { _, bundle ->
      @CreateOrImportPrivateKeyDuringSetupFragment.Result val result =
        bundle.getInt(CreateOrImportPrivateKeyDuringSetupFragment.KEY_STATE)

      val keys =
        bundle.getParcelableArrayListViaExt(CreateOrImportPrivateKeyDuringSetupFragment.KEY_PRIVATE_KEYS)
          ?: emptyList<PgpKeyRingDetails>()

      val account = bundle.getParcelableViaExt<AccountEntity>(
        CreateOrImportPrivateKeyDuringSetupFragment.KEY_ACCOUNT
      )

      when (result) {
        CreateOrImportPrivateKeyDuringSetupFragment.Result.HANDLE_RESOLVED_KEYS -> {
          if (account != null && keys.isNotEmpty()) {
            privateKeysViewModel.doAdditionalActionsAfterPrivateKeysImporting(
              accountEntity = account,
              keys = keys
            )
          }
        }

        CreateOrImportPrivateKeyDuringSetupFragment.Result.HANDLE_CREATED_KEY -> {
          if (account != null && keys.isNotEmpty()) {
            privateKeysViewModel.doAdditionalActionsAfterPrivateKeyCreation(
              accountEntity = account,
              keys = keys,
              idToken = cachedGoogleIdTokenCredential?.idToken
            )
          }
        }

        CreateOrImportPrivateKeyDuringSetupFragment.Result.USE_ANOTHER_ACCOUNT -> {
          //this.cachedGoogleIdTokenCredential = null
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

  private fun onFetchKeysCompleted(keyDetailsList: List<PgpKeyRingDetails>?) {
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
              requestKey = REQUEST_KEY_PRIVATE_KEYS,
              accountEntity = it, isShowAnotherAccountBtnEnabled = true
            )
        )
      }
    } else {
      navController?.navigate(
        object : NavDirections {
          override val actionId = R.id.check_keys_graph
          override val arguments = CheckKeysFragmentArgs(
            requestKey = REQUEST_KEY_CHECK_PRIVATE_KEYS,
            privateKeys = keyDetailsList.toTypedArray(),
            sourceType = KeyImportDetails.SourceType.EMAIL,
            positiveBtnTitle = getString(R.string.continue_),
            negativeBtnTitle = getString(R.string.use_another_account),
            initSubTitlePlurals = R.plurals.found_backup_of_your_account_key
          ).toBundle()
        }
      )
    }
  }

  private fun initEnterpriseViewModels() {
    initCheckFesServerViewModel()
    initClientConfigurationViewModel()
    initEkmViewModel()
  }

  private fun initCheckFesServerViewModel() {
    checkCustomerUrlFesServerViewModel.checkFesServerAvailabilityLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely(this@MainSignInFragment)
          showProgress(progressMsg = it.progressMsg)
        }

        Result.Status.SUCCESS -> {
          if (it.data?.service in ApiClientRepository.FES.ALLOWED_SERVICES) {
            cachedGoogleIdTokenCredential?.id?.let { account ->
              val domain = EmailUtil.getDomain(account)
              val idToken = cachedGoogleIdTokenCredential?.idToken ?: return@let
              val baseFesUrlPath = GeneralUtil.genBaseFesUrlPath(
                useCustomerFesUrl = true,
                domain = domain
              )
              cachedBaseFesUrlPath = baseFesUrlPath
              clientConfigurationViewModel.fetchClientConfiguration(
                idToken = idToken,
                baseFesUrlPath = baseFesUrlPath,
                domain = domain
              )
            }
          } else {
            continueBasedOnFlavorSettings(getString(R.string.fes_server_has_wrong_settings))
          }

          checkCustomerUrlFesServerViewModel.checkFesServerAvailabilityLiveData.value =
            Result.none()
          countingIdlingResource?.decrementSafely(this@MainSignInFragment)
        }

        Result.Status.ERROR -> {
          checkCustomerUrlFesServerViewModel.checkFesServerAvailabilityLiveData.value =
            Result.none()
          showDialogWithRetryButton(it, REQUEST_CODE_RETRY_CHECK_FES_AVAILABILITY)
          countingIdlingResource?.decrementSafely(this@MainSignInFragment)
        }

        Result.Status.EXCEPTION -> {
          when (it.exception) {
            is CommonConnectionException -> {
              if (it.exception.hasInternetAccess == true) {
                continueBasedOnFlavorSettings(
                  getString(
                    R.string.check_fes_error_with_retry,
                    it.exceptionMsg
                  )
                )
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
                  continueBasedOnFlavorSettings(
                    getString(
                      R.string.fes_server_error,
                      it.exceptionMsg
                    )
                  )
                }
              }
            }

            is SSLException -> {
              @Suppress("KotlinConstantConditions")
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

          checkCustomerUrlFesServerViewModel.checkFesServerAvailabilityLiveData.value =
            Result.none()
          countingIdlingResource?.decrementSafely(this@MainSignInFragment)
        }

        else -> {}
      }
    }
  }

  private fun initSignInWithGoogleViewModel() {
    launchAndRepeatWithViewLifecycle {
      signInWithGoogleViewModel.googleIdTokenCredentialStateFlow.collect {
        when (it.status) {
          Result.Status.SUCCESS -> {
            if (it.data != null) {
              handleAuthentication(it.data)
            } else {
              showInfoDialog(
                dialogTitle = "",
                dialogMsg = getString(
                  R.string.error_occurred_with_details_please_try_again,
                  "data == null"
                )
              )
            }
            signInWithGoogleViewModel.cacheAuthenticateState()
          }

          Result.Status.EXCEPTION -> {
            when {
              it.exception is GetCredentialCancellationException
                  && "android.credentials.GetCredentialException.TYPE_USER_CANCELED" == it.exception.type -> {
                //do nothing
              }

              it.exception is AccountAlreadyAddedException -> {
                toast(it.exception.message)
              }

              else -> showInfoDialog(
                dialogTitle = "",
                dialogMsg = it.exceptionMsg
              )
            }

            (it.exception as? Exception)?.printStackTraceIfDebugOnly()
            signInWithGoogleViewModel.resetCachedAuthenticateState()
          }

          else -> {}
        }
      }
    }
  }

  private fun continueBasedOnFlavorSettings(errorMsg: String) {
    @Suppress("KotlinConstantConditions")
    if (BuildConfig.FLAVOR == Constants.FLAVOR_NAME_ENTERPRISE) {
      showDialogWithRetryButton(
        errorMsg,
        REQUEST_CODE_RETRY_CHECK_FES_AVAILABILITY
      )
    } else {
      continueWithRegularFlow()
    }
  }

  private fun continueWithRegularFlow() {
    val idToken = cachedGoogleIdTokenCredential?.idToken
    val account = cachedGoogleIdTokenCredential?.id
    val baseFesUrlPath = cachedBaseFesUrlPath

    if (idToken != null && account != null && baseFesUrlPath != null) {
      val domain = EmailUtil.getDomain(account)
      clientConfigurationViewModel.fetchClientConfiguration(
        idToken = idToken,
        baseFesUrlPath = baseFesUrlPath,
        domain = domain
      )
    } else {
      showContent()
      askUserToReLogin()
    }
  }

  private fun initClientConfigurationViewModel() {
    clientConfigurationViewModel.clientConfigurationLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely(this@MainSignInFragment)
          showProgress(progressMsg = it.progressMsg)
        }

        Result.Status.SUCCESS -> {
          val idToken = cachedGoogleIdTokenCredential?.idToken
          cachedClientConfiguration = it.data?.clientConfiguration

          if (idToken != null) {
            cachedClientConfiguration?.let { fetchedClientConfiguration ->
              ekmViewModel.fetchPrvKeys(
                fetchedClientConfiguration,
                idToken
              )
            }
          } else {
            showContent()
            askUserToReLogin()
          }
          clientConfigurationViewModel.clientConfigurationLiveData.value = Result.none()
          countingIdlingResource?.decrementSafely(this@MainSignInFragment)
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showDialogWithRetryButton(it, REQUEST_CODE_RETRY_GET_CLIENT_CONFIGURATION)
          clientConfigurationViewModel.clientConfigurationLiveData.value = Result.none()
          countingIdlingResource?.decrementSafely(this@MainSignInFragment)
        }

        else -> {}
      }
    }
  }

  private fun initEkmViewModel() {
    ekmViewModel.ekmLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely(this@MainSignInFragment)
          showProgress(progressMsg = it.progressMsg)
        }

        Result.Status.SUCCESS -> {
          showContent()
          importCandidates.clear()
          importCandidates.addAll(it?.data?.pgpKeyRingDetailsList ?: emptyList())
          navController?.navigate(
            object : NavDirections {
              override val actionId = R.id.pass_phrase_strength_graph
              override val arguments = CheckPassphraseStrengthFragmentArgs(
                popBackStackIdIfSuccess = R.id.mainSignInFragment,
                title = getString(R.string.set_up_flow_crypt, getString(R.string.app_name))
              ).toBundle()
            }
          )
          ekmViewModel.ekmLiveData.value = Result.none()
          countingIdlingResource?.decrementSafely(this@MainSignInFragment)
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showContent()
          when (it.exception) {
            is EkmNotSupportedException -> {
              onSignSuccess(cachedGoogleIdTokenCredential)
            }

            is UnsupportedClientConfigurationException -> {
              showInfoDialog(
                dialogTitle = "",
                dialogMsg = getString(
                  R.string.combination_of_client_configuration_is_not_supported,
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
          countingIdlingResource?.decrementSafely(this@MainSignInFragment)
        }

        else -> {}
      }
    }
  }

  private fun initProtectPrivateKeysLiveData() {
    privateKeysViewModel.protectPrivateKeysLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely(this@MainSignInFragment)
          showProgress(getString(R.string.processing))
        }

        Result.Status.SUCCESS -> {
          importCandidates.clear()
          importCandidates.addAll(it.data ?: emptyList())
          importCandidates.forEach { pgpKeyRingDetails ->
            pgpKeyRingDetails.passphraseType = KeyEntity.PassphraseType.RAM
          }
          getTempAccount()?.let { account ->
            accountViewModel.addNewAccount(account)
          }
          countingIdlingResource?.decrementSafely(this@MainSignInFragment)
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showContent()
          showInfoDialog(
            dialogTitle = "",
            dialogMsg = it.exceptionMsg,
            isCancelable = true
          )
          countingIdlingResource?.decrementSafely(this@MainSignInFragment)
        }

        else -> {}
      }
    }
  }

  private fun showDialogWithRetryButton(it: Result<ApiResponse>, requestCode: Int) {
    showDialogWithRetryButton(it.exceptionMsg, requestCode)
  }

  private fun showDialogWithRetryButton(errorMsg: String, requestCode: Int) {
    showContent()
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

  private fun handleUnlockedKeys(accountEntity: AccountEntity?, keys: List<PgpKeyRingDetails>) {
    if (keys.isEmpty()) {
      showContent()
      showInfoSnackbar(msgText = getString(R.string.error_no_keys))
    } else {
      importCandidates.clear()
      importCandidates.addAll(keys)

      if (accountEntity == null) {
        showContent()
        ExceptionUtil.handleError(NullPointerException("GoogleIdTokenCredential is null!"))
        toast(R.string.error_occurred_try_again_later)
      } else {
        accountViewModel.addNewAccount(accountEntity)
      }
    }
  }

  companion object {
    private val REQUEST_KEY_CHECK_ACCOUNT_SETTINGS_AND_SEARCH_BACKUPS =
      GeneralUtil.generateUniqueExtraKey(
        "REQUEST_KEY_CHECK_ACCOUNT_SETTINGS_AND_SEARCH_BACKUPS",
        MainSignInFragment::class.java
      )

    private val REQUEST_KEY_CHECK_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_CHECK_PRIVATE_KEYS",
      MainSignInFragment::class.java
    )

    private val REQUEST_KEY_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_PRIVATE_KEYS",
      MainSignInFragment::class.java
    )

    private const val REQUEST_CODE_RETRY_GET_CLIENT_CONFIGURATION = 105
    private const val REQUEST_CODE_RETRY_FETCH_PRV_KEYS_VIA_EKM = 106
    private const val REQUEST_CODE_RETRY_CHECK_FES_AVAILABILITY = 107
  }
}
