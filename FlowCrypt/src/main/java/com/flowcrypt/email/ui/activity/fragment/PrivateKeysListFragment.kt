/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.databinding.FragmentPrivateKeysBinding
import com.flowcrypt.email.extensions.android.os.getParcelableArrayListViaExt
import com.flowcrypt.email.extensions.countingIdlingResource
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.launchAndRepeatWithViewLifecycle
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.setFragmentResultListenerForTwoWayDialog
import com.flowcrypt.email.extensions.showTwoWayDialog
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.security.model.PgpKeyRingDetails
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.base.ListProgressBehaviour
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.ui.adapter.PrivateKeysListAdapter
import com.flowcrypt.email.util.GeneralUtil
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.pgpainless.key.info.KeyRingInfo

/**
 * This [Fragment] shows information about available private keys in the database.
 *
 * @author Denys Bondarenko
 */
class PrivateKeysListFragment : BaseFragment<FragmentPrivateKeysBinding>(), ListProgressBehaviour {
  override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
    FragmentPrivateKeysBinding.inflate(inflater, container, false)

  private val privateKeysAdapter = PrivateKeysListAdapter(
    onKeySelectedListener = object : PrivateKeysListAdapter.OnKeySelectedListener {
      override fun onKeySelected(position: Int, pgpKeyRingDetails: KeyRingInfo?) {
        if (tracker?.hasSelection() == true) return

        pgpKeyRingDetails?.let {
          navController?.navigate(
            PrivateKeysListFragmentDirections
              .actionPrivateKeysListFragmentToPrivateKeyDetailsFragment(it.fingerprint.toString())
          )
        }
      }
    },
    checkSelection = {
      tracker?.isSelected(it) ?: false
    })
  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()
  private var tracker: SelectionTracker<KeyRingInfo>? = null
  private var actionMode: ActionMode? = null
  private val selectionObserver = object : SelectionTracker.SelectionObserver<KeyRingInfo>() {
    override fun onSelectionChanged() {
      super.onSelectionChanged()
      when {
        tracker?.hasSelection() == true -> {
          if (actionMode == null) {
            actionMode =
              (this@PrivateKeysListFragment.activity as AppCompatActivity).startSupportActionMode(
                genActionModeForKeys()
              )
          }
          actionMode?.title = getString(R.string.selection_text, tracker?.selection?.size() ?: 0)
        }

        tracker?.hasSelection() == false -> {
          actionMode?.finish()
          actionMode = null
        }
      }
    }
  }

