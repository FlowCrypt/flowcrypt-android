/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.ApiName
import com.flowcrypt.email.api.retrofit.BaseResponse
import com.flowcrypt.email.api.retrofit.request.attester.PubRequest
import com.flowcrypt.email.api.retrofit.response.attester.PubResponse
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.model.KeyDetails
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.ui.activity.base.BaseImportKeyActivity
import com.flowcrypt.email.ui.activity.settings.FeedbackActivity
import com.flowcrypt.email.ui.loader.ApiServiceAsyncTaskLoader
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import java.util.*

/**
 * This [Activity] retrieves a public keys string from the different sources and sends it to
 * [PreviewImportPgpContactActivity]
 *
 * @author Denis Bondarenko
 * Date: 04.05.2018
 * Time: 17:07
 * E-mail: DenBond7@gmail.com
 */
class ImportPgpContactActivity : BaseImportKeyActivity(), TextView.OnEditorActionListener {
  private var editTextEmailOrId: EditText? = null

  private var isSearchingActiveNow: Boolean = false

  override val contentViewResourceId: Int
    get() = R.layout.activity_import_public_keys

  override val isPrivateKeyMode: Boolean
    get() = false

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
      LoaderManager.getInstance(this).destroyLoader(R.id.loader_id_search_public_key)
      UIUtil.exchangeViewVisibility(applicationContext, false, layoutProgress, layoutContentView)
    } else {
      super.onBackPressed()
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_RUN_PREVIEW_ACTIVITY -> UIUtil.exchangeViewVisibility(applicationContext, false,
          layoutProgress, layoutContentView)

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

  override fun onKeyFound(type: KeyDetails.Type, keyDetailsList: ArrayList<NodeKeyDetails>) {
    if (type == KeyDetails.Type.CLIPBOARD) {
      if (keyDetailsList.isNotEmpty()) {
        UIUtil.exchangeViewVisibility(applicationContext, true, layoutProgress, layoutContentView)
        startActivityForResult(PreviewImportPgpContactActivity.newIntent(this, keyImportModel!!
            .keyString), REQUEST_CODE_RUN_PREVIEW_ACTIVITY)
      } else {
        UIUtil.exchangeViewVisibility(applicationContext, false, layoutProgress, layoutContentView)
        Toast.makeText(this, R.string.unknown_error, Toast.LENGTH_SHORT).show()
      }
    }
  }

  override fun handleSelectedFile(uri: Uri) {
    UIUtil.exchangeViewVisibility(applicationContext, true, layoutProgress, layoutContentView)
    startActivityForResult(PreviewImportPgpContactActivity.newIntent(this, uri), REQUEST_CODE_RUN_PREVIEW_ACTIVITY)
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<LoaderResult> {
    return when (id) {
      R.id.loader_id_search_public_key -> {
        this.isSearchingActiveNow = true
        UIUtil.exchangeViewVisibility(applicationContext, true, layoutProgress, layoutContentView)
        val lookUpRequest = PubRequest(ApiName.GET_PUB, editTextEmailOrId!!.text.toString())
        ApiServiceAsyncTaskLoader(applicationContext, lookUpRequest)
      }
      else -> super.onCreateLoader(id, args)
    }
  }

  override fun onSuccess(loaderId: Int, result: Any?) {
    when (loaderId) {
      R.id.loader_id_search_public_key -> {
        this.isSearchingActiveNow = false
        val baseResponse = result as BaseResponse<*>?
        if (baseResponse != null) {
          if (baseResponse.responseModel != null) {
            val pubResponse = baseResponse.responseModel as PubResponse?
            handlePubResponse(pubResponse!!)
          } else {
            UIUtil.exchangeViewVisibility(applicationContext, false, layoutProgress, layoutContentView)
            UIUtil.showInfoSnackbar(rootView, getString(R.string.api_error))
          }
        } else {
          UIUtil.exchangeViewVisibility(applicationContext, false, layoutProgress, layoutContentView)
          UIUtil.showInfoSnackbar(rootView, getString(R.string.internal_error))
        }
      }

      else -> super.onSuccess(loaderId, result)
    }
  }

  override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {
    when (actionId) {
      EditorInfo.IME_ACTION_SEARCH -> {
        UIUtil.hideSoftInput(this@ImportPgpContactActivity, v)

        if (v?.text.isNullOrEmpty()) {
          Toast.makeText(this, R.string.please_type_key_id_or_email, Toast.LENGTH_SHORT).show()
          return true
        }

        if (GeneralUtil.isConnected(this)) {
          LoaderManager.getInstance(this).restartLoader(R.id.loader_id_search_public_key, null,
              this@ImportPgpContactActivity)
        } else {
          showInfoSnackbar(rootView, getString(R.string.internet_connection_is_not_available))
        }
      }
    }

    return true
  }

  override fun onError(loaderId: Int, e: Exception?) {
    when (loaderId) {
      R.id.loader_id_search_public_key -> {
        UIUtil.exchangeViewVisibility(applicationContext, false, layoutProgress, layoutContentView)
        Toast.makeText(this, if (TextUtils.isEmpty(e!!.message)) getString(R.string.unknown_error) else e.message,
            Toast.LENGTH_SHORT).show()
      }

      else -> super.onError(loaderId, e)
    }
  }

  override fun initViews() {
    super.initViews()
    this.editTextEmailOrId = findViewById(R.id.editTextKeyIdOrEmail)
    this.editTextEmailOrId!!.setOnEditorActionListener(this)
  }

  private fun handlePubResponse(pubResponse: PubResponse) {
    if (pubResponse.apiError != null) {
      UIUtil.exchangeViewVisibility(applicationContext, false, layoutProgress, layoutContentView)
      UIUtil.showInfoSnackbar(rootView, pubResponse.apiError.msg!!)
    } else {
      val pubkey = pubResponse.pubkey
      if (!pubkey.isNullOrEmpty()) {
        startActivityForResult(PreviewImportPgpContactActivity.newIntent(this, pubkey),
            REQUEST_CODE_RUN_PREVIEW_ACTIVITY)
      } else {
        UIUtil.exchangeViewVisibility(applicationContext, false, layoutProgress, layoutContentView)
        Toast.makeText(this, R.string.no_public_key_found, Toast.LENGTH_SHORT).show()
      }
    }
  }

  companion object {
    private const val REQUEST_CODE_RUN_PREVIEW_ACTIVITY = 100

    fun newIntent(context: Context): Intent {
      return newIntent(context, context.getString(R.string.add_public_keys_of_your_contacts),
          false, ImportPgpContactActivity::class.java)
    }
  }
}
