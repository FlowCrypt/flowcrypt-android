/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.navArgs
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.MessageEntity
import com.flowcrypt.email.databinding.FragmentViewPagerMessageDetailsBinding
import com.flowcrypt.email.jetpack.lifecycle.CustomAndroidViewModelFactory
import com.flowcrypt.email.jetpack.viewmodel.MessagesViewPagerViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment

/**
 * @author Denys Bondarenko
 */
class ViewPagerMessageDetailsFragment : BaseFragment<FragmentViewPagerMessageDetailsBinding>() {
  private val args by navArgs<ViewPagerMessageDetailsFragmentArgs>()
  private lateinit var fragmentsAdapter: FragmentsAdapter

  private val messagesViewPagerViewModel: MessagesViewPagerViewModel by viewModels {
    object : CustomAndroidViewModelFactory(requireActivity().application) {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MessagesViewPagerViewModel(
          messageEntityId = args.messageEntityId,
          localFolder = args.localFolder,
          application = requireActivity().application
        ) as T
      }
    }
  }

  private var isInitialPositionApplied = false

  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentViewPagerMessageDetailsBinding.inflate(inflater, container, false)

  override fun onAttach(context: Context) {
    super.onAttach(context)
    fragmentsAdapter = FragmentsAdapter(emptyList(), requireActivity())
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    binding?.viewPager2?.adapter = fragmentsAdapter
    binding?.viewPager2?.setOffscreenPageLimit(1)
    binding?.viewPager2?.registerOnPageChangeCallback(object : OnPageChangeCallback() {
      override fun onPageSelected(position: Int) {
        super.onPageSelected(position)
        fragmentsAdapter.getItem(position)?.let { messageEntity ->
          messagesViewPagerViewModel.onItemSelected(messageEntity)
        }
      }
    })

    setupMessagesViewPagerViewModel()
  }

  private fun setupMessagesViewPagerViewModel() {
    messagesViewPagerViewModel.messageEntitiesLiveData.observe(viewLifecycleOwner) {
      when (it.status) {
        Result.Status.SUCCESS -> {
          val messages = it.data ?: emptyList()
          fragmentsAdapter.submit(messages)
          if (!isInitialPositionApplied) {
            isInitialPositionApplied = true
            binding?.viewPager2?.currentItem =
              messages.indexOfFirst { messageEntity -> messageEntity.id == args.messageEntityId }
          }
        }

        else -> {}
      }
    }
  }

  private inner class FragmentsAdapter(
    initialList: List<MessageEntity>,
    fragmentActivity: FragmentActivity,
  ) : FragmentStateAdapter(fragmentActivity) {
    private val items = mutableListOf<MessageEntity>()

    init {
      items.addAll(initialList)
    }

    override fun getItemCount(): Int = items.size
    override fun createFragment(position: Int): Fragment =
      MessageDetailsFragment().apply {
        arguments = MessageDetailsFragmentArgs(
          messageEntity = items[position],
          localFolder = args.localFolder
        ).toBundle()
      }

    override fun getItemId(position: Int): Long {
      return items[position].id ?: 0
    }

    override fun containsItem(itemId: Long): Boolean {
      return items.any { it.id == itemId }
    }

    fun submit(newList: List<MessageEntity>) {
      items.clear()
      items.addAll(newList)
      notifyDataSetChanged()
    }

    fun getItem(position: Int): MessageEntity? {
      return items.getOrNull(position)
    }
  }
}