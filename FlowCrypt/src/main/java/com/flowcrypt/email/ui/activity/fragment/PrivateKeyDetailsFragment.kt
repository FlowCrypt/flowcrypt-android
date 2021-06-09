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
import android.text.TextUtils
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.KeyEntity
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
import java.util.*

/**
 * This [Fragment] helps to show details about the given key.
 *
 * @author Denis Bondarenko
 * Date: 20.11.18
 * Time: 12:43
 * E-mail: DenBond7@gmail.com
 */
class PrivateKeyDetailsFragment : BaseFragment(), ProgressBehaviour {
  override val progressView: View?
    get() = view?.findViewById(R.id.progress)
  override val contentView: View?
    get() = view?.findViewById(R.id.content)
  override val statusView: View?
    get() = view?.findViewById(R.id.status)

  private var tVFingerprint: TextView? = null
  private var tVDate: TextView? = null
  private var tVUsers: TextView? = null
  private var tVPassPhraseVerification: TextView? = null
  private var eTKeyPassword: EditText? = null
  private var btnForgetPassphrase: Button? = null
  private var gCheckPassphrase: View? = null
  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()
  private val checkPrivateKeysViewModel: CheckPrivateKeysViewModel by viewModels()
  private val pgpKeyDetailsViewModel: PgpKeyDetailsViewModel by viewModels {
    PgpKeyDetailsViewModelFactory(
      arguments?.getString(KEY_PGP_KEY_FINGERPRINT),
      requireActivity().application
    )
  }

