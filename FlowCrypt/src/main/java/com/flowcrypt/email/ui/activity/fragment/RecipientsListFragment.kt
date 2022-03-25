/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.RecipientEntity
import com.flowcrypt.email.databinding.FragmentRecipientsListBinding
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.jetpack.viewmodel.RecipientsViewModel
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ListProgressBehaviour
import com.flowcrypt.email.ui.adapter.RecipientsRecyclerViewAdapter

/**
 * This fragment shows a list of contacts which have a public key.
 *
 * @author Denis Bondarenko
 *         Date: 9/20/19
 *         Time: 6:11 PM
 *         E-mail: DenBond7@gmail.com
 */
class RecipientsListFragment : BaseFragment<FragmentRecipientsListBinding>(),
  ListProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentRecipientsListBinding.inflate(inflater, container, false)

  private val recipientsRecyclerViewAdapter: RecipientsRecyclerViewAdapter =
    RecipientsRecyclerViewAdapter(
      true,
      object : RecipientsRecyclerViewAdapter.OnRecipientActionsListener {
        override fun onDeleteRecipient(recipientEntity: RecipientEntity) {
          recipientsViewModel.deleteRecipient(recipientEntity)
          Toast.makeText(
            context, getString(R.string.the_contact_was_deleted, recipientEntity.email),
            Toast.LENGTH_SHORT
          ).show()
        }

        override fun onRecipientClick(recipientEntity: RecipientEntity) {
          navController?.navigate(
            RecipientsListFragmentDirections
              .actionRecipientsListFragmentToRecipientDetailsFragment(recipientEntity)
          )
        }
      })
  private val recipientsViewModel: RecipientsViewModel by viewModels()

  override val emptyView: View?
    get() = binding?.emptyView
  override val progressView: View?
    get() = binding?.pB
  override val contentView: View?
    get() = binding?.rVRecipients
  override val statusView: View?
    get() = null

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
  ): View? {
    binding = FragmentRecipientsListBinding.inflate(inflater, container, false)
    return binding?.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    setupRecipientsViewModel()
  }

  private fun initViews() {
    val manager = LinearLayoutManager(context)
    val decoration = DividerItemDecoration(context, manager.orientation)
    val drawable =
      ResourcesCompat.getDrawable(resources, R.drawable.divider_1dp_grey, requireContext().theme)
    drawable?.let { decoration.setDrawable(drawable) }
    binding?.rVRecipients?.addItemDecoration(decoration)
    binding?.rVRecipients?.layoutManager = manager
    binding?.rVRecipients?.adapter = recipientsRecyclerViewAdapter

    binding?.fABtImportPublicKey?.setOnClickListener {
      navController?.navigate(
        RecipientsListFragmentDirections
          .actionRecipientsListFragmentToImportRecipientsFromSourceFragment()
      )
    }
  }

  private fun setupRecipientsViewModel() {
    lifecycleScope.launchWhenStarted {
      recipientsViewModel.recipientsWithPgpFlow.collect {
        if (it.isNullOrEmpty()) {
          showEmptyView()
        } else {
          recipientsRecyclerViewAdapter.submitList(it)
          showContent()
        }
      }
    }
  }
}
