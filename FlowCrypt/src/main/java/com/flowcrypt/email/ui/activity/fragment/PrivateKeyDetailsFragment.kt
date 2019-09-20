/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment
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
class PrivateKeyDetailsFragment : BaseFragment(), View.OnClickListener {

  private var details: NodeKeyDetails? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val args = arguments
    if (args != null) {
      details = args.getParcelable(KEY_NODE_KEY_DETAILS)
    }

    if (details == null) {
      fragmentManager!!.popBackStack()
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, bundle: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_private_key_details, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    if (supportActionBar != null) {
      supportActionBar!!.setTitle(R.string.my_public_key)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      REQUEST_CODE_GET_URI_FOR_SAVING_KEY -> when (resultCode) {
        Activity.RESULT_OK -> if (data != null && data.data != null) {
          saveKey(data)
        }
      }
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.btnShowPubKey -> {
        val dialogFragment = InfoDialogFragment.newInstance("", details!!.publicKey!!)
        dialogFragment.show(fragmentManager!!, InfoDialogFragment::class.java.simpleName)
      }

      R.id.btnCopyToClipboard -> {
        val clipboard = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.primaryClip = ClipData.newPlainText("pubKey", details!!.publicKey)
        Toast.makeText(context, getString(R.string.copied), Toast.LENGTH_SHORT).show()
      }

      R.id.btnSaveToFile -> chooseDest()

      R.id.btnShowPrKey -> Toast.makeText(context, getString(R.string.see_backups_to_save_your_private_keys),
          Toast.LENGTH_SHORT).show()
    }
  }

  private fun saveKey(data: Intent) {
    try {
      GeneralUtil.writeFileFromStringToUri(context!!, data.data!!, details!!.publicKey!!)
      Toast.makeText(context, getString(R.string.saved), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
      e.printStackTrace()
      var error = if (TextUtils.isEmpty(e.message)) getString(R.string.unknown_error) else e.message

      if (e is IllegalStateException) {
        if (e.message != null && e.message!!.startsWith("Already exists")) {
          error = getString(R.string.not_saved_file_already_exists)
          showInfoSnackbar(view!!, error, Snackbar.LENGTH_LONG)

          try {
            DocumentsContract.deleteDocument(context!!.contentResolver, data.data)
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

      showInfoSnackbar(view!!, error ?: "")
    }

  }

  private fun initViews(view: View) {
    val pgpContacts = details!!.pgpContacts
    val emails = ArrayList<String>()

    for ((email) in pgpContacts) {
      emails.add(email)
    }

    val textViewKeyWords = view.findViewById<TextView>(R.id.textViewKeyWords)
    UIUtil.setHtmlTextToTextView(getString(R.string.template_key_words, details!!.keywords), textViewKeyWords)

    val textViewFingerprint = view.findViewById<TextView>(R.id.textViewFingerprint)
    UIUtil.setHtmlTextToTextView(getString(R.string.template_fingerprint,
        GeneralUtil.doSectionsInText(" ", details!!.fingerprint, 4)), textViewFingerprint)

    val textViewLongId = view.findViewById<TextView>(R.id.textViewLongId)
    textViewLongId.text = getString(R.string.template_longid, details!!.longId)

    val textViewDate = view.findViewById<TextView>(R.id.textViewDate)
    textViewDate.text = getString(R.string.template_date,
        DateFormat.getMediumDateFormat(context).format(Date(details!!.created)))

    val textViewUsers = view.findViewById<TextView>(R.id.textViewUsers)
    textViewUsers.text = getString(R.string.template_users, TextUtils.join(", ", emails))

    initButtons(view)
  }

  private fun initButtons(view: View) {
    if (view.findViewById<View>(R.id.btnShowPubKey) != null) {
      view.findViewById<View>(R.id.btnShowPubKey).setOnClickListener(this)
    }

    if (view.findViewById<View>(R.id.btnCopyToClipboard) != null) {
      view.findViewById<View>(R.id.btnCopyToClipboard).setOnClickListener(this)
    }

    if (view.findViewById<View>(R.id.btnSaveToFile) != null) {
      view.findViewById<View>(R.id.btnSaveToFile).setOnClickListener(this)
    }

    if (view.findViewById<View>(R.id.btnShowPrKey) != null) {
      view.findViewById<View>(R.id.btnShowPrKey).setOnClickListener(this)
    }
  }

  private fun chooseDest() {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = Constants.MIME_TYPE_PGP_KEY
    intent.putExtra(Intent.EXTRA_TITLE, "0x" + details!!.longId + ".asc")
    startActivityForResult(intent, REQUEST_CODE_GET_URI_FOR_SAVING_KEY)
  }

  companion object {
    private val KEY_NODE_KEY_DETAILS = GeneralUtil.generateUniqueExtraKey("KEY_NODE_KEY_DETAILS",
        PrivateKeyDetailsFragment::class.java)
    private const val REQUEST_CODE_GET_URI_FOR_SAVING_KEY = 1

    @JvmStatic
    fun newInstance(details: NodeKeyDetails): PrivateKeyDetailsFragment {
      val keyDetailsFragment = PrivateKeyDetailsFragment()
      val args = Bundle()
      args.putParcelable(KEY_NODE_KEY_DETAILS, details)
      keyDetailsFragment.arguments = args
      return keyDetailsFragment
    }
  }
}
