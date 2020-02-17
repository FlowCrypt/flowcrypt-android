/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.settings

import android.os.Bundle
import android.view.View
import android.widget.ListView
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.jetpack.viewmodel.AccountKeysInfoViewModel
import com.flowcrypt.email.ui.activity.base.BaseBackStackActivity
import com.flowcrypt.email.ui.adapter.AttesterKeyAdapter
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

class AttesterSettingsActivity : BaseBackStackActivity() {
  private val accountKeysInfoViewModel: AccountKeysInfoViewModel by viewModels()
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
    setupAccountKeysInfoViewModel()
  }

  private fun initViews() {
    this.progressBar = findViewById(R.id.progressBar)
    this.layoutContent = findViewById(R.id.layoutContent)
    this.emptyView = findViewById(R.id.emptyView)
    listViewKeys = findViewById(R.id.listViewKeys)
  }

  private fun setupAccountKeysInfoViewModel() {
    accountKeysInfoViewModel.accountKeysInfoLiveData.observe(this, Observer {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            UIUtil.exchangeViewVisibility(true, progressBar, layoutContent)
          }

          Result.Status.SUCCESS -> {


            UIUtil.exchangeViewVisibility(false, progressBar, layoutContent)
            val result = it.data
            result?.results?.let { responses ->
              if (responses.isNotEmpty()) {
                listViewKeys?.adapter = AttesterKeyAdapter(this, responses)
              } else {
                UIUtil.exchangeViewVisibility(true, emptyView, layoutContent)
              }
            }
          }

          Result.Status.ERROR -> {
            //toast(it.apiErrors?.firstError ?: getString(R.string.unknown_error))

            UIUtil.exchangeViewVisibility(false, progressBar, layoutContent)
            //showInfoSnackbar(rootView, it.apiError?.message ?: getString(R.string.unknown_error))
          }

          Result.Status.EXCEPTION -> {
            UIUtil.exchangeViewVisibility(false, progressBar, layoutContent)
            showInfoSnackbar(rootView, it.exception?.message ?: getString(R.string.unknown_error))
          }
        }
      }
    })
  }
}
