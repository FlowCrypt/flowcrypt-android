/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentAttesterSettingsBinding
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.jetpack.viewmodel.AccountPublicKeyServersViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ListProgressBehaviour
import com.flowcrypt.email.ui.adapter.AttesterKeyAdapter
import com.google.android.material.snackbar.Snackbar

/**
 * Basically, this Fragment gets all known addresses of the user, and then submits one call with all addresses to
 * /lookup/email/ Attester API, then compares the results.
 *
 * @author Denis Bondarenko
 *         Date: 2/18/20
 *         Time: 9:46 AM
 *         E-mail: DenBond7@gmail.com
 */
class AttesterSettingsFragment : BaseFragment<FragmentAttesterSettingsBinding>(),
  ListProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentAttesterSettingsBinding.inflate(inflater, container, false)

  override val emptyView: View?
    get() = binding?.empty?.root
  override val progressView: View?
    get() = binding?.progress?.root
  override val contentView: View?
    get() = binding?.rVAttester
  override val statusView: View?
    get() = binding?.status?.root

  private val accountPublicKeyServersViewModel: AccountPublicKeyServersViewModel by viewModels()
  private val attesterKeyAdapter: AttesterKeyAdapter = AttesterKeyAdapter()

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)
    setupAccountKeysInfoViewModel()
  }

  private fun initViews(view: View) {
    binding?.sRL?.setColorSchemeResources(
      R.color.colorPrimary,
      R.color.colorPrimary,
      R.color.colorPrimary
    )
    binding?.sRL?.setOnRefreshListener {
      dismissCurrentSnackBar()
      accountPublicKeyServersViewModel.refreshData()
    }

    val rVAttester: RecyclerView? = view.findViewById(R.id.rVAttester)
    context?.let {
      val manager = LinearLayoutManager(it)
      val decoration = DividerItemDecoration(it, manager.orientation)
      val drawable = ResourcesCompat.getDrawable(resources, R.drawable.divider_1dp_grey, it.theme)
      drawable?.let { decoration.setDrawable(drawable) }
      rVAttester?.addItemDecoration(decoration)
      rVAttester?.layoutManager = manager
      rVAttester?.adapter = attesterKeyAdapter
    }
  }

  private fun setupAccountKeysInfoViewModel() {
    accountPublicKeyServersViewModel.accountKeysInfoLiveData.observe(viewLifecycleOwner) {
      it?.let {
        when (it.status) {
          Result.Status.LOADING -> {
            countingIdlingResource?.incrementSafely()
            if (binding?.sRL?.isRefreshing != true || attesterKeyAdapter.itemCount == 0) {
              binding?.sRL?.isRefreshing = false
              showProgress()
            } else return@let
          }

          Result.Status.SUCCESS -> {
            binding?.sRL?.isRefreshing = false
            it.data?.let { responses ->
              if (responses.isNotEmpty()) {
                attesterKeyAdapter.setData(responses)
                showContent()
              } else {
                showEmptyView()
              }
            }
            countingIdlingResource?.decrementSafely()
          }

          Result.Status.ERROR -> {
            binding?.sRL?.isRefreshing = false
            countingIdlingResource?.decrementSafely()
          }

          Result.Status.EXCEPTION -> {
            binding?.sRL?.isRefreshing = false
            showStatus(
              it.exception?.message
                ?: it.exception?.javaClass?.simpleName
                ?: getString(R.string.unknown_error)
            )
            showSnackbar(
              view = contentView,
              msgText = getString(R.string.an_error_has_occurred),
              btnName = getString(R.string.retry),
              duration = Snackbar.LENGTH_LONG
            ) {
              accountPublicKeyServersViewModel.refreshData()
            }

            countingIdlingResource?.decrementSafely()
          }
          else -> {}
        }
      }
    }
  }
}
