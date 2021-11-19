/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.Constants
import com.flowcrypt.email.NavGraphDirections
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.databinding.FragmentPublicKeyDetailsBinding
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.jetpack.viewmodel.ParseKeysViewModel
import com.flowcrypt.email.ui.activity.EditContactActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.util.Date

/**
 * This fragment shows the given public key details
 *
 * @author Denis Bondarenko
 *         Date: 9/20/19
 *         Time: 8:54 AM
 *         E-mail: DenBond7@gmail.com
 */
class PublicKeyDetailsFragment : BaseFragment(), ProgressBehaviour {
  private val args by navArgs<PublicKeyDetailsFragmentArgs>()
  private var binding: FragmentPublicKeyDetailsBinding? = null
  private val parseKeysViewModel: ParseKeysViewModel by viewModels()

  private val savePubKeyActivityResultLauncher =
    registerForActivityResult(ExportPubKeyCreateDocument()) { uri: Uri? -> uri?.let { saveKey(it) } }

  override val contentResourceId: Int = R.layout.fragment_public_key_details

  override val progressView: View?
    get() = binding?.progress?.root
  override val contentView: View?
    get() = binding?.layoutContent
  override val statusView: View?
    get() = binding?.status?.root

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
    parseKeysViewModel.parseKeys(args.publicKeyEntity.publicKey)
    setupParseKeysViewModel()
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View? {
    binding = FragmentPublicKeyDetailsBinding.inflate(inflater, container, false)
    return binding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.setTitle(R.string.pub_key)
  }

  private fun setupParseKeysViewModel() {
    lifecycleScope.launchWhenStarted {
      parseKeysViewModel.pgpKeyDetailsListStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            showProgress()
          }

          Result.Status.SUCCESS -> {
            val pgpKeyDetailsList = it.data
            if (pgpKeyDetailsList.isNullOrEmpty()) {
              Toast.makeText(context, R.string.error_no_keys, Toast.LENGTH_SHORT).show()
              navController?.navigateUp()
            } else {
              updateViews()
              showContent()
            }
          }

          Result.Status.EXCEPTION -> {
            showStatus(getString(R.string.could_not_extract_key_details))

            var msg = it.exception?.message ?: it.exception?.javaClass?.simpleName
            ?: getString(R.string.unknown_error)

            if (it.exception is NoSuchElementException) {
              val matchingString = "No suitable signatures found on the key."
              if (matchingString.equals(other = it.exception.message, ignoreCase = true)) {
                msg = getString(R.string.key_sha1_warning_msg)
              }
            }

            navController?.navigate(
              NavGraphDirections.actionGlobalInfoDialogFragment(
                requestCode = 0,
                dialogTitle = "",
                dialogMsg = msg
              )
            )
          }
          else -> {}
        }
      }
    }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.fragment_pub_key_details, menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menuActionCopy -> {
        val clipboard =
          requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
          ClipData.newPlainText(
            "pubKey",
            args.publicKeyEntity.publicKey.toString()
          )
        )
        Toast.makeText(
          context, getString(R.string.public_key_copied_to_clipboard),
          Toast.LENGTH_SHORT
        ).show()
        return true
      }

      R.id.menuActionSave -> {
        chooseDest()
        return true
      }

      R.id.menuActionDelete -> {
        lifecycleScope.launch {
          val roomDatabase = FlowCryptRoomDatabase.getDatabase(requireContext())
          roomDatabase.pubKeyDao().deleteSuspend(args.publicKeyEntity)
          navController?.navigateUp()
        }
        return true
      }

      R.id.menuActionEdit -> {
        startActivity(
          EditContactActivity.newIntent(
            requireContext(),
            account,
            args.recipientEntity
          )
        )

        return true
      }

      else -> return super.onOptionsItemSelected(item)
    }
  }

  private fun saveKey(uri: Uri?) {
    uri ?: return
    try {
      val context = this.context ?: return
      val pubKey = String(args.publicKeyEntity.publicKey)
      GeneralUtil.writeFileFromStringToUri(context, uri, pubKey)
      Toast.makeText(context, getString(R.string.saved), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
      e.printStackTrace()
      var error = if (e.message.isNullOrEmpty()) e.javaClass.simpleName else e.message

      if (e is IllegalStateException) {
        if (e.message != null && e.message!!.startsWith("Already exists")) {
          error = getString(R.string.not_saved_file_already_exists)
          showInfoSnackbar(requireView(), error, Snackbar.LENGTH_LONG)

          try {
            context?.contentResolver?.let { contentResolver ->
              DocumentsContract.deleteDocument(contentResolver, uri)
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

  private fun updateViews() {
    binding?.layoutUsers?.removeAllViews()
    args.publicKeyEntity.pgpKeyDetails?.users?.forEachIndexed { index, s ->
      val textView = TextView(context)
      textView.text = getString(R.string.template_user, index + 1, s)
      binding?.layoutUsers?.addView(textView)
    }

    binding?.layoutFingerprints?.removeAllViews()
    args.publicKeyEntity.pgpKeyDetails?.ids?.forEachIndexed { index, s ->
      val textViewFingerprint = TextView(context)
      textViewFingerprint.text =
        getString(R.string.template_fingerprint_2, index + 1, s.fingerprint)
      binding?.layoutFingerprints?.addView(textViewFingerprint)
    }

    binding?.textViewAlgorithm?.text =
      getString(R.string.template_algorithm, args.publicKeyEntity.pgpKeyDetails?.algo?.algorithm)
    binding?.textViewCreated?.text = getString(
      R.string.template_created,
      DateFormat.getMediumDateFormat(context)
        .format(Date(args.publicKeyEntity.pgpKeyDetails?.created ?: 0))
    )
  }

  private fun chooseDest() {
    val sanitizedEmail = args.recipientEntity.email.replace("[^a-z0-9]".toRegex(), "")
    val fileName = "0x" + args.publicKeyEntity.pgpKeyDetails?.fingerprint + "-" +
        sanitizedEmail + "-publickey" + ".asc"
    savePubKeyActivityResultLauncher.launch(fileName)
  }

  inner class ExportPubKeyCreateDocument :
    ActivityResultContracts.CreateDocument() {
    override fun createIntent(context: Context, input: String): Intent {

      return super.createIntent(context, input)
        .addCategory(Intent.CATEGORY_OPENABLE)
        .setType(Constants.MIME_TYPE_PGP_KEY)
    }
  }
}
