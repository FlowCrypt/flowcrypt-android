/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.database.entity.KeyEntity
import com.flowcrypt.email.databinding.FragmentPrivateKeyDetailsBinding
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showTwoWayDialog
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.extensions.visible
import com.flowcrypt.email.jetpack.viewmodel.CheckPrivateKeysViewModel
import com.flowcrypt.email.jetpack.viewmodel.PgpKeyDetailsViewModel
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.jetpack.viewmodel.factory.PgpKeyDetailsViewModelFactory
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.material.snackbar.Snackbar
import org.pgpainless.util.Passphrase
import java.io.FileNotFoundException
import java.util.Date

/**
 * This [Fragment] helps to show details about the given key.
 *
 * @author Denis Bondarenko
 * Date: 20.11.18
 * Time: 12:43
 * E-mail: DenBond7@gmail.com
 */
class PrivateKeyDetailsFragment : BaseFragment(), ProgressBehaviour {
  private val args by navArgs<PrivateKeyDetailsFragmentArgs>()
  private var binding: FragmentPrivateKeyDetailsBinding? = null

  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()
  private val checkPrivateKeysViewModel: CheckPrivateKeysViewModel by viewModels()
  private val pgpKeyDetailsViewModel: PgpKeyDetailsViewModel by viewModels {
    PgpKeyDetailsViewModelFactory(args.fingerprint, requireActivity().application)
  }

  override val progressView: View?
    get() = binding?.progress?.root
  override val contentView: View?
    get() = binding?.content
  override val statusView: View?
    get() = binding?.status?.root

