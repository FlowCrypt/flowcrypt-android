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
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.LoadingState
import com.flowcrypt.email.api.retrofit.Status
import com.flowcrypt.email.api.retrofit.node.NodeRepository
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper
import com.flowcrypt.email.api.retrofit.response.node.ParseKeysResult
import com.flowcrypt.email.jetpack.viewmodel.ParseKeysViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.common.util.CollectionUtils
import com.google.android.material.snackbar.Snackbar
import java.io.FileNotFoundException
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * This fragment shows the given public key details
 *
 * @author Denis Bondarenko
 *         Date: 9/20/19
 *         Time: 8:54 AM
 *         E-mail: DenBond7@gmail.com
 */
class PublicKeyDetailsFragment : BaseFragment(), Observer<NodeResponseWrapper<*>> {

  private var email: String? = null
  private var publicKey: String? = null
  private var details: NodeKeyDetails? = null
  private var progressBar: View? = null
  private var content: View? = null
  private var layoutUsers: ViewGroup? = null
  private var layoutLongIdsAndKeyWords: ViewGroup? = null
  private var textViewAlgorithm: TextView? = null
  private var textViewCreated: TextView? = null
  private var onContactDeletedListener: OnContactDeletedListener? = null

  override val contentResourceId: Int = R.layout.fragment_public_key_details

  override fun onAttach(context: Context) {
    super.onAttach(context)

    if (context is OnContactDeletedListener) {
      onContactDeletedListener = context
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)

    publicKey = arguments?.getString(KEY_PUBLIC_KEY)
    email = arguments?.getString(KEY_EMAIL)

    if (publicKey == null || email == null) {
      parentFragmentManager.popBackStack()
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    supportActionBar?.setTitle(R.string.pub_key)

    if (baseActivity.isNodeReady) {
      fetchKeyDetails()
    }
  }

  override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.fragment_pub_key_details, menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.menuActionCopy -> {
        val clipboard = context!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("pubKey", details?.publicKey))
        Toast.makeText(context, getString(R.string.public_key_copied_to_clipboard),
            Toast.LENGTH_SHORT).show()
        return true
      }

      R.id.menuActionSave -> {
        chooseDest()
        return true
      }

      R.id.menuActionDelete -> {
        parentFragmentManager.popBackStack()
        email?.let { onContactDeletedListener?.onContactDeleted(it) }
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

  override fun onChanged(nodeResponseWrapper: NodeResponseWrapper<*>) {
    when (nodeResponseWrapper.requestCode) {
      R.id.live_data_id_fetch_keys -> when (nodeResponseWrapper.status) {
        Status.LOADING -> {
          nodeResponseWrapper.loadingState?.let {
            if (LoadingState.PREPARE_REQUEST == it) {
              UIUtil.exchangeViewVisibility(true, progressBar, content)
            }
          }
        }

        Status.SUCCESS -> {
          val parseKeysResult = nodeResponseWrapper.result as ParseKeysResult?
          val nodeKeyDetailsList = parseKeysResult!!.nodeKeyDetails
          if (CollectionUtils.isEmpty(nodeKeyDetailsList)) {
            Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
          } else {
            details = nodeKeyDetailsList.first()
            updateViews()
            UIUtil.exchangeViewVisibility(false, progressBar, content)
          }
        }

        Status.ERROR -> Toast.makeText(context, nodeResponseWrapper.result?.apiError?.toString(),
            Toast.LENGTH_SHORT).show()

        Status.EXCEPTION -> Toast.makeText(context, nodeResponseWrapper.exception!!.message, Toast.LENGTH_SHORT).show()
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
      var error = if (TextUtils.isEmpty(e.message)) getString(R.string.unknown_error) else e.message

      if (e is IllegalStateException) {
        if (e.message != null && e.message!!.startsWith("Already exists")) {
          error = getString(R.string.not_saved_file_already_exists)
          showInfoSnackbar(view!!, error, Snackbar.LENGTH_LONG)

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

      showInfoSnackbar(view!!, error ?: "")
    }
  }

  private fun initViews(view: View) {
    progressBar = view.findViewById(R.id.progressBar)
    content = view.findViewById(R.id.layoutContent)
    layoutUsers = view.findViewById(R.id.layoutUsers)
    layoutLongIdsAndKeyWords = view.findViewById(R.id.layoutLongIdsAndKeyWords)
    textViewAlgorithm = view.findViewById(R.id.textViewAlgorithm)
    textViewCreated = view.findViewById(R.id.textViewCreated)
  }

  private fun updateViews() {
    details?.users?.forEachIndexed { index, s ->
      val textView = TextView(context)
      textView.text = getString(R.string.template_user, index + 1, s)
      layoutUsers?.addView(textView)
    }

    details?.ids?.forEachIndexed { index, s ->
      val textViewLongId = TextView(context)
      textViewLongId.text = getString(R.string.template_long_id, index + 1, s.longId)
      layoutLongIdsAndKeyWords?.addView(textViewLongId)

      val textViewKeywords = TextView(context)
      textViewKeywords.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
      textViewKeywords.text = s.keywords
      textViewKeywords.setTextColor(resources.getColor(R.color.gray, context?.theme))
      layoutLongIdsAndKeyWords?.addView(textViewKeywords)
    }

    textViewAlgorithm?.text = getString(R.string.template_algorithm, details?.algo?.algorithm)
    textViewCreated?.text = getString(R.string.template_created,
        DateFormat.getMediumDateFormat(context).format(
            Date(TimeUnit.MILLISECONDS.convert(details?.created ?: 0, TimeUnit.SECONDS))))
  }

  private fun fetchKeyDetails() {
    val viewModel = ViewModelProvider(this).get(ParseKeysViewModel::class.java)
    viewModel.init(NodeRepository())
    viewModel.responsesLiveData.observe(viewLifecycleOwner, this)
    viewModel.fetchKeys(publicKey)
  }

  private fun chooseDest() {
    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = Constants.MIME_TYPE_PGP_KEY

    val sanitizedEmail = email?.replace("[^a-z0-9]".toRegex(), "")
    val fileName = "0x" + details?.longId + "-" + sanitizedEmail + "-publickey" + ".asc"

    intent.putExtra(Intent.EXTRA_TITLE, fileName)
    startActivityForResult(intent, REQUEST_CODE_GET_URI_FOR_SAVING_KEY)
  }

  companion object {
    private val KEY_PUBLIC_KEY = GeneralUtil.generateUniqueExtraKey("KEY_PUBLIC_KEY",
        PublicKeyDetailsFragment::class.java)
    private val KEY_EMAIL = GeneralUtil.generateUniqueExtraKey("KEY_EMAIL",
        PublicKeyDetailsFragment::class.java)
    private const val REQUEST_CODE_GET_URI_FOR_SAVING_KEY = 1

    fun newInstance(email: String?, publicKey: String?): PublicKeyDetailsFragment {
      val fragment = PublicKeyDetailsFragment()
      val args = Bundle()
      args.putString(KEY_EMAIL, email)
      args.putString(KEY_PUBLIC_KEY, publicKey)
      fragment.arguments = args
      return fragment
    }
  }

  interface OnContactDeletedListener {
    fun onContactDeleted(email: String)
  }
}
