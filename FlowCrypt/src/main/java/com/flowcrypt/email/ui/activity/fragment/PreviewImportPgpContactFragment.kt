/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.app.Activity
import android.content.OperationApplicationException
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Parcelable
import android.os.RemoteException
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.node.NodeCallsExecutor
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.dao.source.ContactsDaoSource
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.model.PublicKeyInfo
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.adapter.ImportPgpContactsRecyclerViewAdapter
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.common.util.CollectionUtils
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*

/**
 * This fragment displays information about public keys owners and information about keys.
 *
 * @author Denis Bondarenko
 * Date: 05.06.2018
 * Time: 14:15
 * E-mail: DenBond7@gmail.com
 */
//todo-DenBond7 it would be great to improve this fragment
class PreviewImportPgpContactFragment : BaseFragment(), View.OnClickListener {

  private var publicKeyInfoList: List<PublicKeyInfo>? = null
  private var recyclerView: RecyclerView? = null
  private var btnImportAll: TextView? = null
  private var textViewProgressTitle: TextView? = null
  private var progressBar: ProgressBar? = null
  private var layoutContentView: View? = null
  private var layoutProgress: View? = null
  private var emptyView: View? = null

  private var publicKeysString: String? = null
  private var publicKeysFileUri: Uri? = null

  private var isParsingStarted: Boolean = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    retainInstance = true

    val bundle = arguments

