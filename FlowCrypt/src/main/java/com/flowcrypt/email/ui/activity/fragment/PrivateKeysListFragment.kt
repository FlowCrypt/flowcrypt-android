/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.response.base.Result
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.extensions.decrementSafely
import com.flowcrypt.email.extensions.incrementSafely
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.ui.activity.ImportPrivateKeyActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
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

  override fun onAttach(context: Context) {
    super.onAttach(context)
    recyclerViewAdapter = PrivateKeysRecyclerViewAdapter(context, this)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)
    setupPrivateKeysViewModel()
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

      else -> super.onActivityResult(requestCode, resultCode, data)
    }
  }

  override fun onClick(v: View) {
    when (v.id) {
      R.id.floatActionButtonAddKey -> runCreateOrImportKeyActivity()
    }
  }

  override fun onKeySelected(position: Int, nodeKeyDetails: NodeKeyDetails?) {
    nodeKeyDetails?.let {
      parentFragmentManager
          .beginTransaction()
          .replace(R.id.layoutContent, PrivateKeyDetailsFragment.newInstance(it))
          .addToBackStack(null)
          .commit()
    }
  }

  private fun setupPrivateKeysViewModel() {
    privateKeysViewModel.privateKeyDetailsLiveData.observe(viewLifecycleOwner, Observer {
      when (it.status) {
        Result.Status.LOADING -> {
          baseActivity.countingIdlingResource.incrementSafely()
          emptyView?.visibility = View.GONE
          UIUtil.exchangeViewVisibility(true, progressBar, content)
        }

        Result.Status.SUCCESS -> {
          val parseKeysResult = it.data
          val detailsList: List<NodeKeyDetails> = parseKeysResult?.nodeKeyDetails ?: emptyList()
          if (detailsList.isEmpty()) {
            recyclerViewAdapter.swap(emptyList())
            UIUtil.exchangeViewVisibility(true, emptyView, content)
          } else {
            recyclerViewAdapter.swap(detailsList)
            UIUtil.exchangeViewVisibility(false, progressBar, content)
          }
          baseActivity.countingIdlingResource.decrementSafely()
        }

        Result.Status.ERROR -> {
          Toast.makeText(context, it.data?.apiError?.toString(), Toast.LENGTH_SHORT).show()
          baseActivity.countingIdlingResource.decrementSafely()
        }

        Result.Status.EXCEPTION -> {
          Toast.makeText(context, it.exception?.message, Toast.LENGTH_SHORT).show()
          baseActivity.countingIdlingResource.decrementSafely()
        }
      }
    })
  }

  private fun runCreateOrImportKeyActivity() {
    startActivityForResult(ImportPrivateKeyActivity.getIntent(
        context = requireContext(),
        title = getString(R.string.import_private_key),
        throwErrorIfDuplicateFoundEnabled = true,
        cls = ImportPrivateKeyActivity::class.java,
        isSubmittingPubKeysEnabled = false,
        accountEntity = account),
        REQUEST_CODE_START_IMPORT_KEY_ACTIVITY)
  }

  private fun initViews(root: View) {
    this.progressBar = root.findViewById(R.id.progressBar)
    this.content = root.findViewById(R.id.groupContent)
    this.emptyView = root.findViewById(R.id.emptyView)

    val recyclerView = root.findViewById<RecyclerView>(R.id.recyclerViewKeys)
    recyclerView.setHasFixedSize(true)
    val manager = LinearLayoutManager(context)
    val decoration = DividerItemDecoration(recyclerView.context, manager.orientation)
    decoration.setDrawable(resources.getDrawable(R.drawable.divider_1dp_grey, requireContext().theme))
    recyclerView.addItemDecoration(decoration)
    recyclerView.layoutManager = manager
    recyclerView.adapter = recyclerViewAdapter

    if (recyclerViewAdapter.itemCount > 0) {
      progressBar?.visibility = View.GONE
    }

    if (root.findViewById<View>(R.id.floatActionButtonAddKey) != null) {
      root.findViewById<View>(R.id.floatActionButtonAddKey).setOnClickListener(this)
    }
  }

  companion object {

    private const val REQUEST_CODE_START_IMPORT_KEY_ACTIVITY = 0

    fun newInstance(): PrivateKeysListFragment {
      return PrivateKeysListFragment()
    }
  }
}
