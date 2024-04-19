/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.databinding.FragmentPrivateKeyDetailsBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.setFragmentResultListenerForTwoWayDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.showInfoDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.showNeedPassphraseDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.showTwoWayDialog
import com.flowcrypt.email.extensions.androidx.fragment.app.toast
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.CheckPrivateKeysViewModel
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeyDetailsViewModel
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.ui.adapter.UserIdListAdapter
import com.flowcrypt.email.ui.adapter.recyclerview.itemdecoration.MarginItemDecoration
import com.flowcrypt.email.util.DateTimeUtil
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.activity.result.contract.CreateCustomDocument
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.material.snackbar.Snackbar
import org.pgpainless.util.Passphrase
import java.io.FileNotFoundException
import java.util.Date

/**
 * This [Fragment] helps to show details about the given key.
 *
 * @author Denys Bondarenko
 */
class PrivateKeyDetailsFragment : BaseFragment<FragmentPrivateKeyDetailsBinding>(),
  ProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentPrivateKeyDetailsBinding.inflate(inflater, container, false)

  private val args by navArgs<PrivateKeyDetailsFragmentArgs>()
  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()
  private val checkPrivateKeysViewModel: CheckPrivateKeysViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CheckPrivateKeysViewModel(requireActivity().application, true) as T
      }
    }
  }
  private val privateKeyDetailsViewModel: PrivateKeyDetailsViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return PrivateKeyDetailsViewModel(args.fingerprint, requireActivity().application) as T
      }
    }
  }

  private val createDocumentActivityResultLauncher =
    registerForActivityResult(CreateCustomDocument(Constants.MIME_TYPE_PGP_KEY)) { uri: Uri? ->
      uri?.let { saveKey(it) }
    }

  private val userIdsAdapter = UserIdListAdapter()

  override val progressView: View?
    get() = binding?.progress?.root
  override val contentView: View?
    get() = binding?.content
  override val statusView: View?
    get() = binding?.status?.root

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    updateViews()
    setupPgpKeyDetailsViewModel()
    setupPrivateKeysViewModel()
    setupCheckPrivateKeysViewModel()
    subscribeToTwoWayDialog()
  }

  override fun onSetupActionBarMenu(menuHost: MenuHost) {
    super.onSetupActionBarMenu(menuHost)
    menuHost.addMenuProvider(object : MenuProvider {
      override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.fragment_key_details, menu)
      }

      override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        if (account?.clientConfiguration?.usesKeyManager() == true) {
          menu.findItem(R.id.menuActionDeleteKey).isEnabled = false
        }
      }

      override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
          R.id.menuActionDeleteKey -> {
            showTwoWayDialog(
              requestCode = REQUEST_CODE_DELETE_KEY_DIALOG,
              dialogTitle = "",
              dialogMsg = requireContext().resources.getQuantityString(
                R.plurals.delete_key_question, 1, 1
              ),
              positiveButtonTitle = getString(android.R.string.ok),
              negativeButtonTitle = getString(android.R.string.cancel)
            )
            true
          }

          else -> false
        }
      }
    }, viewLifecycleOwner, Lifecycle.State.RESUMED)
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    activity?.invalidateOptionsMenu()

    if (account?.clientConfiguration?.usesKeyManager() == true) {
      binding?.btnShowPrKey?.gone()
      binding?.btnUpdatePrivateKey?.gone()
    }
  }

  private fun saveKey(uri: Uri) {
    try {
      GeneralUtil.writeFileFromStringToUri(
        context = requireContext(),
        uri = uri,
        data = requireNotNull(privateKeyDetailsViewModel.getPgpKeyDetails()).publicKey
      )
      toast(R.string.saved, Toast.LENGTH_SHORT)
    } catch (e: Exception) {
      e.printStackTrace()
      var error = if (e.message.isNullOrEmpty()) e.javaClass.simpleName else e.message

      if (e is IllegalStateException) {
        if (e.message != null && e.message!!.startsWith("Already exists")) {
          error = getString(R.string.not_saved_file_already_exists)
          showInfoSnackbar(requireView(), error, Snackbar.LENGTH_LONG)

          try {
            DocumentsContract.deleteDocument(requireContext().contentResolver, uri)
          } catch (fileNotFound: FileNotFoundException) {
            fileNotFound.printStackTrace()
          }

          return
        } else {
          ExceptionUtil.handleError(e)
        }
      } else {
        ExceptionUtil.handleError(e)
      }

      showInfoSnackbar(requireView(), error ?: "")
    }
  }

  private fun initViews() {
    binding?.btnForgetPassphrase?.setOnClickListener {
      privateKeyDetailsViewModel.forgetPassphrase()
    }

    binding?.btnProvidePassphrase?.setOnClickListener {
      showNeedPassphraseDialog()
    }

    binding?.btnShowPubKey?.setOnClickListener {
      navController?.navigate(
        PrivateKeyDetailsFragmentDirections
          .actionPrivateKeyDetailsFragmentToPrivateToPublicKeyDetailsFragment(
            fingerprint = args.fingerprint
          )
      )
    }

    binding?.btnCopyToClipboard?.setOnClickListener {
      val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
      clipboard?.setPrimaryClip(
        ClipData.newPlainText("pubKey", privateKeyDetailsViewModel.getPgpKeyDetails()?.publicKey)
      )
      toast(R.string.copied, Toast.LENGTH_SHORT)
    }
    binding?.btnSaveToFile?.setOnClickListener {
      chooseDest()
    }
    binding?.btnShowPrKey?.setOnClickListener {
      toast(getString(R.string.see_backups_to_save_your_private_keys), Toast.LENGTH_SHORT)
    }

    binding?.btnUpdatePrivateKey?.setOnClickListener {
      account?.let { accountEntity ->
        val passPhrase = privateKeyDetailsViewModel.getPassphrase()
        if (passPhrase == null || passPhrase.isEmpty) {
          toast(getString(R.string.please_provide_passphrase_to_proceed))
          showNeedPassphraseDialog()
        } else {
          val pgpKeyRingDetails = privateKeyDetailsViewModel.getPgpKeyDetails()
          if (pgpKeyRingDetails != null) {
            navController?.navigate(
              PrivateKeyDetailsFragmentDirections
                .actionPrivateKeyDetailsFragmentToUpdatePrivateKeyFragment(
                  accountEntity = accountEntity,
                  existingPgpKeyRingDetails = pgpKeyRingDetails
                )
            )
          }
        }
      }
    }

    initUserIdsRecyclerView()
  }

  private fun initUserIdsRecyclerView() {
    binding?.recyclerViewUserIds?.apply {
      layoutManager = LinearLayoutManager(context)
      addItemDecoration(
        MarginItemDecoration(
          marginTop = resources.getDimensionPixelSize(R.dimen.default_margin_small)
        )
      )
      adapter = userIdsAdapter
    }
  }

  private fun showNeedPassphraseDialog() {
    val pgpKeyRingDetails = privateKeyDetailsViewModel.getPgpKeyDetails() ?: return
    showNeedPassphraseDialog(
      requestKey = REQUEST_KEY_FIX_MISSING_PASSPHRASE,
      fingerprints = listOf(pgpKeyRingDetails.fingerprint)
    )
  }

  private fun updateViews() {
    privateKeyDetailsViewModel.getPgpKeyDetails()?.let { pgpKeyRingDetails ->
      val dateFormat = DateTimeUtil.getPgpDateFormat(context)

      UIUtil.setHtmlTextToTextView(
        getString(
          R.string.template_fingerprint,
          GeneralUtil.doSectionsInText(" ", pgpKeyRingDetails.fingerprint, 4)
        ), binding?.textViewFingerprint
      )

      binding?.textViewPrimaryKeyAlgorithm?.apply {
        val bitStrength =
          if (pgpKeyRingDetails.algo.bits != -1) pgpKeyRingDetails.algo.bits else null
        val algoWithBits = pgpKeyRingDetails.algo.algorithm + (bitStrength?.let { "/$it" } ?: "")
        text = context.getString(R.string.algorithm, algoWithBits)
      }

      binding?.textViewCreationDate?.text = getString(
        R.string.template_created,
        dateFormat.format(Date(pgpKeyRingDetails.created))
      )

      binding?.textViewModificationDate?.text =
        getString(R.string.template_modified, dateFormat.format(pgpKeyRingDetails.lastModified))


      binding?.textViewExpirationDate?.apply {
        text = pgpKeyRingDetails.expiration?.let {
          getString(R.string.expires, dateFormat.format(Date(it)))
        } ?: getString(R.string.expires, getString(R.string.never))
      }

      binding?.textViewUsableForEncryption?.apply {
        text = context.getString(
          R.string.usable_for_encryption, pgpKeyRingDetails.usableForEncryption.toString()
        )

        setTextColor(
          UIUtil.getColor(
            requireContext(),
            if (pgpKeyRingDetails.usableForEncryption) R.color.colorPrimary else R.color.red
          )
        )
      }

      binding?.textViewUsableForSigning?.apply {
        text = context.getString(
          R.string.usable_for_signing, pgpKeyRingDetails.usableForSigning.toString()
        )

        setTextColor(
          UIUtil.getColor(
            requireContext(),
            if (pgpKeyRingDetails.usableForSigning) R.color.colorAccent else R.color.red
          )
        )
      }

      binding?.textViewStatusValue?.apply {
        backgroundTintList = pgpKeyRingDetails.getColorStateListDependsOnStatus(requireContext())
        setCompoundDrawablesWithIntrinsicBounds(pgpKeyRingDetails.getStatusIconResId(), 0, 0, 0)
        text = pgpKeyRingDetails.getStatusText(requireContext())
      }

      userIdsAdapter.submitList(pgpKeyRingDetails.users)

      val passPhrase = privateKeyDetailsViewModel.getPassphrase()
      val passPhraseType = privateKeyDetailsViewModel.getPassphraseType()
      if (passPhrase == null || passPhrase.isEmpty) {
        handlePassphraseNotProvided()
        return
      }

      if (passPhraseType == KeyEntity.PassphraseType.RAM) {
        binding?.btnForgetPassphrase?.visible()
      }
    }
  }

  private fun handlePassphraseNotProvided() {
    binding?.tVPassPhraseVerification?.setTextColor(UIUtil.getColor(requireContext(), R.color.red))
    binding?.tVPassPhraseVerification?.text = getString(R.string.pass_phrase_not_provided)
    binding?.btnForgetPassphrase?.gone()
    binding?.btnProvidePassphrase?.visible()
  }

  private fun chooseDest() {
    createDocumentActivityResultLauncher.launch(
      "0x" + requireNotNull(privateKeyDetailsViewModel.getPgpKeyDetails()).fingerprint + ".asc"
    )
  }

  private fun setupPgpKeyDetailsViewModel() {
    privateKeyDetailsViewModel.pgpKeyRingDetailsLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely(this@PrivateKeyDetailsFragment)
          showProgress()
        }

        Result.Status.SUCCESS -> {
          if (it.data == null) {
            toast(getString(R.string.no_details_about_given_key))
            navController?.navigateUp()
          } else {
            updateViews()
            matchPassphrase(it.data)
          }
          showContent()
          countingIdlingResource?.decrementSafely(this@PrivateKeyDetailsFragment)
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showContent()
          countingIdlingResource?.decrementSafely(this@PrivateKeyDetailsFragment)
        }

        else -> {}
      }
    }
  }

  private fun matchPassphrase(pgpKeyRingDetails: PgpKeyRingDetails) {
    val passPhrase = privateKeyDetailsViewModel.getPassphrase() ?: return
    if (passPhrase.isEmpty) return
    checkPrivateKeysViewModel.checkKeys(
      keys = listOf(
        pgpKeyRingDetails.copy(passphraseType = privateKeyDetailsViewModel.getPassphraseType())
      ),
      passphrase = passPhrase
    )
  }

  private fun setupPrivateKeysViewModel() {
    privateKeysViewModel.deleteKeysLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely(this@PrivateKeyDetailsFragment)
        }

        Result.Status.SUCCESS -> {
          privateKeysViewModel.deleteKeysLiveData.value = Result.none()
          navController?.navigateUp()
          countingIdlingResource?.decrementSafely(this@PrivateKeyDetailsFragment)
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showInfoDialog(
            dialogMsg = it.exception?.message ?: it.exception?.javaClass?.simpleName
            ?: ("Couldn't delete a key with fingerprint =" +
                " {${privateKeyDetailsViewModel.getPgpKeyDetails()?.fingerprint ?: ""}}")
          )
          countingIdlingResource?.decrementSafely(this@PrivateKeyDetailsFragment)
          privateKeysViewModel.deleteKeysLiveData.value = Result.none()
        }

        else -> {}
      }
    }
  }

  private fun setupCheckPrivateKeysViewModel() {
    checkPrivateKeysViewModel.checkPrvKeysLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource?.incrementSafely(this@PrivateKeyDetailsFragment)
        }

        Result.Status.SUCCESS -> {
          val checkResult = it.data?.firstOrNull()
          val verificationMsg: String?
          if (checkResult != null) {
            if (checkResult.pgpKeyRingDetails.isPrivate) {
              if (checkResult.e == null) {
                binding?.tVPassPhraseVerification?.setTextColor(
                  UIUtil.getColor(requireContext(), R.color.colorPrimaryLight)
                )
                verificationMsg = getString(R.string.stored_pass_phrase_matched)
                if (privateKeyDetailsViewModel.getPassphraseType() == KeyEntity.PassphraseType.RAM) {
                  val existedPassphrase = privateKeyDetailsViewModel.getPassphrase()
                  if (existedPassphrase == null || existedPassphrase.isEmpty) {
                    privateKeyDetailsViewModel.updatePassphrase(
                      Passphrase.fromPassword(checkResult.passphrase)
                    )
                  }
                  binding?.btnForgetPassphrase?.visible()
                  binding?.btnProvidePassphrase?.gone()
                }
              } else {
                if (privateKeyDetailsViewModel.getPassphraseType() == KeyEntity.PassphraseType.RAM) {
                  toast(R.string.password_is_incorrect)
                  verificationMsg = getString(R.string.pass_phrase_not_provided)
                } else {
                  verificationMsg = getString(R.string.stored_pass_phrase_mismatch)
                  binding?.tVPassPhraseVerification?.setTextColor(
                    UIUtil.getColor(requireContext(), R.color.red)
                  )
                }
              }
            } else verificationMsg = getString(R.string.not_private_key)
          } else {
            verificationMsg = getString(R.string.could_not_check_pass_phrase)
            binding?.tVPassPhraseVerification?.setTextColor(
              UIUtil.getColor(requireContext(), R.color.red)
            )
          }

          binding?.tVPassPhraseVerification?.text = verificationMsg
          countingIdlingResource?.decrementSafely(this@PrivateKeyDetailsFragment)
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showInfoDialog(
            dialogMsg = it.exception?.message
              ?: it.exception?.javaClass?.simpleName
              ?: getString(R.string.could_not_check_pass_phrase)
          )
          countingIdlingResource?.decrementSafely(this@PrivateKeyDetailsFragment)
        }

        else -> {}
      }
    }
  }

  private fun subscribeToTwoWayDialog() {
    setFragmentResultListenerForTwoWayDialog { _, bundle ->
      val requestCode = bundle.getInt(TwoWayDialogFragment.KEY_REQUEST_CODE)
      val result = bundle.getInt(TwoWayDialogFragment.KEY_RESULT)

      when (requestCode) {
        REQUEST_CODE_DELETE_KEY_DIALOG -> if (result == TwoWayDialogFragment.RESULT_OK) {
          privateKeyDetailsViewModel.getPgpKeyDetails()?.let {
            account?.let { accountEntity ->
              privateKeysViewModel.deleteKeys(
                accountEntity,
                listOf(it)
              )
            }
          }
        }
      }
    }
  }

  companion object {
    private const val REQUEST_CODE_DELETE_KEY_DIALOG = 100
    private val REQUEST_KEY_FIX_MISSING_PASSPHRASE = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_FIX_MISSING_PASSPHRASE",
      PrivateKeyDetailsFragment::class.java
    )
  }
}
