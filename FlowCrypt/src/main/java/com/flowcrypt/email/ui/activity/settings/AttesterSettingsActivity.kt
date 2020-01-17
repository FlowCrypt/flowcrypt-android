/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings

import android.os.Bundle
import android.view.View
import android.widget.ListView
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.attester.LookUpEmailResponse
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity
import com.flowcrypt.email.ui.adapter.AttesterKeyAdapter
import com.flowcrypt.email.ui.loader.LoadAccountKeysInfo
import com.flowcrypt.email.util.UIUtil

/**
 * Basically, this Activity gets all known addresses of the user, and then submits one call with all addresses to
 * /lookup/email/ Attester API, then compares the results.
 *
 * @author DenBond7
 * Date: 13.11.2017
 * Time: 15:07
 * E-mail: DenBond7@gmail.com
 */

class AttesterSettingsActivity : BaseBackStackActivity(), LoaderManager.LoaderCallbacks<LoaderResult> {
  private var progressBar: View? = null
  private var emptyView: View? = null
  private var layoutContent: View? = null
  private var listViewKeys: ListView? = null

  override val contentViewResourceId: Int
    get() = R.layout.activity_attester_settings

  override val rootView: View
    get() = findViewById(R.id.screenContent)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    initViews()
  }

  override fun onCreateLoader(id: Int, args: Bundle?): Loader<LoaderResult> {
    return when (id) {
      R.id.loader_id_load_keys_info_from_attester -> {
        UIUtil.exchangeViewVisibility(true, progressBar!!, layoutContent!!)
        LoadAccountKeysInfo(this,
            AccountDaoSource().getActiveAccountInformation(this))
      }
      else -> Loader(this)
    }
  }

  override fun onLoadFinished(loader: Loader<LoaderResult>, loaderResult: LoaderResult) {
    handleLoaderResult(loader, loaderResult)
  }

  override fun onLoaderReset(loader: Loader<LoaderResult>) {
    UIUtil.exchangeViewVisibility(false, progressBar!!, layoutContent!!)
  }

  @Suppress("UNCHECKED_CAST")
  override fun onSuccess(loaderId: Int, result: Any?) {
    when (loaderId) {
      R.id.loader_id_load_keys_info_from_attester -> {
        UIUtil.exchangeViewVisibility(false, progressBar!!, layoutContent!!)
        val responses = result as List<LookUpEmailResponse>?
        if (responses != null && responses.isNotEmpty()) {
          listViewKeys!!.adapter = AttesterKeyAdapter(this, responses)
        } else {
          UIUtil.exchangeViewVisibility(true, emptyView!!, layoutContent!!)
        }
      }

      else -> super.onSuccess(loaderId, result)
    }
  }

  override fun onError(loaderId: Int, e: Exception?) {
    when (loaderId) {
      R.id.loader_id_load_keys_info_from_attester -> {
        UIUtil.exchangeViewVisibility(false, progressBar!!, layoutContent!!)
        showInfoSnackbar(rootView, e!!.message)
      }

      else -> super.onError(loaderId, e)
    }
  }

  private fun initViews() {
    this.progressBar = findViewById(R.id.progressBar)
    this.layoutContent = findViewById(R.id.layoutContent)
    this.emptyView = findViewById(R.id.emptyView)
    listViewKeys = findViewById(R.id.listViewKeys)

    LoaderManager.getInstance(this).initLoader(R.id.loader_id_load_keys_info_from_attester, null, this)
  }
}