  override val contentResourceId: Int = R.layout.fragment_private_key_details

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.fragment_key_details, menu)
  }

  override fun onPrepareOptionsMenu(menu: Menu) {
    super.onPrepareOptionsMenu(menu)
    if (account?.clientConfiguration?.usesKeyManager() == true) {
      menu.findItem(R.id.menuActionDeleteKey).isEnabled = false
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menuActionDeleteKey -> {
        showTwoWayDialog(
          dialogTitle = "",
          dialogMsg = requireContext().resources.getQuantityString(
            R.plurals.delete_key_question, 1, 1
          ),
          positiveButtonTitle = getString(android.R.string.ok),
          negativeButtonTitle = getString(android.R.string.cancel),
          requestCode = REQUEST_CODE_DELETE_KEY_DIALOG
        )
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    binding = FragmentPrivateKeyDetailsBinding.inflate(inflater, container, false)
    return binding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.setTitle(R.string.key_details)
    initViews()
    updateViews()
    setupPgpKeyDetailsViewModel()
    setupPrivateKeysViewModel()
    setupCheckPrivateKeysViewModel()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_GET_URI_FOR_SAVING_KEY -> when (resultCode) {
        Activity.RESULT_OK -> if (data != null && data.data != null) {
          saveKey(data)
        }
      }

      REQUEST_CODE_DELETE_KEY_DIALOG -> {
        when (resultCode) {
          TwoWayDialogFragment.RESULT_OK -> {
            pgpKeyDetailsViewModel.getPgpKeyDetails()?.let {
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

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    activity?.invalidateOptionsMenu()

    if (account?.clientConfiguration?.usesKeyManager() == true) {
      binding?.btnShowPrKey?.gone()
    }
  }

  private fun saveKey(data: Intent) {
    try {
      GeneralUtil.writeFileFromStringToUri(
        context = requireContext(),
        uri = data.data!!,
        data = pgpKeyDetailsViewModel.getPgpKeyDetails()!!.publicKey
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
            data.data?.let {
              DocumentsContract.deleteDocument(
                requireContext().contentResolver,
                it
              )
            }
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
      pgpKeyDetailsViewModel.forgetPassphrase()
      toast(getString(R.string.passphrase_purged_from_memory))
      binding?.eTKeyPassword?.text = null
    }

    binding?.btnUpdatePassphrase?.setOnClickListener {
      UIUtil.hideSoftInput(requireContext(), binding?.eTKeyPassword)
      val typedText = binding?.eTKeyPassword?.text?.toString()
      if (typedText.isNullOrEmpty()) {
        showInfoSnackbar(binding?.eTKeyPassword, getString(R.string.passphrase_must_be_non_empty))
      } else {
        snackBar?.dismiss()
        binding?.eTKeyPassword?.let {
          val passPhrase = Passphrase.fromPassword(typedText)
          val pgpKeyDetails = pgpKeyDetailsViewModel.getPgpKeyDetails() ?: return@let
          checkPrivateKeysViewModel.checkKeys(
            keys = listOf(
              pgpKeyDetails.copy(passphraseType = pgpKeyDetailsViewModel.getPassphraseType())
            ),
            passphrase = passPhrase
          )
        }
      }
    }

    binding?.btnShowPubKey?.setOnClickListener {
      val dialogFragment = InfoDialogFragment.newInstance(
        dialogTitle = "",
        dialogMsg = pgpKeyDetailsViewModel.getPgpKeyDetails()!!.publicKey
      )
      dialogFragment.show(parentFragmentManager, InfoDialogFragment::class.java.simpleName)
    }

    binding?.btnCopyToClipboard?.setOnClickListener {
      val clipboard = context?.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
      clipboard?.setPrimaryClip(
        ClipData.newPlainText("pubKey", pgpKeyDetailsViewModel.getPgpKeyDetails()?.publicKey)
      )
      toast(R.string.copied, Toast.LENGTH_SHORT)
    }
    binding?.btnSaveToFile?.setOnClickListener {
      chooseDest()
    }
    binding?.btnShowPrKey?.setOnClickListener {
      toast(getString(R.string.see_backups_to_save_your_private_keys), Toast.LENGTH_SHORT)
    }
  }

  private fun updateViews() {
    pgpKeyDetailsViewModel.getPgpKeyDetails()?.let { value ->
      UIUtil.setHtmlTextToTextView(
        getString(
          R.string.template_fingerprint,
          GeneralUtil.doSectionsInText(" ", value.fingerprint, 4)
        ), binding?.tVFingerprint
      )

      binding?.tVDate?.text = getString(
        R.string.template_date,
        DateFormat.getMediumDateFormat(context).format(Date(value.created))
      )
      binding?.tVUsers?.text = getString(R.string.template_users, value.getUserIdsAsSingleString())

      val passPhrase = pgpKeyDetailsViewModel.getPassphrase()
      val passPhraseType = pgpKeyDetailsViewModel.getPassphraseType()
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
    binding?.gCheckPassphrase?.visible()
  }

  private fun chooseDest() {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = Constants.MIME_TYPE_PGP_KEY
    intent.putExtra(
      Intent.EXTRA_TITLE,
      "0x" + pgpKeyDetailsViewModel.getPgpKeyDetails()!!.fingerprint + ".asc"
    )
    startActivityForResult(intent, REQUEST_CODE_GET_URI_FOR_SAVING_KEY)
  }

  private fun setupPgpKeyDetailsViewModel() {
    pgpKeyDetailsViewModel.pgpKeyDetailsLiveData.observe(viewLifecycleOwner, {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource.incrementSafely()
          showProgress()
        }

        Result.Status.SUCCESS -> {
          if (it.data == null) {
            toast(getString(R.string.no_details_about_given_key))
            parentFragmentManager.popBackStack()
          } else {
            updateViews()
            matchPassphrase(it.data)
          }
          showContent()
          countingIdlingResource.decrementSafely()
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showContent()
          countingIdlingResource.decrementSafely()
        }
      }
    })
  }

  private fun matchPassphrase(pgpKeyDetails: PgpKeyDetails) {
    val passPhrase = pgpKeyDetailsViewModel.getPassphrase() ?: return
    if (passPhrase.isEmpty) return
    checkPrivateKeysViewModel.checkKeys(
      keys = listOf(
        pgpKeyDetails.copy(passphraseType = pgpKeyDetailsViewModel.getPassphraseType())
      ),
      passphrase = passPhrase
    )
  }

  private fun setupPrivateKeysViewModel() {
    privateKeysViewModel.deleteKeysLiveData.observe(viewLifecycleOwner, {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource.incrementSafely()
        }

        Result.Status.SUCCESS -> {
          privateKeysViewModel.deleteKeysLiveData.value = Result.none()
          parentFragmentManager.popBackStack()
          countingIdlingResource.decrementSafely()
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showInfoDialog(
            dialogMsg = it.exception?.message ?: it.exception?.javaClass?.simpleName
            ?: "Couldn't delete a key with fingerprint =" +
            " {${pgpKeyDetailsViewModel.getPgpKeyDetails()?.fingerprint ?: ""}}"
          )
          countingIdlingResource.decrementSafely()
          privateKeysViewModel.deleteKeysLiveData.value = Result.none()
        }
      }
    })
  }

  private fun setupCheckPrivateKeysViewModel() {
    checkPrivateKeysViewModel.checkPrvKeysLiveData.observe(viewLifecycleOwner, { it ->
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource.incrementSafely()
        }

        Result.Status.SUCCESS -> {
          val checkResult = it.data?.firstOrNull()
          val verificationMsg: String?
          if (checkResult != null) {
            if (checkResult.pgpKeyDetails.isPrivate) {
              if (checkResult.e == null) {
                binding?.tVPassPhraseVerification?.setTextColor(
                  UIUtil.getColor(requireContext(), R.color.colorPrimaryLight)
                )
                verificationMsg = getString(R.string.stored_pass_phrase_matched)
                if (pgpKeyDetailsViewModel.getPassphraseType() == KeyEntity.PassphraseType.RAM) {
                  val existedPassphrase = pgpKeyDetailsViewModel.getPassphrase()
                  if (existedPassphrase == null || existedPassphrase.isEmpty) {
                    pgpKeyDetailsViewModel.updatePassphrase(
                      Passphrase.fromPassword(checkResult.passphrase)
                    )
                  }
                  binding?.btnForgetPassphrase?.visible()
                  binding?.gCheckPassphrase?.gone()
                }
              } else {
                if (pgpKeyDetailsViewModel.getPassphraseType() == KeyEntity.PassphraseType.RAM) {
                  binding?.eTKeyPassword?.requestFocus()
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
          countingIdlingResource.decrementSafely()
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showInfoDialog(
            dialogMsg = it.exception?.message
              ?: it.exception?.javaClass?.simpleName
              ?: getString(R.string.could_not_check_pass_phrase)
          )
          countingIdlingResource.decrementSafely()
        }
      }
    })
  }

  companion object {
    private const val REQUEST_CODE_GET_URI_FOR_SAVING_KEY = 1
    private const val REQUEST_CODE_DELETE_KEY_DIALOG = 100
  }
}
