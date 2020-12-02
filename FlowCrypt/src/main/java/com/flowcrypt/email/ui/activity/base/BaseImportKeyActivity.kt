/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.base

import android.app.Activity
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.showInfoDialogFragment
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.model.KeyImportModel
import com.flowcrypt.email.service.CheckClipboardToFindKeyService
import com.flowcrypt.email.ui.activity.fragment.dialog.InfoDialogFragment
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.flowcrypt.email.util.exception.NodeException
import com.google.android.material.snackbar.Snackbar
import java.io.FileNotFoundException

/**
 * The base import key activity. This activity defines a logic of import a key (private or
 * public) via select a file or using clipboard.
 *
 * @author Denis Bondarenko
 * Date: 03.08.2017
 * Time: 12:35
 * E-mail: DenBond7@gmail.com
 */
abstract class BaseImportKeyActivity : BaseBackStackSyncActivity(), View.OnClickListener {
  protected lateinit var checkClipboardToFindKeyService: CheckClipboardToFindKeyService
  protected lateinit var layoutContentView: View
  protected lateinit var layoutProgress: View
  protected lateinit var textViewProgressText: TextView
  protected lateinit var textViewTitle: TextView
  protected lateinit var buttonLoadFromFile: View

  protected var keyImportModel: KeyImportModel? = null
  protected var isCheckingClipboardEnabled = true
  protected var isClipboardServiceBound: Boolean = false
  protected var tempAccount: AccountEntity? = null
  protected val privateKeysViewModel: PrivateKeysViewModel by viewModels()

  private var isCheckingPrivateKeyNow: Boolean = false
  private var throwErrorIfDuplicateFound: Boolean = false

  private lateinit var clipboardManager: ClipboardManager
  private var title: String? = null