    if (bundle != null) {
      publicKeysString = bundle.getString(KEY_EXTRA_PUBLIC_KEY_STRING)
      publicKeysFileUri = bundle.getParcelable(KEY_EXTRA_PUBLIC_KEYS_FILE_URI)
    }
  }

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                            savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_preview_import_pgp_contact, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    if (TextUtils.isEmpty(publicKeysString) && publicKeysFileUri == null) {
      if (activity != null) {
        activity!!.setResult(Activity.RESULT_CANCELED)
        activity!!.finish()
      }
    } else if (!isParsingStarted) {
      PublicKeysParserAsyncTask(this, publicKeysString ?: "", publicKeysFileUri).execute()
      isParsingStarted = true
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonImportAll -> publicKeyInfoList?.let { SaveAllContactsAsyncTask(this, it).execute() }
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun onSuccess(loaderId: Int, result: Any?) {
    when (loaderId) {
      R.id.loader_id_parse_public_keys -> {
        publicKeyInfoList = result as List<PublicKeyInfo>?
        if (publicKeyInfoList!!.isNotEmpty()) {
          UIUtil.exchangeViewVisibility(context, false, layoutProgress!!, layoutContentView!!)
          recyclerView!!.adapter = ImportPgpContactsRecyclerViewAdapter(publicKeyInfoList!!)
          btnImportAll!!.visibility = if (publicKeyInfoList!!.size > 1) View.VISIBLE else View.GONE
        } else {
          UIUtil.exchangeViewVisibility(context, false, layoutProgress!!, emptyView!!)
        }
      }

      else -> super.onSuccess(loaderId, result)
    }
  }

  override fun onError(loaderId: Int, e: Exception?) {
    when (loaderId) {
      R.id.loader_id_parse_public_keys -> if (activity != null) {
        activity!!.setResult(Activity.RESULT_CANCELED)
        Toast.makeText(context, if (TextUtils.isEmpty(e!!.message))
          getString(R.string.unknown_error)
        else
          e.message, Toast.LENGTH_SHORT).show()
        activity!!.finish()
      }

      else -> super.onError(loaderId, e)
    }
  }

  private fun initViews(root: View) {
    layoutContentView = root.findViewById(R.id.layoutContentView)
    layoutProgress = root.findViewById(R.id.layoutProgress)
    btnImportAll = root.findViewById(R.id.buttonImportAll)
    textViewProgressTitle = root.findViewById(R.id.textViewProgressTitle)
    progressBar = root.findViewById(R.id.progressBar)
    btnImportAll!!.setOnClickListener(this)
    recyclerView = root.findViewById(R.id.recyclerViewContacts)
    recyclerView!!.setHasFixedSize(true)
    recyclerView!!.layoutManager = LinearLayoutManager(context)
    emptyView = root.findViewById(R.id.emptyView)
  }

  private fun handleImportAllResult(result: Boolean?) {
    if (isAdded) {
      if (result!!) {
        Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show()
        if (activity != null) {
          activity!!.setResult(Activity.RESULT_OK)
          activity!!.finish()
        }
      } else {
        UIUtil.exchangeViewVisibility(context, false, layoutProgress!!, layoutContentView!!)
        Toast.makeText(context, R.string.unknown_error, Toast.LENGTH_SHORT).show()
      }
    }
  }

  private class PublicKeysParserAsyncTask internal constructor(fragment: PreviewImportPgpContactFragment,
                                                               private val publicKeysString: String,
                                                               private val publicKeysFileUri: Uri?)
    : BaseAsyncTask<Void, Int, LoaderResult>(fragment) {
    override val progressTitleResourcesId: Int
      get() = R.string.parsing_public_keys

    override fun doInBackground(vararg uris: Void): LoaderResult {
      var armoredKeys: String? = publicKeysString

      try {
        if (publicKeysFileUri != null && weakRef.get() != null) {
          armoredKeys = GeneralUtil.readFileFromUriToString(weakRef.get()?.context!!, publicKeysFileUri)
        }
      } catch (e: IOException) {
        e.printStackTrace()
        return LoaderResult(null, e)
      }

      return if (!TextUtils.isEmpty(armoredKeys) && weakRef.get() != null) {
        parseKeys(armoredKeys)
      } else {
        LoaderResult(null, NullPointerException("An input string is null!"))
      }
    }

    override fun onPostExecute(loaderResult: LoaderResult) {
      super.onPostExecute(loaderResult)
      if (weakRef.get() != null) {
        weakRef.get()?.handleLoaderResult(R.id.loader_id_parse_public_keys, loaderResult)
      }
    }

    override fun updateProgress(progress: Int) {
      if (weakRef.get() != null) {
        weakRef.get()?.progressBar!!.progress = progress
      }
    }

    private fun parseKeys(armoredKeys: String?): LoaderResult {
      try {
        val details = NodeCallsExecutor.parseKeys(armoredKeys!!)

        return if (!CollectionUtils.isEmpty(details)) {
          LoaderResult(parsePublicKeysInfo(details), null)
        } else {
          if (weakRef.get() != null) {
            LoaderResult(null, IllegalArgumentException(
                weakRef.get()?.context!!.getString(R.string.clipboard_has_wrong_structure,
                    weakRef.get()?.context!!.getString(R.string.public_))))
          } else {
            LoaderResult(null,
                IllegalArgumentException("The content of your clipboard doesn't look like a valid PGP pubkey."))
          }
        }

      } catch (e: Exception) {
        e.printStackTrace()
        ExceptionUtil.handleError(e)
        return LoaderResult(null, e)
      }
    }

    private fun parsePublicKeysInfo(details: List<NodeKeyDetails>): List<PublicKeyInfo> {
      val publicKeyInfoList = ArrayList<PublicKeyInfo>()

      val emails = HashSet<String>()

      val blocksCount = details.size
      var progress: Float
      var lastProgress = 0f

      for (i in 0 until blocksCount) {
        val nodeKeyDetails = details[i]
        val publicKeyInfo = getPublicKeyInfo(nodeKeyDetails, emails)

        if (publicKeyInfo != null) {
          publicKeyInfoList.add(publicKeyInfo)
        }

        progress = i * 100f / blocksCount
        if (progress - lastProgress >= 1) {
          publishProgress(progress.toInt())
          lastProgress = progress
        }
      }

      publishProgress(100)

      return publicKeyInfoList
    }

    private fun getPublicKeyInfo(nodeKeyDetails: NodeKeyDetails, emails: MutableSet<String>): PublicKeyInfo? {
      val fingerprint = nodeKeyDetails.fingerprint
      val longId = nodeKeyDetails.longId
      val keyWords = nodeKeyDetails.keywords
      var keyOwner: String? = nodeKeyDetails.primaryPgpContact.email

      if (keyOwner != null) {
        keyOwner = keyOwner.toLowerCase(Locale.getDefault())

        if (emails.contains(keyOwner)) {
          return null
        }

        emails.add(keyOwner)

        if (weakRef.get() != null) {
          val contact = ContactsDaoSource().getPgpContact(weakRef.get()?.context!!, keyOwner)
          return PublicKeyInfo(keyWords!!, fingerprint!!, keyOwner, longId!!, contact, nodeKeyDetails.publicKey!!)
        }
      }
      return null
    }
  }

  private class SaveAllContactsAsyncTask internal constructor(
      fragment: PreviewImportPgpContactFragment,
      private val publicKeyInfoList: List<PublicKeyInfo>) : BaseAsyncTask<Void, Int, Boolean>(fragment) {

    override val progressTitleResourcesId: Int
      get() = R.string.importing_public_keys

    override fun doInBackground(vararg uris: Void): Boolean? {
      val source = ContactsDaoSource()
      val newCandidates = ArrayList<PgpContact>()
      val updateCandidates = ArrayList<PgpContact>()

      for (publicKeyInfo in publicKeyInfoList) {
        val pgpContact = PgpContact(publicKeyInfo.keyOwner, null, publicKeyInfo.publicKey,
            true, null, publicKeyInfo.fingerprint, publicKeyInfo.longId, publicKeyInfo.keyWords, 0)

        if (publicKeyInfo.hasPgpContact()) {
          if (publicKeyInfo.isUpdateEnabled) {
            updateCandidates.add(pgpContact)
          }
        } else {
          newCandidates.add(pgpContact)
        }
      }

      try {
        var progress: Float
        var lastProgress = 0f
        val totalOperationsCount = newCandidates.size + updateCandidates.size

        run {
          var i = 0
          while (i < newCandidates.size) {
            val start = i
            val end = if (newCandidates.size - i > STEP_AMOUNT) i + STEP_AMOUNT else newCandidates.size

            if (weakRef.get() != null) {
              source.addRowsUsingApplyBatch(weakRef.get()?.context, newCandidates.subList(start, end))
            }
            i = end

            progress = i * 100f / totalOperationsCount
            if (progress - lastProgress >= 1) {
              publishProgress(progress.toInt())
              lastProgress = progress
            }

            i--
            i++
          }
        }

        var i = 0
        while (i < updateCandidates.size) {
          val start = i
          val end = if (updateCandidates.size - i > STEP_AMOUNT) i + STEP_AMOUNT else updateCandidates.size - 1

          if (weakRef.get() != null) {
            source.updatePgpContacts(weakRef.get()?.context!!, updateCandidates.subList(start, end + 1))
          }
          i = end + 1

          progress = i * 100f / totalOperationsCount
          if (progress - lastProgress >= 1) {
            publishProgress(progress.toInt())
            lastProgress = progress
          }
          i++
        }

      } catch (e: RemoteException) {
        e.printStackTrace()
        return false
      } catch (e: OperationApplicationException) {
        e.printStackTrace()
        return false
      }

      publishProgress(100)
      return true
    }


    override fun onPostExecute(b: Boolean?) {
      super.onPostExecute(b)
      if (b != null && weakRef.get() != null) {
        weakRef.get()?.handleImportAllResult(b)
      }
    }

    override fun updateProgress(progress: Int) {
      if (weakRef.get() != null) {
        weakRef.get()?.progressBar!!.progress = progress
      }
    }

    companion object {
      private const val STEP_AMOUNT = 50
    }
  }

  private abstract class BaseAsyncTask<Params, Progress, Result>
  internal constructor(previewImportPgpContactFragment: PreviewImportPgpContactFragment)
    : AsyncTask<Params, Progress, Result>() {
    internal val weakRef: WeakReference<PreviewImportPgpContactFragment> =
        WeakReference(previewImportPgpContactFragment)

    abstract val progressTitleResourcesId: Int

    abstract fun updateProgress(progress: Progress)

    override fun onPreExecute() {
      super.onPreExecute()
      if (weakRef.get() != null) {
        weakRef.get()?.progressBar!!.isIndeterminate = true
        weakRef.get()?.textViewProgressTitle!!.setText(progressTitleResourcesId)
        UIUtil.exchangeViewVisibility(weakRef.get()?.context, true,
            weakRef.get()?.layoutProgress!!, weakRef.get()?.layoutContentView!!)
      }
    }

    @SafeVarargs
    override fun onProgressUpdate(vararg values: Progress) {
      super.onProgressUpdate(*values)
      if (weakRef.get() != null) {
        if (weakRef.get()?.progressBar!!.isIndeterminate) {
          weakRef.get()?.progressBar!!.isIndeterminate = false
        }
        updateProgress(values[0])
      }
    }
  }

  companion object {
    private val KEY_EXTRA_PUBLIC_KEY_STRING =
        GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_PUBLIC_KEY_STRING",
            PreviewImportPgpContactFragment::class.java)

    private val KEY_EXTRA_PUBLIC_KEYS_FILE_URI =
        GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_PUBLIC_KEYS_FILE_URI",
            PreviewImportPgpContactFragment::class.java)

    @JvmStatic
    fun newInstance(stringExtra: String?, fileUri: Parcelable?): PreviewImportPgpContactFragment {
      val args = Bundle()
      args.putString(KEY_EXTRA_PUBLIC_KEY_STRING, stringExtra)
      args.putParcelable(KEY_EXTRA_PUBLIC_KEYS_FILE_URI, fileUri)

      val previewImportPgpContactFragment = PreviewImportPgpContactFragment()
      previewImportPgpContactFragment.arguments = args
      return previewImportPgpContactFragment
    }
  }
}
