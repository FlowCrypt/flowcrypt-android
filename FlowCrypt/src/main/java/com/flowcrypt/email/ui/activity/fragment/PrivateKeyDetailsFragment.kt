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
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.showInfoDialog
import com.flowcrypt.email.extensions.showTwoWayDialog
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.CheckPrivateKeysViewModel
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.material.snackbar.Snackbar
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
class PrivateKeyDetailsFragment : BaseFragment() {
  private var tVPassPhraseVerification: TextView? = null
  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()
  private val checkPrivateKeysViewModel: CheckPrivateKeysViewModel by viewModels()
  private var pgpKeyDetails: PgpKeyDetails? = null

  override val contentResourceId: Int = R.layout.fragment_private_key_details

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)

    val args = arguments
    if (args != null) {
      pgpKeyDetails = args.getParcelable(KEY_NODE_KEY_DETAILS)
    }

    if (pgpKeyDetails == null) {
      parentFragmentManager.popBackStack()
    } else {
      pgpKeyDetails?.let {
        val context = context ?: return@let
        val passPhrase = KeysStorageImpl.getInstance(context)
            .getPassphraseByFingerprint(it.fingerprint)
            ?: return@let
        val passPhraseType = KeysStorageImpl.getInstance(context)
            .getPassphraseTypeByFingerprint(it.fingerprint)
            ?: return@let
        checkPrivateKeysViewModel.checkKeys(listOf(it), passPhrase, passPhraseType)
      }
    }
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
            dialogMsg = requireContext().resources.getQuantityString(R.plurals.delete_key_question, 1, 1),
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
            pgpKeyDetails?.let {
              account?.let { accountEntity -> privateKeysViewModel.deleteKeys(accountEntity, listOf(it)) }
            }
          }
        }
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  private fun saveKey(data: Intent) {
    try {
      GeneralUtil.writeFileFromStringToUri(requireContext(), data.data!!, pgpKeyDetails!!.publicKey)
      Toast.makeText(context, getString(R.string.saved), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
      e.printStackTrace()
      var error = if (e.message.isNullOrEmpty()) e.javaClass.simpleName else e.message

      if (e is IllegalStateException) {
        if (e.message != null && e.message!!.startsWith("Already exists")) {
          error = getString(R.string.not_saved_file_already_exists)
          showInfoSnackbar(requireView(), error, Snackbar.LENGTH_LONG)

          try {
            data.data?.let { DocumentsContract.deleteDocument(requireContext().contentResolver, it) }
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
    val pgpContacts = pgpKeyDetails?.pgpContacts ?: emptyList()
    val emails = ArrayList<String>()

    for ((email) in pgpContacts) {
      emails.add(email)
    }

    val textViewFingerprint = view.findViewById<TextView>(R.id.textViewFingerprint)
    UIUtil.setHtmlTextToTextView(getString(R.string.template_fingerprint,
        GeneralUtil.doSectionsInText(" ", pgpKeyDetails?.fingerprint, 4)), textViewFingerprint)

    val textViewDate = view.findViewById<TextView>(R.id.textViewDate)
    textViewDate?.text = getString(R.string.template_date,
        DateFormat.getMediumDateFormat(context).format(Date(pgpKeyDetails?.created ?: 0)))

    val textViewUsers = view.findViewById<TextView>(R.id.textViewUsers)
    textViewUsers.text = getString(R.string.template_users, TextUtils.join(", ", emails))

    tVPassPhraseVerification = view.findViewById(R.id.tVPassPhraseVerification)

    initButtons(view)
  }

  private fun initButtons(view: View) {
    view.findViewById<View>(R.id.btnShowPubKey)?.setOnClickListener {
      val dialogFragment = InfoDialogFragment.newInstance("", pgpKeyDetails!!.publicKey)
      dialogFragment.show(parentFragmentManager, InfoDialogFragment::class.java.simpleName)
    }

    view.findViewById<View>(R.id.btnCopyToClipboard)?.setOnClickListener {
      val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      clipboard.setPrimaryClip(ClipData.newPlainText("pubKey", pgpKeyDetails?.publicKey))
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
    intent.putExtra(Intent.EXTRA_TITLE, "0x" + pgpKeyDetails!!.fingerprint + ".asc")
    startActivityForResult(intent, REQUEST_CODE_GET_URI_FOR_SAVING_KEY)
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
              ?: "Couldn't delete a key with fingerprint = {${pgpKeyDetails?.fingerprint ?: ""}}")
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
          val verificationMsg: String?
          if (checkResult != null) {
            if (checkResult.pgpKeyDetails.isPrivate) {
              if (checkResult.e == null) {
                verificationMsg = getString(R.string.stored_pass_phrase_matched)
              } else {
                verificationMsg = getString(R.string.stored_pass_phrase_mismatch)
                context?.let {
                  tVPassPhraseVerification?.setTextColor(UIUtil.getColor(it, R.color.red))
                }
              }
            } else verificationMsg = getString(R.string.not_private_key)
          } else {
            verificationMsg = getString(R.string.could_not_check_pass_phrase)
            context?.let {
              tVPassPhraseVerification?.setTextColor(UIUtil.getColor(it, R.color.red))
            }
          }

          tVPassPhraseVerification?.text = verificationMsg
          baseActivity.countingIdlingResource.decrementSafely()
        }

        Result.Status.ERROR, Result.Status.EXCEPTION -> {
          showInfoDialog(dialogMsg = it.exception?.message
              ?: it.exception?.javaClass?.simpleName
              ?: getString(R.string.could_not_check_pass_phrase))
          baseActivity.countingIdlingResource.decrementSafely()
        }
      }
    })
  }

  companion object {
    private val KEY_NODE_KEY_DETAILS =
        GeneralUtil.generateUniqueExtraKey("KEY_NODE_KEY_DETAILS",
            PrivateKeyDetailsFragment::class.java)
    private const val REQUEST_CODE_GET_URI_FOR_SAVING_KEY = 1
    private const val REQUEST_CODE_DELETE_KEY_DIALOG = 100

    fun newInstance(details: PgpKeyDetails): PrivateKeyDetailsFragment {
      val keyDetailsFragment = PrivateKeyDetailsFragment()
      val args = Bundle()
      args.putParcelable(KEY_NODE_KEY_DETAILS, details)
      keyDetailsFragment.arguments = args
      return keyDetailsFragment
    }
  }
}
