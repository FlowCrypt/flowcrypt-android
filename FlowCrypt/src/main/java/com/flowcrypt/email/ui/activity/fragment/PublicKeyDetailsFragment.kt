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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.ContactEntity
import com.flowcrypt.email.jetpack.viewmodel.ContactsViewModel
import com.flowcrypt.email.jetpack.viewmodel.ParseKeysViewModel
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.EditContactActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.material.snackbar.Snackbar
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
class PublicKeyDetailsFragment : BaseFragment() {
  private val args by navArgs<PublicKeyDetailsFragmentArgs>()

  private val contactsViewModel: ContactsViewModel by viewModels()
  private val parseKeysViewModel: ParseKeysViewModel by viewModels()

  private var contactEntity: ContactEntity? = null
  private var details: PgpKeyDetails? = null
  private var progressBar: View? = null
  private var content: View? = null
  private var layoutUsers: ViewGroup? = null
  private var layoutFingerprints: ViewGroup? = null
  private var textViewAlgorithm: TextView? = null
  private var textViewCreated: TextView? = null

  override val contentResourceId: Int = R.layout.fragment_public_key_details

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
    contactEntity = args.contactEntity
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    supportActionBar?.setTitle(R.string.pub_key)
    initViews(view)

    setupContactsViewModel()
    setupParseKeysViewModel()
  }

  private fun setupParseKeysViewModel() {
    parseKeysViewModel.parseKeysLiveData.observe(viewLifecycleOwner, {
      when (it.status) {
        Result.Status.LOADING -> {
          UIUtil.exchangeViewVisibility(true, progressBar, content)
        }

        Result.Status.SUCCESS -> {
          val nodeKeyDetailsList = it.data
          if (nodeKeyDetailsList.isNullOrEmpty()) {
            Toast.makeText(context, R.string.error_no_keys, Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
          } else {
            details = nodeKeyDetailsList.first()
            updateViews()
            UIUtil.exchangeViewVisibility(false, progressBar, content)
          }
        }

        Result.Status.EXCEPTION -> {
          val msg = it.exception?.message ?: it.exception?.javaClass?.simpleName
          ?: getString(R.string.unknown_error)

          Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
      }
    })
  }

  private fun setupContactsViewModel() {
    contactEntity?.let {
      contactsViewModel.contactChangesLiveData(it).observe(viewLifecycleOwner, { contactEntity ->
        this.contactEntity = contactEntity
        parseKeysViewModel.fetchKeys(it.publicKey)
      })
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
        clipboard.setPrimaryClip(ClipData.newPlainText("pubKey", details?.publicKey))
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
          contactEntity?.let { roomDatabase.contactsDao().deleteSuspend(it) }
          parentFragmentManager.popBackStack()
        }
        return true
      }

      R.id.menuActionEdit -> {
        contactEntity?.let {
          startActivity(EditContactActivity.newIntent(requireContext(), account, it))
        }

        return true
      }

      else -> return super.onOptionsItemSelected(item)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      REQUEST_CODE_GET_URI_FOR_SAVING_KEY -> when (resultCode) {
        Activity.RESULT_OK -> saveKey(data)
      }
    }
  }

  private fun saveKey(data: Intent?) {
    try {
      val context = this.context ?: return
      val uri = data?.data ?: return
      val pubKey = details?.publicKey ?: return
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
              data?.data?.let { DocumentsContract.deleteDocument(contentResolver, it) }
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
    progressBar = view.findViewById(R.id.progressBar)
    content = view.findViewById(R.id.layoutContent)
    layoutUsers = view.findViewById(R.id.layoutUsers)
    layoutFingerprints = view.findViewById(R.id.layoutFingerprints)
    textViewAlgorithm = view.findViewById(R.id.textViewAlgorithm)
    textViewCreated = view.findViewById(R.id.textViewCreated)
  }

  private fun updateViews() {
    layoutUsers?.removeAllViews()
    details?.users?.forEachIndexed { index, s ->
      val textView = TextView(context)
      textView.text = getString(R.string.template_user, index + 1, s)
      layoutUsers?.addView(textView)
    }

    layoutFingerprints?.removeAllViews()
    details?.ids?.forEachIndexed { index, s ->
      val textViewFingerprint = TextView(context)
      textViewFingerprint.text =
        getString(R.string.template_fingerprint_2, index + 1, s.fingerprint)
      layoutFingerprints?.addView(textViewFingerprint)
    }

    textViewAlgorithm?.text = getString(R.string.template_algorithm, details?.algo?.algorithm)
    textViewCreated?.text = getString(
      R.string.template_created,
      DateFormat.getMediumDateFormat(context).format(Date(details?.created ?: 0))
    )
  }

  private fun chooseDest() {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = Constants.MIME_TYPE_PGP_KEY

    val sanitizedEmail = contactEntity?.email?.replace("[^a-z0-9]".toRegex(), "")
    val fileName = "0x" + details?.fingerprint + "-" + sanitizedEmail + "-publickey" + ".asc"

    intent.putExtra(Intent.EXTRA_TITLE, fileName)
    startActivityForResult(intent, REQUEST_CODE_GET_URI_FOR_SAVING_KEY)
  }

  companion object {
    private const val REQUEST_CODE_GET_URI_FOR_SAVING_KEY = 1
  }
}
