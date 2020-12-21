/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.BackupsViewModel
import com.flowcrypt.email.ui.activity.BackupKeysActivity
import com.flowcrypt.email.ui.activity.base.BaseSettingsBackStackSyncActivity
import com.flowcrypt.email.util.UIUtil

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
  private val backupsViewModel: BackupsViewModel by viewModels()

  private lateinit var progressBar: View
  override lateinit var rootView: View
  private lateinit var layoutSyncStatus: View
  private lateinit var layoutBackupFound: View
  private lateinit var layoutBackupNotFound: View
  private lateinit var textViewBackupFound: TextView

  private var privateKeys = mutableListOf<NodeKeyDetails>()

  override val contentViewResourceId: Int
    get() = R.layout.activity_backup_settings

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initViews()
    initBackupsViewModel()
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
    textViewBackupFound.text = getString(R.string.backups_found_message, privateKeys.size)
  }

  private fun initBackupsViewModel() {
    backupsViewModel.onlineBackupsLiveData.observe(this, {
      when (it.status) {
        Result.Status.LOADING -> {
          countingIdlingResource.incrementSafely()
          UIUtil.exchangeViewVisibility(true, progressBar, rootView)
        }

        Result.Status.SUCCESS -> {
          UIUtil.exchangeViewVisibility(false, progressBar, rootView)

          val keys = it.data

          privateKeys.clear()
          if (keys.isNullOrEmpty()) {
            showNoBackupFoundView()
          } else {
            privateKeys.addAll(keys)
            showBackupFoundView()
          }
          countingIdlingResource.decrementSafely()
        }

        Result.Status.EXCEPTION -> {
          toast(it.exception?.message
              ?: it.exception?.cause?.message
              ?: getString(R.string.unknown_error))
          countingIdlingResource.decrementSafely()
          finish()
        }

        else -> {
          countingIdlingResource.decrementSafely()
        }
      }
    })
  }

  companion object {
    private const val REQUEST_CODE_BACKUP_WITH_OPTION = 100
  }
}
