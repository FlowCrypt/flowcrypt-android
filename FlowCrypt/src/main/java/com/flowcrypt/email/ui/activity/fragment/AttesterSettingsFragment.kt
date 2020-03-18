/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.jetpack.viewmodel.AccountKeysInfoViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ListProgressBehaviour
import com.flowcrypt.email.ui.activity.settings.AttesterSettingsActivity
import com.flowcrypt.email.ui.adapter.AttesterKeyAdapter
import com.flowcrypt.email.util.idling.SingleIdlingResources
import com.google.android.material.snackbar.Snackbar

/**
 * @author Denis Bondarenko
 *         Date: 2/18/20
 *         Time: 9:46 AM
 *         E-mail: DenBond7@gmail.com
 */
class AttesterSettingsFragment : BaseFragment(), ListProgressBehaviour {
  override val emptyView: View?
    get() = view?.findViewById(R.id.empty)
  override val progressView: View?
    get() = view?.findViewById(R.id.progress)
  override val contentView: View?
    get() = view?.findViewById(R.id.rVAttester)
  override val statusView: View?
    get() = view?.findViewById(R.id.status)

  override val contentResourceId: Int = R.layout.fragment_attester_settings

  private var sRL: SwipeRefreshLayout? = null
  private val accountKeysInfoViewModel: AccountKeysInfoViewModel by viewModels()
  private val attesterKeyAdapter: AttesterKeyAdapter = AttesterKeyAdapter()

  private val idlingForAttester: SingleIdlingResources?
    get() {
      return (activity as? AttesterSettingsActivity)?.idlingForAttester
    }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)
    setupAccountKeysInfoViewModel()
  }

  private fun initViews(view: View) {
    sRL = view.findViewById(R.id.sRL)
    sRL?.setColorSchemeResources(R.color.colorPrimary, R.color.colorPrimary, R.color.colorPrimary)
    sRL?.setOnRefreshListener {
      dismissCurrentSnackBar()
      accountKeysInfoViewModel.refreshData()
    }

    val rVAttester: RecyclerView? = view.findViewById(R.id.rVAttester)
    context?.let {
      val manager = LinearLayoutManager(it)
      val decoration = DividerItemDecoration(it, manager.orientation)
      decoration.setDrawable(resources.getDrawable(R.drawable.divider_1dp_grey, it.theme))
      rVAttester?.addItemDecoration(decoration)
      rVAttester?.layoutManager = manager
      rVAttester?.adapter = attesterKeyAdapter
    }
  }

  private fun setupAccountKeysInfoViewModel() {
    accountKeysInfoViewModel.accountKeysInfoLiveData.observe(viewLifecycleOwner, Observer {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            idlingForAttester?.setIdleState(false)
            if (sRL?.isRefreshing != true) {
              showProgress()
            } else return@let
          }

          Result.Status.SUCCESS -> {
            idlingForAttester?.setIdleState(true)
            sRL?.isRefreshing = false
            it.data?.results?.let { responses ->
              if (responses.isNotEmpty()) {
                attesterKeyAdapter.setData(responses)
              } else {
                showEmptyView()
              }
            }
            showContent()
          }

          Result.Status.ERROR -> {
            idlingForAttester?.setIdleState(true)
            sRL?.isRefreshing = false
            showStatus(it.data?.apiError?.msg ?: getString(R.string.unknown_error))
            showSnackbar(contentView, getString(R.string.an_error_has_occurred),
                getString(R.string.retry), Snackbar.LENGTH_LONG, View.OnClickListener {
              accountKeysInfoViewModel.refreshData()
            })
          }

          Result.Status.EXCEPTION -> {
            idlingForAttester?.setIdleState(true)
            sRL?.isRefreshing = false
            showStatus(it.exception?.message ?: getString(R.string.unknown_error))
            showSnackbar(contentView, getString(R.string.an_error_has_occurred),
                getString(R.string.retry), Snackbar.LENGTH_LONG, View.OnClickListener {
              accountKeysInfoViewModel.refreshData()
            })
          }
        }
      }
    })
  }
}