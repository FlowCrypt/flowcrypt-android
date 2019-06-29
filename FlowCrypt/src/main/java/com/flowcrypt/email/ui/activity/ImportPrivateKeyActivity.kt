/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.VisibleForTesting
import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.security.KeysStorageImpl
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.google.android.gms.common.util.CollectionUtils
import java.util.*

/**
 * This activity describes a logic of import private keys.
 *
 * @author Denis Bondarenko
 * Date: 20.07.2017
 * Time: 16:59
 * E-mail: DenBond7@gmail.com
 */

class ImportPrivateKeyActivity : BaseImportKeyActivity() {
  @get:VisibleForTesting
  var countingIdlingResource: CountingIdlingResource? = null
    private set
  private var privateKeys: ArrayList<NodeKeyDetails>? = null

  private var progressBar: View? = null
  private var layoutContent: View? = null
  private var layoutSyncStatus: View? = null
  private var buttonImportBackup: Button? = null

  private var isLoadPrivateKeysRequestSent: Boolean = false

  override val contentViewResourceId: Int
    get() = R.layout.activity_import_private_key

  override val isPrivateKeyMode: Boolean
    get() = true

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (isSyncEnabled && GeneralUtil.isConnected(this)) {
      UIUtil.exchangeViewVisibility(this, true, progressBar!!, layoutContent!!)
      countingIdlingResource = CountingIdlingResource(
          GeneralUtil.genIdlingResourcesName(ImportPrivateKeyActivity::class.java), GeneralUtil.isDebugBuild())
    } else {
      hideImportButton()
      UIUtil.exchangeViewVisibility(this, false, progressBar!!, layoutContent!!)
    }
  }

  override fun initViews() {
    super.initViews()

    this.progressBar = findViewById(R.id.progressBarLoadingBackups)
    this.layoutContent = findViewById(R.id.layoutContent)
    this.layoutSyncStatus = findViewById(R.id.layoutSyncStatus)
    this.buttonImportBackup = findViewById(R.id.buttonImportBackup)
    this.buttonImportBackup!!.setOnClickListener(this)
  }

  override fun onSyncServiceConnected() {
    if (!isLoadPrivateKeysRequestSent) {
      isLoadPrivateKeysRequestSent = true
      loadPrivateKeys(R.id.syns_load_private_keys)

      if (countingIdlingResource != null) {
        countingIdlingResource!!.increment()
      }
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun onReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {
    when (requestCode) {
      R.id.syns_load_private_keys -> {
        if (privateKeys == null) {
          val keys = obj as ArrayList<NodeKeyDetails>?
          if (keys != null) {
            if (keys.isNotEmpty()) {
              this.privateKeys = keys

              val uniqueKeysLongIds = filterKeys()

              if (this.privateKeys!!.isEmpty()) {
                hideImportButton()
              } else {
                buttonImportBackup!!.text = resources.getQuantityString(
                    R.plurals.import_keys, uniqueKeysLongIds.size)
                textViewTitle.text = resources.getQuantityString(
                    R.plurals.you_have_backups_that_was_not_imported, uniqueKeysLongIds.size)
              }
            } else {
              hideImportButton()
            }
          } else {
            hideImportButton()
          }
          UIUtil.exchangeViewVisibility(this, false, progressBar!!, layoutContent!!)
        }
        if (!countingIdlingResource!!.isIdleNow) {
          countingIdlingResource!!.decrement()
        }
      }
    }
  }

  override fun onErrorHappened(requestCode: Int, errorType: Int, e: Exception) {
    when (requestCode) {
      R.id.syns_load_private_keys -> {
        hideImportButton()
        UIUtil.exchangeViewVisibility(this, false, progressBar!!, layoutSyncStatus!!)
        UIUtil.showSnackbar(rootView, getString(R.string.error_occurred_while_receiving_private_keys),
            getString(android.R.string.ok), View.OnClickListener {
          layoutSyncStatus!!.visibility = View.GONE
          UIUtil.exchangeViewVisibility(this@ImportPrivateKeyActivity,
              false, progressBar!!, layoutContent!!)
        })
        if (!countingIdlingResource!!.isIdleNow) {
          countingIdlingResource!!.decrement()
        }
      }
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonImportBackup -> if (!CollectionUtils.isEmpty(privateKeys)) {
        startActivityForResult(CheckKeysActivity.newIntent(this, privateKeys!!, KeyDetails.Type.EMAIL, null,
            getString(R.string.continue_), getString(R.string.choose_another_key)), REQUEST_CODE_CHECK_PRIVATE_KEYS)
      }

      else -> super.onClick(v)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_CHECK_PRIVATE_KEYS -> {
        isCheckingClipboardEnabled = false

        when (resultCode) {
          Activity.RESULT_OK -> {
            setResult(Activity.RESULT_OK)
            finish()
          }
        }
      }
      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onKeyFound(type: KeyDetails.Type, keyDetailsList: ArrayList<NodeKeyDetails>) {
    when (type) {
      KeyDetails.Type.FILE -> {
        val fileName = GeneralUtil.getFileNameFromUri(this, keyImportModel!!.fileUri)
        val bottomTitle = resources.getQuantityString(R.plurals.file_contains_some_amount_of_keys,
            keyDetailsList.size, fileName, keyDetailsList.size)
        val posBtnTitle = getString(R.string.continue_)
        val intent = CheckKeysActivity.newIntent(this, keyDetailsList, KeyDetails.Type.FILE,
            bottomTitle, posBtnTitle, null, getString(R.string.choose_another_key), true)
        startActivityForResult(intent, REQUEST_CODE_CHECK_PRIVATE_KEYS)
      }

      KeyDetails.Type.CLIPBOARD -> {
        val title = resources.getQuantityString(R.plurals.loaded_private_keys_from_clipboard,
            keyDetailsList.size, keyDetailsList.size)
        val clipboardIntent = CheckKeysActivity.newIntent(this, keyDetailsList, KeyDetails.Type.CLIPBOARD, title,
            getString(R.string.continue_), null, getString(R.string.choose_another_key), true)
        startActivityForResult(clipboardIntent,
            REQUEST_CODE_CHECK_PRIVATE_KEYS)
      }

      else -> {
      }
    }
  }

  private fun hideImportButton() {
    buttonImportBackup!!.visibility = View.GONE
    val marginLayoutParams = buttonLoadFromFile
        .layoutParams as ViewGroup.MarginLayoutParams
    marginLayoutParams.topMargin = resources.getDimensionPixelSize(R.dimen
        .margin_top_first_button)
    buttonLoadFromFile.requestLayout()
  }

  private fun filterKeys(): Set<String> {
    val connector = KeysStorageImpl.getInstance(this)

    val iterator = privateKeys!!.iterator()
    val uniqueKeysLongIds = HashSet<String>()

    while (iterator.hasNext()) {
      val privateKey = iterator.next()
      uniqueKeysLongIds.add(privateKey.longId!!)
      if (connector.getPgpPrivateKey(privateKey.longId!!) != null) {
        iterator.remove()
        uniqueKeysLongIds.remove(privateKey.longId!!)
      }
    }
    return uniqueKeysLongIds
  }

  companion object {
    private const val REQUEST_CODE_CHECK_PRIVATE_KEYS = 100
  }
}
