/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors:
 *   DenBond7
 *   Ivan Pizhenko
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
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.Constants
import com.flowcrypt.email.R
import com.flowcrypt.email.database.FlowCryptRoomDatabase
import com.flowcrypt.email.database.entity.ContactEntity
import com.flowcrypt.email.jetpack.viewmodel.ContactsViewModel
import com.flowcrypt.email.model.PgpContact
import com.flowcrypt.email.model.PublicKeyInfo
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.security.pgp.PgpKey
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.adapter.ImportPgpContactsRecyclerViewAdapter
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.ExceptionUtil
import com.google.android.gms.common.util.CollectionUtils
import java.io.IOException
import java.io.InputStream
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
//todo-denbond7 Improve this class.
//Need to migrate to use LiveData and improve performance for the file parsing.
class PreviewImportPgpContactFragment : BaseFragment(), View.OnClickListener,
    ImportPgpContactsRecyclerViewAdapter.ContactActionsListener {

  private var recyclerView: RecyclerView? = null
  private var btnImportAll: TextView? = null
  private var textViewProgressTitle: TextView? = null
  private var progressBar: ProgressBar? = null
  private var layoutContentView: View? = null
  private var layoutProgress: View? = null
  private var emptyView: View? = null

  private var publicKeysString: String? = null
  private var publicKeysFileUri: Uri? = null

  private val contactsViewModel: ContactsViewModel by viewModels()
  private val adapter: ImportPgpContactsRecyclerViewAdapter = ImportPgpContactsRecyclerViewAdapter()
  private var isParsingStarted: Boolean = false

  override val contentResourceId: Int = R.layout.fragment_preview_import_pgp_contact

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    retainInstance = true

    val bundle = arguments

    if (bundle != null) {
      publicKeysString = bundle.getString(KEY_EXTRA_PUBLIC_KEY_STRING)
      publicKeysFileUri = bundle.getParcelable(KEY_EXTRA_PUBLIC_KEYS_FILE_URI)
    }

    adapter.contactActionsListener = this
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    if (TextUtils.isEmpty(publicKeysString) && publicKeysFileUri == null) {
      if (activity != null) {
        requireActivity().setResult(Activity.RESULT_CANCELED)
        requireActivity().finish()
      }
    } else if (!isParsingStarted) {
      PublicKeysParserAsyncTask(this, publicKeysString ?: "", publicKeysFileUri).execute()
      isParsingStarted = true
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonImportAll -> SaveAllContactsAsyncTask(this, adapter.publicKeys).execute()
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun onSuccess(loaderId: Int, result: Any?) {
    when (loaderId) {
      R.id.loader_id_parse_public_keys -> {
        val list = result as? List<PublicKeyInfo> ?: emptyList()
        if (list.isNotEmpty()) {
          UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
          adapter.swap(list)
          btnImportAll?.visibility = if (list.size > 1) View.VISIBLE else View.GONE
        } else {
          UIUtil.exchangeViewVisibility(false, layoutProgress, emptyView)
        }
      }

      else -> super.onSuccess(loaderId, result)
    }
  }

  override fun onError(loaderId: Int, e: Exception?) {
    when (loaderId) {
      R.id.loader_id_parse_public_keys -> if (activity != null) {
        requireActivity().setResult(Activity.RESULT_CANCELED)
        Toast.makeText(context, if (e?.message.isNullOrEmpty()) {
          e?.javaClass?.simpleName ?: getString(R.string.unknown_error)
        } else
          e?.message, Toast.LENGTH_SHORT).show()
        requireActivity().finish()
      }

      else -> super.onError(loaderId, e)
    }
  }

  override fun onSaveContactClick(publicKeyInfo: PublicKeyInfo) {
    contactsViewModel.addContact(publicKeyInfo.toPgpContact())
  }

  override fun onUpdateContactClick(publicKeyInfo: PublicKeyInfo) {
    contactsViewModel.updateContact(publicKeyInfo.toPgpContact())
  }

  private fun initViews(root: View) {
    layoutContentView = root.findViewById(R.id.layoutContentView)
    layoutProgress = root.findViewById(R.id.layoutProgress)
    btnImportAll = root.findViewById(R.id.buttonImportAll)
    textViewProgressTitle = root.findViewById(R.id.textViewProgressTitle)
    progressBar = root.findViewById(R.id.progressBar)
    btnImportAll?.setOnClickListener(this)
    recyclerView = root.findViewById(R.id.recyclerViewContacts)
    recyclerView?.setHasFixedSize(true)
    recyclerView?.layoutManager = LinearLayoutManager(context)
    recyclerView?.adapter = adapter
    emptyView = root.findViewById(R.id.emptyView)
  }

  private fun handleImportAllResult(result: Boolean?) {
    if (isAdded) {
      if (result == true) {
        Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show()
        if (activity != null) {
          requireActivity().setResult(Activity.RESULT_OK)
          requireActivity().finish()
        }
      } else {
        UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
        Toast.makeText(context, getString(R.string.could_not_import_data), Toast.LENGTH_SHORT).show()
      }
    }
  }

  private class PublicKeysParserAsyncTask(fragment: PreviewImportPgpContactFragment,
                                          private val publicKeysString: String,
                                          private val publicKeysFileUri: Uri?)
    : BaseAsyncTask<Void, Int, LoaderResult>(fragment) {
    override val progressTitleResourcesId: Int
      get() = R.string.parsing_public_keys

    override fun doInBackground(vararg uris: Void): LoaderResult {
      var keysInputStream: InputStream? = publicKeysString.toByteArray().inputStream()

      try {
        if (publicKeysFileUri != null && weakRef.get() != null) {
          weakRef.get()?.context?.let {
            keysInputStream = it.contentResolver.openInputStream(publicKeysFileUri)
          }
        }
      } catch (e: IOException) {
        e.printStackTrace()
        return LoaderResult(null, e)
      }

      return if (keysInputStream != null) {
        parseKeys(keysInputStream!!)
      } else {
        LoaderResult(null, IllegalStateException("Source is null"))
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

    private fun parseKeys(inputStream: InputStream): LoaderResult {
      try {
        val details = PgpKey.parseKeys(inputStream).toNodeKeyDetailsList()

        return if (!CollectionUtils.isEmpty(details)) {
          LoaderResult(parsePublicKeysInfo(details), null)
        } else {
          if (weakRef.get() != null) {
            LoaderResult(null, IllegalArgumentException(
                weakRef.get()?.requireContext()?.getString(R.string.clipboard_has_wrong_structure,
                    weakRef.get()?.requireContext()?.getString(R.string.public_))))
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

    private fun parsePublicKeysInfo(details: List<PgpKeyDetails>): List<PublicKeyInfo> {
      val publicKeyInfoList = ArrayList<PublicKeyInfo>()

      val emails = HashSet<String>()

      val blocksCount = details.size
      var progress: Float
      var lastProgress = 0f

      for (i in 0 until blocksCount) {
        val nodeKeyDetails = details[i]
        getPublicKeyInfo(nodeKeyDetails, emails)?.let {
          if (it.publicKey.length <= Constants.MAX_PUB_KEY_SIZE) {
            publicKeyInfoList.add(it)
          }
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

    private fun getPublicKeyInfo(pgpKeyDetails: PgpKeyDetails, emails: MutableSet<String>): PublicKeyInfo? {
      val fingerprint = pgpKeyDetails.fingerprint
      var keyOwner: String? = pgpKeyDetails.primaryPgpContact.email

      if (keyOwner != null) {
        keyOwner = keyOwner.toLowerCase(Locale.getDefault())

        if (emails.contains(keyOwner)) {
          return null
        }

        emails.add(keyOwner)

        if (weakRef.get() != null) {
          val contact = FlowCryptRoomDatabase.getDatabase(weakRef.get()?.requireContext()!!)
              .contactsDao().getContactByEmail(keyOwner)?.toPgpContact()
          return PublicKeyInfo(fingerprint!!, keyOwner, contact, pgpKeyDetails.publicKey!!)
        }
      }
      return null
    }
  }

  private class SaveAllContactsAsyncTask(
      fragment: PreviewImportPgpContactFragment,
      private val publicKeyInfoList: List<PublicKeyInfo>) : BaseAsyncTask<Void, Int, Boolean>(fragment) {

    override val progressTitleResourcesId: Int
      get() = R.string.importing_public_keys

    override fun doInBackground(vararg uris: Void): Boolean {
      val newCandidates = ArrayList<PgpContact>()
      val updateCandidates = ArrayList<PgpContact>()

      for (publicKeyInfo in publicKeyInfoList) {
        val pgpContact = PgpContact(publicKeyInfo.keyOwner, null, publicKeyInfo.publicKey,
            true, null, publicKeyInfo.fingerprint, 0)

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
              FlowCryptRoomDatabase.getDatabase(weakRef.get()?.requireContext()!!)
                  .contactsDao().insert(newCandidates.subList(start, end).map { it.toContactEntity() })
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
            val contacts = mutableListOf<ContactEntity>()
            val list = updateCandidates.subList(start, end + 1)

            list.forEach { pgpContact ->
              val foundContactEntity = FlowCryptRoomDatabase.getDatabase(weakRef.get()?.requireContext()!!)
                  .contactsDao().getContactByEmail(pgpContact.email)
              foundContactEntity?.let { entity -> contacts.add(pgpContact.toContactEntity().copy(id = entity.id)) }
            }

            FlowCryptRoomDatabase.getDatabase(weakRef.get()?.requireContext()!!).contactsDao().update(contacts)
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

  private abstract class BaseAsyncTask<Params, Progress, Result>(previewImportPgpContactFragment: PreviewImportPgpContactFragment)
    : AsyncTask<Params, Progress, Result>() {
    val weakRef: WeakReference<PreviewImportPgpContactFragment> =
        WeakReference(previewImportPgpContactFragment)

    abstract val progressTitleResourcesId: Int

    abstract fun updateProgress(progress: Progress)

    override fun onPreExecute() {
      super.onPreExecute()
      if (weakRef.get() != null) {
        weakRef.get()?.progressBar!!.isIndeterminate = true
        weakRef.get()?.textViewProgressTitle!!.setText(progressTitleResourcesId)
        UIUtil.exchangeViewVisibility(true,
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
