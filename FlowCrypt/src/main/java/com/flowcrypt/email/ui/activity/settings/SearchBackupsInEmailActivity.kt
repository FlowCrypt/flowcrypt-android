/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.test.espresso.idling.CountingIdlingResource
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.ui.activity.BackupKeysActivity
import com.flowcrypt.email.ui.activity.base.BaseSettingsBackStackSyncActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import com.google.android.gms.common.util.CollectionUtils
import java.util.*

/**
 * This activity helps a user to backup his private keys via next methods:
 *
 *  * BACKUP AS EMAIL
 *  * BACKUP AS A FILE
 *
 *
 * @author DenBond7
 * Date: 30.05.2017
 * Time: 15:27
 * E-mail: DenBond7@gmail.com
 */

class SearchBackupsInEmailActivity : BaseSettingsBackStackSyncActivity(), View.OnClickListener {

  @get:VisibleForTesting
  lateinit var countingIdlingResource: CountingIdlingResource
  private lateinit var progressBar: View
  override lateinit var rootView: View
  private lateinit var layoutSyncStatus: View
  private lateinit var layoutBackupFound: View
  private lateinit var layoutBackupNotFound: View
  private lateinit var textViewBackupFound: TextView

  private var privateKeys: ArrayList<NodeKeyDetails>? = null

  private var isRequestSent: Boolean = false

  override val contentViewResourceId: Int
    get() = R.layout.activity_backup_settings

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initViews()

    if (GeneralUtil.isConnected(this)) {
      UIUtil.exchangeViewVisibility(true, progressBar, rootView)
    } else {
      Toast.makeText(this, R.string.internet_connection_is_not_available, Toast.LENGTH_SHORT).show()
      finish()
    }
    countingIdlingResource = CountingIdlingResource(
        GeneralUtil.genIdlingResourcesName(SearchBackupsInEmailActivity::class.java), GeneralUtil.isDebugBuild())
    countingIdlingResource.increment()
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_BACKUP_WITH_OPTION -> when (resultCode) {
        Activity.RESULT_OK -> {
          Toast.makeText(this, R.string.backed_up_successfully, Toast.LENGTH_SHORT).show()
          finish()
        }
      }
      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun onReplyReceived(requestCode: Int, resultCode: Int, obj: Any?) {
    when (requestCode) {
      R.id.syns_load_private_keys -> {
        if (privateKeys == null) {
          UIUtil.exchangeViewVisibility(false, progressBar, rootView)
          val keys = obj as ArrayList<NodeKeyDetails>?
          if (CollectionUtils.isEmpty(keys)) {
            showNoBackupFoundView()
          } else {
            this.privateKeys = keys
            showBackupFoundView()
          }
        }
        if (!countingIdlingResource.isIdleNow) {
          countingIdlingResource.decrement()
        }
      }
    }
  }

  override fun onErrorHappened(requestCode: Int, errorType: Int, e: Exception) {
    when (requestCode) {
      R.id.syns_load_private_keys -> {
        UIUtil.exchangeViewVisibility(false, progressBar, layoutSyncStatus)
        if (!countingIdlingResource.isIdleNow) {
          countingIdlingResource.decrement()
        }
        UIUtil.showSnackbar(rootView, getString(R.string.error_occurred_while_receiving_private_keys),
            getString(R.string.retry), View.OnClickListener {
          layoutSyncStatus.visibility = View.GONE
          UIUtil.exchangeViewVisibility(true, progressBar, rootView)
          loadPrivateKeys(R.id.syns_load_private_keys)
        })
      }
    }
  }

  override fun onSyncServiceConnected() {
    super.onSyncServiceConnected()
    if (!isRequestSent) {
      isRequestSent = true
      loadPrivateKeys(R.id.syns_load_private_keys)
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.buttonSeeMoreBackupOptions,
      R.id.buttonBackupMyKey ->
        startActivityForResult(Intent(this, BackupKeysActivity::class.java), REQUEST_CODE_BACKUP_WITH_OPTION)
    }
  }

  private fun initViews() {
    this.progressBar = findViewById(R.id.progressBar)
    this.rootView = findViewById(R.id.layoutContent)
    this.layoutSyncStatus = findViewById(R.id.layoutSyncStatus)
    this.layoutBackupFound = findViewById(R.id.layoutBackupFound)
    this.layoutBackupNotFound = findViewById(R.id.layoutBackupNotFound)
    this.textViewBackupFound = findViewById(R.id.textViewBackupFound)

    if (findViewById<View>(R.id.buttonSeeMoreBackupOptions) != null) {
      findViewById<View>(R.id.buttonSeeMoreBackupOptions).setOnClickListener(this)
    }

    if (findViewById<View>(R.id.buttonBackupMyKey) != null) {
      findViewById<View>(R.id.buttonBackupMyKey).setOnClickListener(this)
    }
  }

  private fun showNoBackupFoundView() {
    layoutBackupNotFound.visibility = View.VISIBLE
  }

  private fun showBackupFoundView() {
    layoutBackupFound.visibility = View.VISIBLE
    if (privateKeys != null) {
      textViewBackupFound.text = getString(R.string.backups_found_message, privateKeys!!.size)
    }
  }

  companion object {
    private const val REQUEST_CODE_BACKUP_WITH_OPTION = 100
  }
}