  override val contentResourceId: Int = R.layout.fragment_private_key_details

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.fragment_key_details, menu)
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

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.setTitle(R.string.key_details)
    initViews(view)
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

  private fun saveKey(data: Intent) {
    try {
      GeneralUtil.writeFileFromStringToUri(
        context = requireContext(),
        uri = data.data!!,
        data = pgpKeyDetailsViewModel.getPgpKeyDetails()!!.publicKey
      )
      Toast.makeText(context, getString(R.string.saved), Toast.LENGTH_SHORT).show()
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

  private fun initViews(view: View) {
    tVFingerprint = view.findViewById(R.id.textViewFingerprint)
    tVDate = view.findViewById(R.id.textViewDate)
    tVUsers = view.findViewById(R.id.textViewUsers)
    tVPassPhraseVerification = view.findViewById(R.id.tVPassPhraseVerification)
    eTKeyPassword = view.findViewById(R.id.eTKeyPassword)
    gCheckPassphrase = view.findViewById(R.id.gCheckPassphrase)

    initButtons(view)
  }

  private fun updateViews() {
    pgpKeyDetailsViewModel.getPgpKeyDetails()?.let { value ->
      UIUtil.setHtmlTextToTextView(
        getString(
          R.string.template_fingerprint,
          GeneralUtil.doSectionsInText(" ", value.fingerprint, 4)
        ), tVFingerprint
      )

      tVDate?.text = getString(
        R.string.template_date,
        DateFormat.getMediumDateFormat(context).format(Date(value.created))
      )

      tVUsers?.text = getString(
        R.string.template_users,
        TextUtils.join(", ", value.pgpContacts.map { it.email })
      )

      val passPhrase = pgpKeyDetailsViewModel.getPassphrase()
      val passPhraseType = pgpKeyDetailsViewModel.getPassphraseType()
      if (passPhrase == null || passPhrase.isEmpty) {
        handlePassphraseNotProvided()
        return
      }

      if (passPhraseType == KeyEntity.PassphraseType.RAM) {
        btnForgetPassphrase?.visible()
      }
    }
  }

  private fun handlePassphraseNotProvided() {
    tVPassPhraseVerification?.setTextColor(UIUtil.getColor(requireContext(), R.color.red))
    tVPassPhraseVerification?.text = getString(R.string.pass_phrase_not_provided)
    btnForgetPassphrase?.gone()
    gCheckPassphrase?.visible()
  }

  private fun initButtons(view: View) {
    btnForgetPassphrase = view.findViewById(R.id.btnForgetPassphrase)
    btnForgetPassphrase?.setOnClickListener {
      pgpKeyDetailsViewModel.forgetPassphrase()
      toast(getString(R.string.passphrase_purged_from_memory))
      eTKeyPassword?.text = null
    }

    view.findViewById<View>(R.id.btnUpdatePassphrase)?.setOnClickListener {
      UIUtil.hideSoftInput(requireContext(), eTKeyPassword)
      val typedText = eTKeyPassword?.text?.toString()
      if (typedText.isNullOrEmpty()) {
        showInfoSnackbar(eTKeyPassword, getString(R.string.passphrase_must_be_non_empty))
      } else {
        snackBar?.dismiss()
        eTKeyPassword?.let {
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

    view.findViewById<View>(R.id.btnShowPubKey)?.setOnClickListener {
      val dialogFragment = InfoDialogFragment.newInstance(
        dialogTitle = "",
        dialogMsg = pgpKeyDetailsViewModel.getPgpKeyDetails()!!.publicKey
      )
      dialogFragment.show(parentFragmentManager, InfoDialogFragment::class.java.simpleName)
    }

    view.findViewById<View>(R.id.btnCopyToClipboard)?.setOnClickListener {
      val clipboard =
        requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      clipboard.setPrimaryClip(
        ClipData.newPlainText(
          "pubKey",
          pgpKeyDetailsViewModel.getPgpKeyDetails()?.publicKey
        )
      )
      Toast.makeText(context, getString(R.string.copied), Toast.LENGTH_SHORT).show()
    }
    view.findViewById<View>(R.id.btnSaveToFile)?.setOnClickListener {
      chooseDest()
    }
    view.findViewById<View>(R.id.btnShowPrKey)?.setOnClickListener {
      toast(getString(R.string.see_backups_to_save_your_private_keys), Toast.LENGTH_SHORT)
    }
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
          baseActivity.countingIdlingResource.incrementSafely()
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
          baseActivity.countingIdlingResource.decrementSafely()
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showContent()
          baseActivity.countingIdlingResource.decrementSafely()
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
          baseActivity.countingIdlingResource.incrementSafely()
        }

        Result.Status.SUCCESS -> {
          privateKeysViewModel.deleteKeysLiveData.value = Result.none()
          parentFragmentManager.popBackStack()
          baseActivity.countingIdlingResource.decrementSafely()
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showInfoDialog(
            dialogMsg = it.exception?.message ?: it.exception?.javaClass?.simpleName
            ?: "Couldn't delete a key with fingerprint =" +
            " {${pgpKeyDetailsViewModel.getPgpKeyDetails()?.fingerprint ?: ""}}"
          )
          baseActivity.countingIdlingResource.decrementSafely()
          privateKeysViewModel.deleteKeysLiveData.value = Result.none()
        }
      }
    })
  }

  private fun setupCheckPrivateKeysViewModel() {
    checkPrivateKeysViewModel.checkPrvKeysLiveData.observe(viewLifecycleOwner, { it ->
      when (it.status) {
        Result.Status.LOADING -> {
          baseActivity.countingIdlingResource.incrementSafely()
        }

        Result.Status.SUCCESS -> {
          val checkResult = it.data?.firstOrNull()
          var verificationMsg: String?
          if (checkResult != null) {
            if (checkResult.pgpKeyDetails.isPrivate) {
              if (checkResult.e == null) {
                tVPassPhraseVerification?.setTextColor(
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
                  btnForgetPassphrase?.visible()
                  gCheckPassphrase?.gone()
                }
              } else {
                if (pgpKeyDetailsViewModel.getPassphraseType() == KeyEntity.PassphraseType.RAM) {
                  eTKeyPassword?.requestFocus()
                  toast(R.string.password_is_incorrect)
                  verificationMsg = getString(R.string.pass_phrase_not_provided)
                } else {
                  verificationMsg = getString(R.string.stored_pass_phrase_mismatch)
                  tVPassPhraseVerification?.setTextColor(
                    UIUtil.getColor(requireContext(), R.color.red)
                  )
                }
              }
            } else verificationMsg = getString(R.string.not_private_key)
          } else {
            verificationMsg = getString(R.string.could_not_check_pass_phrase)
            tVPassPhraseVerification?.setTextColor(UIUtil.getColor(requireContext(), R.color.red))
          }

          tVPassPhraseVerification?.text = verificationMsg
          baseActivity.countingIdlingResource.decrementSafely()
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showInfoDialog(
            dialogMsg = it.exception?.message
              ?: it.exception?.javaClass?.simpleName
              ?: getString(R.string.could_not_check_pass_phrase)
          )
          baseActivity.countingIdlingResource.decrementSafely()
        }
      }
    })
  }

  companion object {
    private val KEY_PGP_KEY_FINGERPRINT =
      GeneralUtil.generateUniqueExtraKey(
        "KEY_PGP_KEY_FINGERPRINT",
        PrivateKeyDetailsFragment::class.java
      )
    private const val REQUEST_CODE_GET_URI_FOR_SAVING_KEY = 1
    private const val REQUEST_CODE_DELETE_KEY_DIALOG = 100

    fun newInstance(fingerprint: String): PrivateKeyDetailsFragment {
      val keyDetailsFragment = PrivateKeyDetailsFragment()
      val args = Bundle()
      args.putString(KEY_PGP_KEY_FINGERPRINT, fingerprint)
      keyDetailsFragment.arguments = args
      return keyDetailsFragment
    }
  }
}
