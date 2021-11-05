/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.attester.PubResponse
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.jetpack.viewmodel.RecipientsViewModel
import com.flowcrypt.email.model.KeyImportDetails
import com.flowcrypt.email.security.model.PgpKeyDetails
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import java.util.*

/**
 * This [Activity] retrieves a public keys string from the different sources and sends it to
 * [PreviewImportRecipientWithPubKeysActivity]
 *
 * @author Denis Bondarenko
 * Date: 04.05.2018
 * Time: 17:07
 * E-mail: DenBond7@gmail.com
 */
class ImportRecipientWithPubKeysActivity : BaseImportKeyActivity() {
  private val recipientsViewModel: RecipientsViewModel by viewModels()
  private var editTextEmailOrId: EditText? = null

  private var isSearchingActiveNow: Boolean = false
  private var fetchPubKeysRequestCode = 0L

  override val contentViewResourceId: Int
    get() = R.layout.activity_import_public_keys

  override val isPrivateKeyMode: Boolean
    get() = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setupContactsViewModel()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.activity_import_public_keys, menu)
    return true
  }

  override fun onPause() {
    super.onPause()
    isCheckingClipboardEnabled = false
  }

  override fun onBackPressed() {
    if (isSearchingActiveNow) {
      this.isSearchingActiveNow = false
      fetchPubKeysRequestCode = 0L
      UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
    } else {
      super.onBackPressed()
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_RUN_PREVIEW_ACTIVITY -> UIUtil.exchangeViewVisibility(
        false,
        layoutProgress, layoutContentView
      )

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menuActionHelp -> {
        FeedbackActivity.show(this)
        true
      }

      else -> super.onOptionsItemSelected(item)
    }
  }

  override fun onKeyFound(
    sourceType: KeyImportDetails.SourceType,
    keyDetailsList: List<PgpKeyDetails>
  ) {
    if (sourceType == KeyImportDetails.SourceType.CLIPBOARD) {
      if (keyDetailsList.isNotEmpty()) {
        UIUtil.exchangeViewVisibility(true, layoutProgress, layoutContentView)
        startActivityForResult(
          PreviewImportRecipientWithPubKeysActivity.newIntent(
            this, keyImportModel!!
              .keyString
          ), REQUEST_CODE_RUN_PREVIEW_ACTIVITY
        )
      } else {
        UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
        Toast.makeText(this, R.string.error_no_keys, Toast.LENGTH_SHORT).show()
      }
    }
  }

  override fun handleSelectedFile(uri: Uri) {
    UIUtil.exchangeViewVisibility(true, layoutProgress, layoutContentView)
    startActivityForResult(
      PreviewImportRecipientWithPubKeysActivity.newIntent(this, uri),
      REQUEST_CODE_RUN_PREVIEW_ACTIVITY
    )
  }

  override fun initViews() {
    super.initViews()
    this.editTextEmailOrId = findViewById(R.id.editTextKeyIdOrEmail)
    this.editTextEmailOrId?.setOnEditorActionListener { _, actionId, _ ->
      if (actionId == EditorInfo.IME_ACTION_SEARCH) {
        fetchPubKey()
      }
      return@setOnEditorActionListener true
    }

    findViewById<View>(R.id.iBSearchKey).setOnClickListener {
      editTextEmailOrId?.let { fetchPubKey() }
    }
  }

  private fun fetchPubKey() {
    val v = editTextEmailOrId ?: return
    UIUtil.hideSoftInput(this@ImportRecipientWithPubKeysActivity, v)

    if (v.text.isNullOrEmpty()) {
      Toast.makeText(this, R.string.please_type_key_id_or_email, Toast.LENGTH_SHORT).show()
      v.requestFocus()
      return
    }

    if (GeneralUtil.isConnected(this)) {
      editTextEmailOrId?.text?.toString()?.let {
        fetchPubKeysRequestCode = System.currentTimeMillis()
        recipientsViewModel.fetchPubKeys(it, fetchPubKeysRequestCode)
      }
    } else {
      showInfoSnackbar(rootView, getString(R.string.internet_connection_is_not_available))
    }
  }

  private fun setupContactsViewModel() {
    recipientsViewModel.pubKeysFromServerLiveData.observe(this, Observer {
      if (it.requestCode != fetchPubKeysRequestCode) return@Observer

      when (it.status) {
        Result.Status.LOADING -> {
          this.isSearchingActiveNow = true
          countingIdlingResource.incrementSafely()
          UIUtil.exchangeViewVisibility(true, layoutProgress, layoutContentView)
        }

        Result.Status.SUCCESS -> {
          this.isSearchingActiveNow = false
          it.data?.let { pubResponse -> handlePubResponse(pubResponse) }
          countingIdlingResource.decrementSafely()
        }

        Result.Status.EXCEPTION, Result.Status.ERROR -> {
          this.isSearchingActiveNow = false
          UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)

          val exception = it.exception ?: return@Observer
          Toast.makeText(
            this, if (exception.message.isNullOrEmpty()) {
              exception.javaClass.simpleName
            } else exception.message, Toast.LENGTH_SHORT
          ).show()

          countingIdlingResource.decrementSafely()
        }
      }
    })
  }

  private fun handlePubResponse(pubResponse: PubResponse) {
    if (pubResponse.apiError != null) {
      UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
      UIUtil.showInfoSnackbar(rootView, pubResponse.apiError.msg!!)
    } else {
      val pubkey = pubResponse.pubkey
      if (!pubkey.isNullOrEmpty()) {
        startActivityForResult(
          PreviewImportRecipientWithPubKeysActivity.newIntent(this, pubkey),
          REQUEST_CODE_RUN_PREVIEW_ACTIVITY
        )
      } else {
        UIUtil.exchangeViewVisibility(false, layoutProgress, layoutContentView)
        Toast.makeText(this, R.string.supported_public_key_not_found, Toast.LENGTH_SHORT).show()
      }
    }
  }

  companion object {
    private const val REQUEST_CODE_RUN_PREVIEW_ACTIVITY = 100

    fun newIntent(context: Context, accountEntity: AccountEntity?): Intent {
      return newIntent(
        context = context,
        accountEntity = accountEntity,
        title = context.getString(R.string.add_public_keys_of_your_contacts),
        throwErrorIfDuplicateFoundEnabled = false,
        cls = ImportRecipientWithPubKeysActivity::class.java
      )
    }
  }
}
