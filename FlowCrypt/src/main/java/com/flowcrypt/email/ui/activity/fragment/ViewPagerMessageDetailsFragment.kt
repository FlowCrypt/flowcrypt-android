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
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.databinding.FragmentViewPagerMessageDetailsBinding
import com.flowcrypt.email.extensions.supportActionBar
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.MessagesViewPagerViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ProgressBehaviour
import com.flowcrypt.email.ui.adapter.FragmentsAdapter

/**
 * @author Denys Bondarenko
 */
class ViewPagerMessageDetailsFragment : BaseFragment<FragmentViewPagerMessageDetailsBinding>(),
  ProgressBehaviour {
  private val args by navArgs<ViewPagerMessageDetailsFragmentArgs>()

  private val messagesViewPagerViewModel: MessagesViewPagerViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MessagesViewPagerViewModel(
          initialMessageEntityId = args.messageEntityId,
          localFolder = args.localFolder,
          application = requireActivity().application
        ) as T
      }
    }
  }

  private var isInitialPositionApplied = false

  override val progressView: View?
    get() = binding?.progress?.root
  override val contentView: View?
    get() = binding?.viewPager2
  override val statusView: View?
    get() = binding?.status?.root

  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentViewPagerMessageDetailsBinding.inflate(inflater, container, false)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    isInitialPositionApplied =
      savedInstanceState?.getBoolean(KEY_IS_INITIAL_POSITION_APPLIED) ?: false
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    //need to clear action bar titles
    supportActionBar?.title = null
    supportActionBar?.subtitle = null

    binding?.viewPager2?.apply {
      adapter = FragmentsAdapter(
        localFolder = args.localFolder,
        initialList = emptyList(),
        fragment = this@ViewPagerMessageDetailsFragment
      ) { _, _ ->
        showContent()
        if (!isInitialPositionApplied) {
          isInitialPositionApplied = true
          val id = args.messageEntityId
          val position = (adapter as FragmentsAdapter).getItemPositionById(id)
          binding?.viewPager2?.currentItem = position
        }
      }

      setOffscreenPageLimit(1)
      registerOnPageChangeCallback(object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
          super.onPageSelected(position)
          /*(adapter as FragmentsAdapter).getItem(position)?.let { messageEntity ->
            messagesViewPagerViewMode.onItemSelected(messageEntity)
          }*/
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

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(KEY_IS_INITIAL_POSITION_APPLIED, isInitialPositionApplied)
  }

  private fun setupMessagesViewPagerViewModel() {
    messagesViewPagerViewModel.messageEntitiesLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.SUCCESS -> {
          val messages = it.data ?: emptyList()
          (binding?.viewPager2?.adapter as FragmentsAdapter).submit(messages)
        }

        else -> {}
      }
    }
  }

  companion object {
    private const val KEY_IS_INITIAL_POSITION_APPLIED = "KEY_IS_INITIAL_POSITION_APPLIED"
  }
}