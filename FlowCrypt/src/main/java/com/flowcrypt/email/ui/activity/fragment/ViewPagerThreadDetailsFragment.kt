/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentViewPagerThreadDetailsBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.supportActionBar
import com.flowcrypt.email.extensions.androidx.fragment.app.toast
import com.flowcrypt.email.extensions.observeOnce
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.ThreadsViewPagerViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.adapter.ThreadDetailsFragmentsAdapter

/**
 * @author Denys Bondarenko
 */
class ViewPagerThreadDetailsFragment : BaseFragment<FragmentViewPagerThreadDetailsBinding>() {
  private val args by navArgs<ViewPagerThreadDetailsFragmentArgs>()

  private val threadsViewPagerViewModel: ThreadsViewPagerViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ThreadsViewPagerViewModel(
          initialMessageEntityId = args.messageEntityId,
          localFolder = args.localFolder,
          application = requireActivity().application
        ) as T
      }
    }
  }

  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentViewPagerThreadDetailsBinding.inflate(inflater, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    //need to clear action bar titles
    supportActionBar?.title = null
    supportActionBar?.subtitle = null

    binding?.viewPager2?.apply {
      adapter = ThreadDetailsFragmentsAdapter(
        localFolder = args.localFolder,
        initialList = threadsViewPagerViewModel.messageEntitiesLiveData.value?.data ?: emptyList(),
        fragment = this@ViewPagerThreadDetailsFragment
      ) { _, _ -> }

      addItemDecoration(DividerItemDecoration(view.context, ORIENTATION_HORIZONTAL))

      setOffscreenPageLimit(1)
      registerOnPageChangeCallback(object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
          super.onPageSelected(position)
          (adapter as ThreadDetailsFragmentsAdapter).getItem(position)?.let { messageEntity ->
            threadsViewPagerViewModel.onItemSelected(messageEntity)
            val activeFragment = childFragmentManager.fragments.firstOrNull {
              messageEntity.id == it.navArgs<ThreadDetailsFragmentArgs>().value.messageEntityId
            }?.apply {
              (this as? ThreadDetailsFragment)?.changeActiveState(true)
            }
            val childFragmentsExceptSelected = childFragmentManager.fragments - activeFragment
            childFragmentsExceptSelected.forEach {
              (it as? ThreadDetailsFragment)?.changeActiveState(false)
            }
          }
        }
      })
    }

    setupThreadsViewPagerViewModel()
  }

  override fun onDestroyView() {
    binding?.viewPager2?.adapter = null
    super.onDestroyView()
  }

  /*
   * we have to define this code to manage action bar items
   * for [ThreadDetailsFragment] automatically
   */
  override fun onSetupActionBarMenu(menuHost: MenuHost) {
    super.onSetupActionBarMenu(menuHost)
    menuHost.addMenuProvider(object : MenuProvider {
      override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.fragment_thread_details, menu)
      }

      override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
    }, viewLifecycleOwner, Lifecycle.State.RESUMED)
  }

  private fun setupThreadsViewPagerViewModel() {
    threadsViewPagerViewModel.initialLiveData.observeOnce(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.SUCCESS -> {
          (binding?.viewPager2?.adapter as? ThreadDetailsFragmentsAdapter)?.submit(
            it.data ?: emptyList()
          )
        }

        else -> {
          toast(R.string.message_not_found)
          navController?.navigateUp()
        }
      }
    }

    threadsViewPagerViewModel.messageEntitiesLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.SUCCESS -> {
          (binding?.viewPager2?.adapter as? ThreadDetailsFragmentsAdapter)?.submit(
            it.data ?: emptyList()
          )
        }

        else -> {}
      }
    }
  }
}