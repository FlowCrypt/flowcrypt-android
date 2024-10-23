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
import com.flowcrypt.email.databinding.FragmentViewPagerMessageDetailsBinding
import com.flowcrypt.email.extensions.androidx.fragment.app.navController
import com.flowcrypt.email.extensions.androidx.fragment.app.supportActionBar
import com.flowcrypt.email.extensions.androidx.fragment.app.toast
import com.flowcrypt.email.extensions.androidx.viewpager2.widget.reduceDragSensitivity
import com.flowcrypt.email.extensions.observeOnce
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.MessagesViewPagerViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.adapter.MessageDetailsFragmentsAdapter

/**
 * @author Denys Bondarenko
 */
class ViewPagerMessageDetailsFragment : BaseFragment<FragmentViewPagerMessageDetailsBinding>() {
  private val args by navArgs<ViewPagerMessageDetailsFragmentArgs>()

  private val messagesViewPagerViewModel: MessagesViewPagerViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MessagesViewPagerViewModel(
          initialMessageEntityId = args.messageEntityId,
          localFolder = args.localFolder,
          sortedEntityIdListForThread = args.sortedEntityIdListForThread?.toList(),
          application = requireActivity().application
        ) as T
      }
    }
  }

  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentViewPagerMessageDetailsBinding.inflate(inflater, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    //need to clear action bar titles
    supportActionBar?.title = null
    supportActionBar?.subtitle = null

    binding?.viewPager2?.apply {
      reduceDragSensitivity()
      adapter = MessageDetailsFragmentsAdapter(
        localFolder = args.localFolder,
        initialList = messagesViewPagerViewModel.messageEntitiesLiveData.value?.data ?: emptyList(),
        fragment = this@ViewPagerMessageDetailsFragment,
        isThreadMode = args.sortedEntityIdListForThread?.isNotEmpty() == true
      ) { _, _ -> }

      addItemDecoration(DividerItemDecoration(view.context, ORIENTATION_HORIZONTAL))

      setOffscreenPageLimit(1)
      registerOnPageChangeCallback(object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
          super.onPageSelected(position)
          (adapter as MessageDetailsFragmentsAdapter).getItem(position)?.let { messageEntity ->
            messagesViewPagerViewModel.onItemSelected(messageEntity)
          }
        }
      })
    }

    setupMessagesViewPagerViewModel()
  }

  override fun onDestroyView() {
    binding?.viewPager2?.adapter = null
    super.onDestroyView()
  }

  /*
   * we have to define this code to manage action bar items
   * for [MessageDetailsFragment] automatically
   */
  override fun onSetupActionBarMenu(menuHost: MenuHost) {
    super.onSetupActionBarMenu(menuHost)
    menuHost.addMenuProvider(object : MenuProvider {
      override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.fragment_message_details, menu)
      }

      override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false
    }, viewLifecycleOwner, Lifecycle.State.RESUMED)
  }

  private fun setupMessagesViewPagerViewModel() {
    messagesViewPagerViewModel.initialLiveData.observeOnce(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.SUCCESS -> {
          (binding?.viewPager2?.adapter as? MessageDetailsFragmentsAdapter)?.submit(
            it.data ?: emptyList()
          )
        }

        else -> {
          toast(R.string.message_not_found)
          navController?.navigateUp()
        }
      }
    }

    messagesViewPagerViewModel.messageEntitiesLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.SUCCESS -> {
          (binding?.viewPager2?.adapter as? MessageDetailsFragmentsAdapter)?.submit(
            it.data ?: emptyList()
          )
        }

        else -> {}
      }
    }
  }
}