  private val clipboardConn = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
      val binder = service as CheckClipboardToFindKeyService.LocalBinder
      checkClipboardToFindKeyService = binder.service
      checkClipboardToFindKeyService.isPrivateKeyMode = isPrivateKeyMode
      isClipboardServiceBound = true
    }

    override fun onServiceDisconnected(name: ComponentName) {
      isClipboardServiceBound = false
    }
  }

  abstract val isPrivateKeyMode: Boolean

  override val rootView: View
    get() = findViewById(R.id.layoutContent)

  abstract fun onKeyFound(type: KeyDetails.Type, keyDetailsList: ArrayList<NodeKeyDetails>)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    bindService(Intent(this, CheckClipboardToFindKeyService::class.java), clipboardConn, Context.BIND_AUTO_CREATE)

    this.tempAccount = intent?.getParcelableExtra(KEY_EXTRA_ACCOUNT)
    this.throwErrorIfDuplicateFound =
        intent?.getBooleanExtra(KEY_EXTRA_IS_THROW_ERROR_IF_DUPLICATE_FOUND, false) ?: false
    this.keyImportModel = intent?.getParcelableExtra(KEY_EXTRA_PRIVATE_KEY_IMPORT_MODEL_FROM_CLIPBOARD)
    this.title = intent?.getStringExtra(KEY_EXTRA_TITLE)

    clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    initViews()

    setupPrivateKeysViewModel()
    keyImportModel?.let { privateKeysViewModel.parseKeys(it, false) }
  }

  override fun onResume() {
    super.onResume()
    if (isClipboardServiceBound && !isCheckingPrivateKeyNow && isCheckingClipboardEnabled) {
      keyImportModel = checkClipboardToFindKeyService.keyImportModel
      keyImportModel?.let { privateKeysViewModel.parseKeys(it, false) }
    }
  }

  public override fun onPause() {
    super.onPause()
    isCheckingClipboardEnabled = true
  }

  override fun onDestroy() {
    super.onDestroy()
    if (isClipboardServiceBound) {
      unbindService(clipboardConn)
      isClipboardServiceBound = false
    }
  }

  public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_SELECT_KEYS_FROM_FILES_SYSTEM -> {
        isCheckingClipboardEnabled = false

        when (resultCode) {
          Activity.RESULT_OK -> if (data != null) {
            if (data.data != null) {
              handleSelectedFile(data.data!!)
            } else {
              showInfoSnackbar(rootView, getString(R.string.please_use_another_app_to_choose_file),
                  Snackbar.LENGTH_LONG)
            }
          }
        }
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }

  }

  override fun onBackPressed() {
    if (isCheckingPrivateKeyNow) {
      Toast.makeText(this, R.string.parsing_keys_please_wait, Toast.LENGTH_SHORT).show()
    } else {
      super.onBackPressed()
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonLoadFromFile -> {
        dismissSnackBar()
        selectFile()
      }

      R.id.buttonLoadFromClipboard -> {
        dismissSnackBar()

        if (clipboardManager.hasPrimaryClip()) {
          val clipData = clipboardManager.primaryClip
          if (clipData != null) {
            val item = clipData.getItemAt(0)
            val privateKeyFromClipboard = item.text
            if (!TextUtils.isEmpty(privateKeyFromClipboard)) {
              keyImportModel = KeyImportModel(null, privateKeyFromClipboard.toString(),
                  isPrivateKeyMode, KeyDetails.Type.CLIPBOARD)
              keyImportModel?.let { privateKeysViewModel.parseKeys(it, false) }
            } else {
              showClipboardIsEmptyInfoDialog()
            }
          }
        } else {
          showClipboardIsEmptyInfoDialog()
        }
      }
    }
  }

  /**
   * Handle a selected file.
   *
   * @param uri A [Uri] of the selected file.
   */
  protected open fun handleSelectedFile(uri: Uri) {
    keyImportModel = KeyImportModel(uri, null, isPrivateKeyMode, KeyDetails.Type.FILE)
    privateKeysViewModel.parseKeys(keyImportModel, true)
  }

  protected open fun initViews() {
    layoutContentView = findViewById(R.id.layoutContentView)
    layoutProgress = findViewById(R.id.layoutProgress)
    textViewProgressText = findViewById(R.id.textViewProgressText)

    textViewTitle = findViewById(R.id.textViewTitle)
    textViewTitle.text = title

    buttonLoadFromFile = findViewById(R.id.buttonLoadFromFile)
    buttonLoadFromFile.setOnClickListener(this)

    if (findViewById<View>(R.id.buttonLoadFromClipboard) != null) {
      findViewById<View>(R.id.buttonLoadFromClipboard).setOnClickListener(this)
    }
  }

  private fun setupPrivateKeysViewModel() {
    privateKeysViewModel.parseKeysLiveData.observe(this, Observer {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource.incrementSafely()
            isCheckingPrivateKeyNow = true
            textViewProgressText.setText(R.string.evaluating)
            UIUtil.exchangeViewVisibility(true, layoutProgress, layoutContentView)
          }

          Result.Status.SUCCESS -> {
            isCheckingPrivateKeyNow = false
            UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
            val keys = it.data?.nodeKeyDetails

            if (keys.isNullOrEmpty()) {
              val msg = when (keyImportModel?.type) {
                KeyDetails.Type.FILE ->
                  getString(R.string.file_has_wrong_pgp_structure,
                      if (isPrivateKeyMode) getString(R.string.private_) else getString(R.string.public_))

                KeyDetails.Type.CLIPBOARD ->
                  getString(R.string.clipboard_has_wrong_structure,
                      if (isPrivateKeyMode) getString(R.string.private_) else getString(R.string.public_))

                else ->
                  getString(R.string.source_has_wrong_pgp_structure,
                      if (isPrivateKeyMode) getString(R.string.private_) else getString(R.string.public_))
              }
              showInfoDialogFragment(dialogMsg = msg)
            } else {
              keyImportModel?.type?.let { type -> onKeyFound(type, ArrayList(keys)) }
            }

            countingIdlingResource.decrementSafely()
          }

          Result.Status.ERROR, Result.Status.EXCEPTION -> {
            isCheckingPrivateKeyNow = false
            UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
            var msg = when (it.status) {
              Result.Status.ERROR -> {
                val apiErrorMsg = it.data?.apiError?.msg
                if ("Error: This key is only partially encrypted." == apiErrorMsg) {
                  getString(R.string.partially_encrypted_private_key_error_msg)
                } else it.data?.apiError?.msg ?: getString(R.string.unknown_error)
              }

              else -> it.exception?.message ?: it.exception?.javaClass?.simpleName
              ?: getString(R.string.unknown_error)
            }

            if (it.exception is FileNotFoundException) {
              msg = getString(R.string.file_not_found)
            }

            if (it.exception is NodeException) {
              val nodeException = it.exception as NodeException?

              if (WRONG_STRUCTURE_ERROR == nodeException?.nodeError?.msg) {
                val mode = if (isPrivateKeyMode) getString(R.string.private_) else getString(R.string.public_)
                msg = when (keyImportModel?.type) {
                  KeyDetails.Type.FILE ->
                    getString(R.string.file_has_wrong_pgp_structure, mode)

                  else ->
                    getString(R.string.clipboard_has_wrong_structure, mode)
                }
              }
            }

            showInfoDialogFragment(dialogMsg = msg)

            countingIdlingResource.decrementSafely()
          }
        }
      }
    })
  }

  private fun selectFile() {
    val intent = Intent()
    intent.action = Intent.ACTION_OPEN_DOCUMENT
    intent.addCategory(Intent.CATEGORY_OPENABLE)
    intent.type = "*/*"
    startActivityForResult(Intent.createChooser(intent, getString(R.string.select_key_to_import)),
        REQUEST_CODE_SELECT_KEYS_FROM_FILES_SYSTEM)
  }

  private fun showClipboardIsEmptyInfoDialog() {
    val dialogMsg = getString(R.string.hint_clipboard_is_empty, if (isPrivateKeyMode)
      getString(R.string.private_)
    else
      getString(R.string.public_), getString(R.string.app_name))
    val infoDialogFragment = InfoDialogFragment.newInstance(getString(R.string.hint), dialogMsg)
    infoDialogFragment.show(supportFragmentManager, InfoDialogFragment::class.java.simpleName)
  }

  companion object {

    val KEY_EXTRA_ACCOUNT =
        GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_ACCOUNT", BaseImportKeyActivity::class.java)

    val KEY_EXTRA_IS_SYNC_ENABLE =
        GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_IS_SYNC_ENABLE", BaseImportKeyActivity::class.java)

    val KEY_EXTRA_IS_THROW_ERROR_IF_DUPLICATE_FOUND =
        GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_IS_THROW_ERROR_IF_DUPLICATE_FOUND",
            BaseImportKeyActivity::class.java)

    val KEY_EXTRA_PRIVATE_KEY_IMPORT_MODEL_FROM_CLIPBOARD =
        GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_PRIVATE_KEY_IMPORT_MODEL_FROM_CLIPBOARD",
            BaseImportKeyActivity::class.java)

    val KEY_EXTRA_TITLE =
        GeneralUtil.generateUniqueExtraKey("KEY_EXTRA_TITLE", BaseImportKeyActivity::class.java)
    private const val WRONG_STRUCTURE_ERROR = "Cannot parse key: could not determine pgpType"
    private const val REQUEST_CODE_SELECT_KEYS_FROM_FILES_SYSTEM = 10

    fun newIntent(context: Context?, accountEntity: AccountEntity?, isSyncEnabled: Boolean = false,
                  title: String, model: KeyImportModel? = null,
                  throwErrorIfDuplicateFoundEnabled: Boolean = false, cls: Class<*>): Intent {
      val intent = Intent(context, cls)
      intent.putExtra(KEY_EXTRA_ACCOUNT, accountEntity)
      intent.putExtra(KEY_EXTRA_IS_SYNC_ENABLE, isSyncEnabled)
      intent.putExtra(KEY_EXTRA_TITLE, title)
      intent.putExtra(KEY_EXTRA_PRIVATE_KEY_IMPORT_MODEL_FROM_CLIPBOARD, model)
      intent.putExtra(KEY_EXTRA_IS_THROW_ERROR_IF_DUPLICATE_FOUND, throwErrorIfDuplicateFoundEnabled)
      return intent
    }
  }
}
