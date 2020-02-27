/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.flowcrypt.email.R
import com.flowcrypt.email.api.retrofit.LoadingState
import com.flowcrypt.email.api.retrofit.Status
import com.flowcrypt.email.api.retrofit.node.NodeRepository
import com.flowcrypt.email.api.retrofit.response.model.node.NodeKeyDetails
import com.flowcrypt.email.api.retrofit.response.node.NodeResponseWrapper
import com.flowcrypt.email.api.retrofit.response.node.ParseKeysResult
import com.flowcrypt.email.database.dao.source.AccountDaoSource
import com.flowcrypt.email.jetpack.viewmodel.PrivateKeysViewModel
import com.flowcrypt.email.ui.activity.ImportPrivateKeyActivity
import com.flowcrypt.email.ui.activity.fragment.base.BaseFragment
import com.flowcrypt.email.util.UIUtil
import com.google.android.gms.common.util.CollectionUtils
import java.util.*

/**
 * This [Fragment] shows information about available private keys in the database.
 *
 * @author DenBond7
 * Date: 20.11.2018
 * Time: 10:30
 * E-mail: DenBond7@gmail.com
 */
class PrivateKeysListFragment : BaseFragment(), View.OnClickListener, PrivateKeysRecyclerViewAdapter.OnKeySelectedListener,
    Observer<NodeResponseWrapper<*>> {

  private var progressBar: View? = null
  private var emptyView: View? = null
  private var content: View? = null

  private var recyclerViewAdapter: PrivateKeysRecyclerViewAdapter? = null

  override val contentResourceId: Int = R.layout.fragment_private_keys

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    recyclerViewAdapter = PrivateKeysRecyclerViewAdapter(requireContext(), ArrayList(), this)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initViews(view)

    if (recyclerViewAdapter!!.itemCount == 0 && baseActivity.isNodeReady) {
      fetchKeys()
    }
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)

    if (supportActionBar != null) {
      supportActionBar!!.setTitle(R.string.keys)
    }
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

  override fun onChanged(nodeResponseWrapper: NodeResponseWrapper<*>) {
    when (nodeResponseWrapper.requestCode) {
      R.id.live_data_id_fetch_keys -> when (nodeResponseWrapper.status) {
        Status.LOADING -> {
          nodeResponseWrapper.loadingState?.let {
            if (LoadingState.PREPARE_REQUEST == it) {
              emptyView?.visibility = View.GONE
              UIUtil.exchangeViewVisibility(true, progressBar, content)
            }
          }
        }

        Status.SUCCESS -> {
          val parseKeysResult = nodeResponseWrapper.result as ParseKeysResult?
          val nodeKeyDetailsList = parseKeysResult!!.nodeKeyDetails
          if (CollectionUtils.isEmpty(nodeKeyDetailsList)) {
            recyclerViewAdapter!!.swap(emptyList())
            UIUtil.exchangeViewVisibility(true, emptyView!!, content!!)
          } else {
            recyclerViewAdapter!!.swap(nodeKeyDetailsList)
            UIUtil.exchangeViewVisibility(false, progressBar!!, content!!)
          }
        }

        Status.ERROR -> Toast.makeText(context, nodeResponseWrapper.result?.apiError?.toString(),
            Toast.LENGTH_SHORT).show()

        Status.EXCEPTION -> Toast.makeText(context, nodeResponseWrapper.exception!!.message, Toast.LENGTH_SHORT).show()
      }
    }
  }

  fun fetchKeys() {
    val viewModel = ViewModelProvider(this).get(PrivateKeysViewModel::class.java)
    viewModel.init(NodeRepository())
    viewModel.responsesLiveData.observe(viewLifecycleOwner, this)
  }

  private fun runCreateOrImportKeyActivity() {
    val account = AccountDaoSource().getActiveAccountInformation(context) ?: return
    startActivityForResult(ImportPrivateKeyActivity.getIntent(context = requireContext(), accountDao = account,
        title = getString(R.string.import_private_key),
        throwErrorIfDuplicateFoundEnabled = true,
        cls = ImportPrivateKeyActivity::class.java,
        isSubmittingPubKeysEnabled = false),
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

    if (recyclerViewAdapter!!.itemCount > 0) {
      progressBar!!.visibility = View.GONE
    }

    if (root.findViewById<View>(R.id.floatActionButtonAddKey) != null) {
      root.findViewById<View>(R.id.floatActionButtonAddKey).setOnClickListener(this)
    }
  }

  companion object {

    private const val REQUEST_CODE_START_IMPORT_KEY_ACTIVITY = 0

    @JvmStatic
    fun newInstance(): PrivateKeysListFragment {
      return PrivateKeysListFragment()
    }
  }
}
