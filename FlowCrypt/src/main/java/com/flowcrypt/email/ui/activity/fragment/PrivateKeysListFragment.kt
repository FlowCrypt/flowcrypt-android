/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.extensions.showTwoWayDialog
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.security.model.NodeKeyDetails
import com.flowcrypt.email.ui.activity.ImportPrivateKeyActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.ui.activity.fragment.dialog.TwoWayDialogFragment
import com.flowcrypt.email.ui.adapter.PrivateKeysRecyclerViewAdapter
import com.flowcrypt.email.ui.adapter.selection.NodeKeyDetailsKeyProvider
import com.flowcrypt.email.ui.adapter.selection.PrivateKeyItemDetailsLookup
import com.flowcrypt.email.util.UIUtil

/**
 * This [Fragment] shows information about available private keys in the database.
 *
 * @author DenBond7
 * Date: 20.11.2018
 * Time: 10:30
 * E-mail: DenBond7@gmail.com
 */
class PrivateKeysListFragment : BaseFragment(), View.OnClickListener, PrivateKeysRecyclerViewAdapter.OnKeySelectedListener {

  private var progressBar: View? = null
  private var emptyView: View? = null
  private var content: View? = null

  private val privateKeysViewModel: PrivateKeysViewModel by viewModels()
  private lateinit var recyclerViewAdapter: PrivateKeysRecyclerViewAdapter

  override val contentResourceId: Int = R.layout.fragment_private_keys

  private var tracker: SelectionTracker<NodeKeyDetails>? = null
  private var actionMode: ActionMode? = null
  private val selectionObserver = object : SelectionTracker.SelectionObserver<NodeKeyDetails>() {
    override fun onSelectionChanged() {
      super.onSelectionChanged()
      when {
        tracker?.hasSelection() == true -> {
          if (actionMode == null) {
            actionMode = (this@PrivateKeysListFragment.activity as AppCompatActivity).startSupportActionMode(genActionModeForKeys())
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

  override fun onAttach(context: Context) {
    super.onAttach(context)
    recyclerViewAdapter = PrivateKeysRecyclerViewAdapter(context, this)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)
    setupPrivateKeysViewModel()
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

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    supportActionBar?.setTitle(R.string.keys)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    when (requestCode) {
      REQUEST_CODE_START_IMPORT_KEY_ACTIVITY -> when (resultCode) {
        Activity.RESULT_OK -> Toast.makeText(context, R.string.key_successfully_imported, Toast.LENGTH_SHORT).show()
      }

      REQUEST_CODE_DELETE_KEYS_DIALOG -> {
        when (resultCode) {
          TwoWayDialogFragment.RESULT_OK -> {
            account?.let { accountEntity ->
              tracker?.selection?.map { it }?.let { privateKeysViewModel.deleteKeys(accountEntity, it) }
            }

            actionMode?.finish()
          }
        }
      }

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.floatActionButtonAddKey -> startActivityForResult(ImportPrivateKeyActivity.getIntent(
          context = requireContext(),
          title = getString(R.string.import_private_key),
          throwErrorIfDuplicateFoundEnabled = true,
          cls = ImportPrivateKeyActivity::class.java,
          isSubmittingPubKeysEnabled = false,
          accountEntity = account,
          isSyncEnabled = true,
          skipImportedKeys = true),
          REQUEST_CODE_START_IMPORT_KEY_ACTIVITY)
    }
  }

  override fun onKeySelected(position: Int, nodeKeyDetails: NodeKeyDetails?) {
    if (tracker?.hasSelection() == true) {
      return
    }

    nodeKeyDetails?.let {
      parentFragmentManager
          .beginTransaction()
          .replace(R.id.layoutContent, PrivateKeyDetailsFragment.newInstance(it))
          .addToBackStack(null)
          .commit()
    }
  }

  private fun setupPrivateKeysViewModel() {
    privateKeysViewModel.parseKeysResultLiveData.observe(viewLifecycleOwner, {
      when (it.status) {
        Result.Status.LOADING -> {
          baseActivity.countingIdlingResource.incrementSafely()
          emptyView?.visibility = View.GONE
          UIUtil.exchangeViewVisibility(true, progressBar, content)
        }

        Result.Status.SUCCESS -> {
          val detailsList = it.data ?: emptyList()
          if (detailsList.isEmpty()) {
            recyclerViewAdapter.swap(emptyList())
            UIUtil.exchangeViewVisibility(true, emptyView, content)
          } else {
            recyclerViewAdapter.swap(detailsList)
            UIUtil.exchangeViewVisibility(false, progressBar, content)
          }
          baseActivity.countingIdlingResource.decrementSafely()
        }

        Result.Status.EXCEPTION -> {
          Toast.makeText(context, it.exception?.message, Toast.LENGTH_SHORT).show()
          baseActivity.countingIdlingResource.decrementSafely()
        }
      }
    })
  }

  private fun initViews(root: View) {
    this.progressBar = root.findViewById(R.id.progressBar)
    this.content = root.findViewById(R.id.groupContent)
    this.emptyView = root.findViewById(R.id.emptyView)

    val recyclerView = root.findViewById<RecyclerView>(R.id.recyclerViewKeys)
    recyclerView.setHasFixedSize(true)
    val manager = LinearLayoutManager(context)
    val decoration = DividerItemDecoration(recyclerView.context, manager.orientation)
    val drawable = ResourcesCompat.getDrawable(resources, R.drawable.divider_1dp_grey, requireContext().theme)
    drawable?.let { decoration.setDrawable(drawable) }
    recyclerView.addItemDecoration(decoration)
    recyclerView.layoutManager = manager
    recyclerView.adapter = recyclerViewAdapter

    //setupSelectionTracker(recyclerView)

    if (recyclerViewAdapter.itemCount > 0) {
      progressBar?.visibility = View.GONE
    }

    if (root.findViewById<View>(R.id.floatActionButtonAddKey) != null) {
      root.findViewById<View>(R.id.floatActionButtonAddKey).setOnClickListener(this)
    }
  }

  private fun setupSelectionTracker(recyclerView: RecyclerView) {
    tracker = SelectionTracker.Builder(
        javaClass.simpleName,
        recyclerView,
        NodeKeyDetailsKeyProvider(recyclerViewAdapter.nodeKeyDetailsList),
        PrivateKeyItemDetailsLookup(recyclerView),
        StorageStrategy.createParcelableStorage(NodeKeyDetails::class.java)
    ).build()

    recyclerViewAdapter.tracker = tracker
    tracker?.addObserver(selectionObserver)
  }

  private fun genActionModeForKeys(): ActionMode.Callback {
    return object : ActionMode.Callback {
      override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        val count = tracker?.selection?.size() ?: 0
        return when (item?.itemId) {
          R.id.menuActionDeleteKey -> {
            showTwoWayDialog(
                dialogTitle = "",
                dialogMsg = requireContext().resources.getQuantityString(R.plurals.delete_key_question, count, count),
                positiveButtonTitle = getString(android.R.string.ok),
                negativeButtonTitle = getString(android.R.string.cancel),
                requestCode = REQUEST_CODE_DELETE_KEYS_DIALOG,
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

  companion object {

    private const val REQUEST_CODE_START_IMPORT_KEY_ACTIVITY = 0
    private const val REQUEST_CODE_DELETE_KEYS_DIALOG = 100

    fun newInstance(): PrivateKeysListFragment {
      return PrivateKeysListFragment()
    }
  }
}