  override val emptyView: View?
    get() = binding?.emptyView
  override val progressView: View?
    get() = binding?.progressBar
  override val contentView: View?
    get() = binding?.content
  override val statusView: View? = null

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews()
    setupPrivateKeysViewModel()
    subscribeToImportingAdditionalPrivateKeys()
    subscribeToTwoWayDialog()
  }

  override fun onViewStateRestored(savedInstanceState: Bundle?) {
    super.onViewStateRestored(savedInstanceState)
    tracker?.onRestoreInstanceState(savedInstanceState)

    if (tracker?.hasSelection() == true) {
      selectionObserver.onSelectionChanged()
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    tracker?.onSaveInstanceState(outState)
  }

  override fun onAccountInfoRefreshed(accountEntity: AccountEntity?) {
    super.onAccountInfoRefreshed(accountEntity)
    if (accountEntity?.clientConfiguration?.usesKeyManager() == true) {
      binding?.floatActionButtonAddKey?.gone()
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun setupPrivateKeysViewModel() {
    launchAndRepeatWithViewLifecycle {
      privateKeysViewModel.secretKeyRingsInfoStateFlow.collect {
        when (it.status) {
          Result.Status.LOADING -> {
            showProgress()
            countingIdlingResource?.incrementSafely(this@PrivateKeysListFragment)
          }

          Result.Status.SUCCESS -> {
            val detailsList = it.data ?: emptyList()
            privateKeysAdapter.submitList(detailsList)
            if (detailsList.isEmpty()) {
              showEmptyView()
            } else {
              showContent()
            }
            countingIdlingResource?.decrementSafely(this@PrivateKeysListFragment)
          }

          Result.Status.EXCEPTION -> {
            showContent()
            toast(it.exception?.message, Toast.LENGTH_SHORT)
            countingIdlingResource?.decrementSafely(this@PrivateKeysListFragment)
          }

          else -> {}
        }
      }
    }
  }

  private fun initViews() {
    binding?.recyclerViewKeys?.apply {
      setHasFixedSize(true)
      val manager = LinearLayoutManager(context)
      val decoration = DividerItemDecoration(context, manager.orientation)
      addItemDecoration(decoration)
      layoutManager = manager
      adapter = privateKeysAdapter
      setupSelectionTracker(this)
    }

    if (privateKeysAdapter.itemCount > 0) {
      showContent()
    }

    binding?.floatActionButtonAddKey?.setOnClickListener {
      account?.let { accountEntity ->
        navController?.navigate(
          PrivateKeysListFragmentDirections
            .actionPrivateKeysListFragmentToImportAdditionalPrivateKeysFragment(
              requestKey = REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS,
              accountEntity = accountEntity
            )
        )
      }
    }
  }

  private fun setupSelectionTracker(recyclerView: RecyclerView) {
    /*tracker = SelectionTracker.Builder(
      javaClass.simpleName,
      recyclerView,
      KeyRingInfoItemKeyProvider(privateKeysAdapter.currentList),
      KeyRingInfoItemDetailsLookup(recyclerView),
      StorageStrategy.createStringStorage()
    ).build()*/

    tracker?.addObserver(selectionObserver)
  }

  private fun genActionModeForKeys(): ActionMode.Callback {
    return object : ActionMode.Callback {
      override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        val count = tracker?.selection?.size() ?: 0
        return when (item?.itemId) {
          R.id.menuActionDeleteKey -> {
            showTwoWayDialog(
              requestCode = REQUEST_CODE_DELETE_KEYS_DIALOG,
              dialogTitle = "",
              dialogMsg = requireContext().resources.getQuantityString(
                R.plurals.delete_key_question,
                count,
                count
              ),
              positiveButtonTitle = getString(android.R.string.ok),
              negativeButtonTitle = getString(android.R.string.cancel),
              isCancelable = false
            )
            true
          }

          else -> false
        }
      }

      override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        mode?.menuInflater?.inflate(R.menu.private_keys_list_context_menu, menu)
        return true
      }

      override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return true
      }

      override fun onDestroyActionMode(mode: ActionMode?) {
        tracker?.clearSelection()
      }
    }
  }

  private fun subscribeToImportingAdditionalPrivateKeys() {
    setFragmentResultListener(REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS) { _, bundle ->
      val keys = bundle.getParcelableArrayListViaExt<PgpKeyRingDetails>(
        ImportAdditionalPrivateKeysFragment.KEY_IMPORTED_PRIVATE_KEYS
      )
      if (keys?.isNotEmpty() == true) {
        toast(R.string.key_successfully_imported)
      }
    }
  }

  private fun subscribeToTwoWayDialog() {
    setFragmentResultListenerForTwoWayDialog { _, bundle ->
      val requestCode = bundle.getInt(TwoWayDialogFragment.KEY_REQUEST_CODE)
      val result = bundle.getInt(TwoWayDialogFragment.KEY_RESULT)

      when (requestCode) {
        REQUEST_CODE_DELETE_KEYS_DIALOG -> if (result == TwoWayDialogFragment.RESULT_OK) {
          /*account?.let { accountEntity ->
            tracker?.selection?.map { it }
              ?.let { privateKeysViewModel.deleteKeys(accountEntity, it) }
          }*/

          actionMode?.finish()
        }
      }
    }
  }

  companion object {
    private const val REQUEST_CODE_DELETE_KEYS_DIALOG = 100
    private val REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_IMPORT_ADDITIONAL_PRIVATE_KEYS",
      PrivateKeysListFragment::class.java
    )
  }
}
